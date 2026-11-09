package com.urlshortener.orchestration.engine;

import com.urlshortener.orchestration.executor.StageExecutor;
import com.urlshortener.orchestration.executor.StageExecutorRegistry;
import com.urlshortener.orchestration.executor.StageOutcome;
import com.urlshortener.orchestration.graph.DependencyGraph;
import com.urlshortener.orchestration.graph.StageDefinition;
import com.urlshortener.orchestration.model.EventType;
import com.urlshortener.orchestration.model.RunStatus;
import com.urlshortener.orchestration.model.StageName;
import com.urlshortener.orchestration.model.StageResult;
import com.urlshortener.orchestration.model.StageStatus;
import com.urlshortener.orchestration.model.WorkflowRun;
import com.urlshortener.orchestration.policy.GuardrailPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import jakarta.annotation.PreDestroy;

/**
 * Stateful workflow engine that drives a {@link WorkflowRun} across the SDLC
 * dependency graph: resolves which stages are ready (dependency-satisfied),
 * runs independent stages in parallel, synchronizes at fan-in points, blocks
 * at human approval gates, applies bounded retries with fallback/rollback,
 * enforces guardrails, and re-plans the graph when upstream output changes.
 *
 * <p>Execution is driven synchronously per call to {@link #advance(WorkflowRun)}:
 * the engine runs as far as it can — executing every stage batch whose
 * dependencies are satisfied — until it either completes the run, fails it,
 * or hits a human approval gate, at which point control returns to the
 * caller. This keeps the "agent executes autonomously between checkpoints"
 * behavior fully deterministic and testable, while still modeling non-linear,
 * stateful, gated execution rather than a simple linear chain.
 */
@Slf4j
@Component
public class OrchestrationEngine {

    private final DependencyGraph graph;
    private final StageExecutorRegistry executors;
    private final GuardrailPolicy guardrailPolicy;
    private final ExecutorService parallelPool = Executors.newFixedThreadPool(4);

    public OrchestrationEngine(DependencyGraph graph, StageExecutorRegistry executors, GuardrailPolicy guardrailPolicy) {
        this.graph = graph;
        this.executors = executors;
        this.guardrailPolicy = guardrailPolicy;
    }

    @PreDestroy
    void shutdown() {
        parallelPool.shutdown();
    }

    /** Initializes every base-graph stage as PENDING and starts execution. */
    public void start(WorkflowRun run) {
        for (StageName stage : baseStages()) {
            run.getStageResults().put(stage, StageResult.builder().stage(stage).status(StageStatus.PENDING).attempts(0).build());
        }
        run.setStatus(RunStatus.RUNNING);
        run.audit(null, EventType.STAGE_STARTED, "Workflow run created for scenario " + run.getScenarioType());
        advance(run);
    }

    /** Grants a pending approval gate and resumes execution. */
    public void approve(WorkflowRun run, StageName stage, String approver) {
        requirePendingApproval(run, stage);
        run.getApprovedStages().add(stage);
        // The gate previously parked this stage in WAITING_APPROVAL so it would
        // not be picked up as "ready"; reset it to PENDING so the next advance()
        // pass schedules it now that the human has signed off.
        run.getStageResults().get(stage).setStatus(StageStatus.PENDING);
        run.setPendingApprovalStage(null);
        run.setStatus(RunStatus.RUNNING);
        run.audit(stage, EventType.APPROVAL_GRANTED, "Approved by " + approver);
        advance(run);
    }

    /** Rejects a pending approval gate; rolls back the gated stage and stops the run. */
    public void reject(WorkflowRun run, StageName stage, String approver, String reason) {
        requirePendingApproval(run, stage);
        StageResult result = run.getStageResults().get(stage);
        result.setStatus(StageStatus.ROLLED_BACK);
        result.setCompletedAt(Instant.now());
        result.setMessage("Rejected by " + approver + ": " + reason);
        run.setPendingApprovalStage(null);
        run.setStatus(RunStatus.ROLLED_BACK);
        run.setRollbackCount(run.getRollbackCount() + 1);
        run.audit(stage, EventType.APPROVAL_REJECTED, "Rejected by " + approver + ": " + reason);
        finalizeLatency(run);
    }

    /** Safe-stop: halts further progress on this run; already-completed stages are preserved. */
    public void stop(WorkflowRun run) {
        if (isTerminal(run.getStatus())) {
            return;
        }
        run.setStatus(RunStatus.STOPPED);
        run.audit(null, EventType.RUN_STOPPED, "Run stopped by operator request");
        finalizeLatency(run);
    }

    private void requirePendingApproval(WorkflowRun run, StageName stage) {
        if (run.getStatus() != RunStatus.WAITING_APPROVAL || stage != run.getPendingApprovalStage()) {
            throw new IllegalStateException("Run " + run.getId() + " has no pending approval for stage " + stage);
        }
    }

    /** Drives execution forward until completion, failure, a gate, or a stop. */
    public void advance(WorkflowRun run) {
        while (true) {
            if (isTerminal(run.getStatus()) || run.getStatus() == RunStatus.WAITING_APPROVAL) {
                return;
            }

            List<StageName> ready = readyStages(run);
            if (ready.isEmpty()) {
                if (allActiveStagesResolved(run)) {
                    completeRun(run);
                }
                return; // no progress possible this round (blocked, or nothing left)
            }

            // Guardrail check gates IMPLEMENTATION before it is allowed to execute.
            if (ready.contains(StageName.IMPLEMENTATION)) {
                Optional<String> violation = guardrailPolicy.evaluate(run.getRequirementText());
                if (violation.isPresent()) {
                    blockOnGuardrailViolation(run, violation.get());
                    return;
                }
            }

            // Any ready stage that needs a not-yet-granted approval blocks the run;
            // stages that don't need approval still execute in the same batch.
            List<StageName> executable = ready.stream()
                .filter(stage -> !needsApproval(run, stage))
                .collect(Collectors.toList());
            Optional<StageName> blockedGate = ready.stream()
                .filter(stage -> needsApproval(run, stage))
                .findFirst();

            if (!executable.isEmpty()) {
                runBatch(run, executable);
                if (run.getStatus() == RunStatus.FAILED || run.getStatus() == RunStatus.ROLLED_BACK) {
                    finalizeLatency(run);
                    return;
                }
            }

            if (blockedGate.isPresent()) {
                StageName gate = blockedGate.get();
                StageResult result = run.getStageResults().get(gate);
                result.setStatus(StageStatus.WAITING_APPROVAL);
                run.setPendingApprovalStage(gate);
                run.setStatus(RunStatus.WAITING_APPROVAL);
                run.audit(gate, EventType.GATE_BLOCKED, "Awaiting human approval before executing " + gate);
                return;
            }
            // Loop again: completing this batch may have unblocked new ready stages.
        }
    }

    private void blockOnGuardrailViolation(WorkflowRun run, String reason) {
        StageResult result = run.getStageResults().get(StageName.IMPLEMENTATION);
        result.setStatus(StageStatus.WAITING_APPROVAL);
        run.setPendingApprovalStage(StageName.IMPLEMENTATION);
        run.setStatus(RunStatus.WAITING_APPROVAL);
        run.audit(StageName.IMPLEMENTATION, EventType.GUARDRAIL_VIOLATION, reason);
        run.audit(StageName.IMPLEMENTATION, EventType.GATE_BLOCKED,
            "Implementation blocked pending human review of guardrail violation");
    }

    private boolean needsApproval(WorkflowRun run, StageName stage) {
        return graph.get(stage).requiresApproval() && !run.getApprovedStages().contains(stage);
    }

    /** Executes a batch of independent, dependency-satisfied stages in parallel. */
    private void runBatch(WorkflowRun run, List<StageName> stages) {
        List<CompletableFuture<Void>> futures = stages.stream()
            .map(stage -> CompletableFuture.runAsync(() -> executeStageWithRetries(run, stage), parallelPool))
            .collect(Collectors.toList());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        boolean anyHardFailure = stages.stream()
            .map(s -> run.getStageResults().get(s).getStatus())
            .anyMatch(status -> status == StageStatus.FAILED || status == StageStatus.ROLLED_BACK);
        if (anyHardFailure && run.getStatus() != RunStatus.ROLLED_BACK) {
            run.setStatus(RunStatus.FAILED);
        }
    }

    /** Runs one stage to completion: attempts up to its bounded retry limit, then fallback or rollback. */
    private void executeStageWithRetries(WorkflowRun run, StageName stage) {
        StageDefinition def = graph.get(stage);
        StageExecutor executor = executors.get(stage);
        StageResult result = run.getStageResults().get(stage);
        result.setStatus(StageStatus.RUNNING);
        if (result.getFirstAttemptAt() == null) {
            result.setFirstAttemptAt(Instant.now());
        }
        run.audit(stage, EventType.STAGE_STARTED, "Executing (max retries=" + def.maxRetries() + ")");

        int attempt = 0;
        StageOutcome outcome = null;
        while (attempt <= def.maxRetries()) {
            attempt++;
            result.setAttempts(attempt);
            outcome = executor.execute(run, attempt);
            if (outcome.isSuccess()) {
                break;
            }
            run.audit(stage, EventType.STAGE_FAILED, "Attempt " + attempt + " failed: " + outcome.getMessage());
            if (attempt <= def.maxRetries()) {
                run.setRetryCount(run.getRetryCount() + 1);
                result.setStatus(StageStatus.RETRYING);
                run.audit(stage, EventType.RETRY_SCHEDULED, "Retrying " + stage + " (attempt " + (attempt + 1) + ")");
            }
        }

        synchronized (run) {
            if (outcome != null && outcome.isSuccess()) {
                result.setStatus(StageStatus.PASSED);
                result.setMessage(outcome.getMessage());
                result.setArtifacts(outcome.getArtifacts());
                result.setCompletedAt(Instant.now());
                run.audit(stage, EventType.STAGE_PASSED, outcome.getMessage());

                if (outcome.isTriggersReplan()) {
                    replan(run, stage);
                }
            } else if (def.hasFallback()) {
                result.setStatus(StageStatus.FALLBACK_APPLIED);
                result.setMessage("Retries exhausted; applied fallback strategy. Last error: "
                    + (outcome != null ? outcome.getMessage() : "unknown"));
                result.setCompletedAt(Instant.now());
                run.audit(stage, EventType.FALLBACK_APPLIED, result.getMessage());
            } else {
                result.setStatus(StageStatus.FAILED);
                result.setMessage("Retries exhausted, no fallback available. Last error: "
                    + (outcome != null ? outcome.getMessage() : "unknown"));
                result.setCompletedAt(Instant.now());
                run.audit(stage, EventType.STAGE_FAILED, result.getMessage());
                rollbackUpstream(run, stage);
            }
        }
    }

    /**
     * When a stage permanently fails (retries exhausted, no fallback), its
     * immediate upstream dependencies that already passed are invalidated
     * and marked for rework: a downstream failure means the "done" verdict on
     * those upstream stages can no longer be trusted as-is. Dependents of the
     * failed stage cannot yet have run (the DAG would not have scheduled
     * them), so there is nothing further downstream to unwind.
     */
    private void rollbackUpstream(WorkflowRun run, StageName failedStage) {
        boolean rolledBackAny = false;
        for (StageName dependency : graph.effectiveDependsOn(failedStage, run.getDynamicallyAddedStages())) {
            StageResult dependencyResult = run.getStageResults().get(dependency);
            if (dependencyResult != null
                && (dependencyResult.getStatus() == StageStatus.PASSED || dependencyResult.getStatus() == StageStatus.FALLBACK_APPLIED)) {
                dependencyResult.setStatus(StageStatus.ROLLED_BACK);
                dependencyResult.setMessage("Rolled back: downstream stage " + failedStage + " failed, invalidating this result");
                run.audit(dependency, EventType.ROLLBACK, "Rolled back because downstream stage " + failedStage + " failed permanently");
                rolledBackAny = true;
            }
        }
        if (rolledBackAny) {
            run.setRollbackCount(run.getRollbackCount() + 1);
            run.setStatus(RunStatus.ROLLED_BACK);
        } else {
            run.setStatus(RunStatus.FAILED);
        }
    }

    /** Dynamic re-planning: brownfield schema impact discovered after implementation ran. */
    private void replan(WorkflowRun run, StageName triggeringStage) {
        if (run.getDynamicallyAddedStages().contains(StageName.DATA_MIGRATION_REVIEW)) {
            return;
        }
        run.getDynamicallyAddedStages().add(StageName.DATA_MIGRATION_REVIEW);
        run.getStageResults().put(StageName.DATA_MIGRATION_REVIEW,
            StageResult.builder().stage(StageName.DATA_MIGRATION_REVIEW).status(StageStatus.PENDING).attempts(0).build());
        run.setReplanCount(run.getReplanCount() + 1);
        run.audit(triggeringStage, EventType.RE_PLAN,
            "Inserted DATA_MIGRATION_REVIEW gate ahead of UNIT_TESTING due to schema impact from " + triggeringStage);
    }

    private List<StageName> readyStages(WorkflowRun run) {
        Set<StageName> active = activeStages(run);
        return active.stream()
            .filter(stage -> run.getStageResults().get(stage).getStatus() == StageStatus.PENDING)
            .filter(stage -> graph.effectiveDependsOn(stage, run.getDynamicallyAddedStages()).stream()
                .allMatch(dep -> isSatisfied(run, dep)))
            .collect(Collectors.toList());
    }

    private boolean isSatisfied(WorkflowRun run, StageName dep) {
        StageStatus status = run.getStageResults().get(dep).getStatus();
        return status == StageStatus.PASSED || status == StageStatus.FALLBACK_APPLIED || status == StageStatus.SKIPPED;
    }

    private boolean allActiveStagesResolved(WorkflowRun run) {
        return activeStages(run).stream().allMatch(stage -> {
            StageStatus status = run.getStageResults().get(stage).getStatus();
            return status == StageStatus.PASSED || status == StageStatus.FALLBACK_APPLIED || status == StageStatus.SKIPPED;
        });
    }

    private Set<StageName> activeStages(WorkflowRun run) {
        return baseStages().stream()
            .filter(s -> s != StageName.DATA_MIGRATION_REVIEW || run.getDynamicallyAddedStages().contains(s))
            .collect(Collectors.toSet());
    }

    private Set<StageName> baseStages() {
        return graph.allStages();
    }

    private void completeRun(WorkflowRun run) {
        run.setStatus(RunStatus.COMPLETED);
        run.audit(null, EventType.RUN_COMPLETED, "All stages passed; run complete");
        finalizeLatency(run);
    }

    private void finalizeLatency(WorkflowRun run) {
        if (run.getCreatedAt() != null) {
            run.setEndToEndLatencyMillis(Instant.now().toEpochMilli() - run.getCreatedAt().toEpochMilli());
        }
    }

    private boolean isTerminal(RunStatus status) {
        return status == RunStatus.COMPLETED || status == RunStatus.FAILED
            || status == RunStatus.ROLLED_BACK || status == RunStatus.STOPPED;
    }
}
