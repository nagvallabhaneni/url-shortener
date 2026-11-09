package com.urlshortener.orchestration.service;

import com.urlshortener.exception.UrlException;
import com.urlshortener.orchestration.dto.MetricsResponse;
import com.urlshortener.orchestration.dto.StartRunRequest;
import com.urlshortener.orchestration.engine.OrchestrationEngine;
import com.urlshortener.orchestration.model.RunStatus;
import com.urlshortener.orchestration.model.StageName;
import com.urlshortener.orchestration.model.WorkflowRun;
import com.urlshortener.orchestration.repository.WorkflowRunRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Application-facing façade over {@link OrchestrationEngine}: creates and
 * persists {@link WorkflowRun} aggregates and translates human decisions
 * (approve/reject/stop) into engine calls.
 */
@Slf4j
@Service
public class OrchestrationService {

    private final OrchestrationEngine engine;
    private final WorkflowRunRepository repository;

    public OrchestrationService(OrchestrationEngine engine, WorkflowRunRepository repository) {
        this.engine = engine;
        this.repository = repository;
    }

    public WorkflowRun startRun(StartRunRequest request) {
        WorkflowRun run = WorkflowRun.builder()
            .id(UUID.randomUUID().toString())
            .requirementText(request.getRequirementText())
            .scenarioType(request.getScenarioType())
            .status(RunStatus.CREATED)
            .createdAt(Instant.now())
            .simulatedFailures(request.getSimulatedFailures() != null
                ? new LinkedHashMap<>(request.getSimulatedFailures())
                : new LinkedHashMap<>())
            .build();

        engine.start(run);
        run.setUpdatedAt(Instant.now());
        repository.save(run);
        log.info("Orchestration run {} started for scenario {} -> status {}", run.getId(), run.getScenarioType(), run.getStatus());
        return run;
    }

    public WorkflowRun getRun(String runId) {
        return repository.findById(runId)
            .orElseThrow(() -> new UrlException("Workflow run not found: " + runId));
    }

    public WorkflowRun approve(String runId, StageName stage, String approver) {
        WorkflowRun run = getRun(runId);
        engine.approve(run, stage, approver != null ? approver : "unknown-approver");
        run.setUpdatedAt(Instant.now());
        repository.save(run);
        return run;
    }

    public WorkflowRun reject(String runId, StageName stage, String approver, String reason) {
        WorkflowRun run = getRun(runId);
        engine.reject(run, stage, approver != null ? approver : "unknown-approver", reason != null ? reason : "no reason given");
        run.setUpdatedAt(Instant.now());
        repository.save(run);
        return run;
    }

    public WorkflowRun stop(String runId) {
        WorkflowRun run = getRun(runId);
        engine.stop(run);
        run.setUpdatedAt(Instant.now());
        repository.save(run);
        return run;
    }

    public MetricsResponse aggregateMetrics() {
        List<WorkflowRun> runs = repository.findAll();
        long total = runs.size();
        if (total == 0) {
            return MetricsResponse.builder().totalRuns(0).build();
        }
        Set<RunStatus> terminalFailureStates = Set.of(RunStatus.FAILED, RunStatus.ROLLED_BACK);
        long completed = runs.stream().filter(r -> r.getStatus() == RunStatus.COMPLETED).count();
        long failed = runs.stream().filter(r -> terminalFailureStates.contains(r.getStatus())).count();

        double avgRetry = runs.stream().mapToInt(WorkflowRun::getRetryCount).average().orElse(0);
        double avgRollback = runs.stream().mapToInt(WorkflowRun::getRollbackCount).average().orElse(0);
        double avgReplan = runs.stream().mapToInt(WorkflowRun::getReplanCount).average().orElse(0);
        double avgLatency = runs.stream()
            .filter(r -> r.getEndToEndLatencyMillis() != null)
            .mapToLong(WorkflowRun::getEndToEndLatencyMillis)
            .average().orElse(0);

        return MetricsResponse.builder()
            .totalRuns(total)
            .completedRuns(completed)
            .failedOrRolledBackRuns(failed)
            .successRatePercent(total == 0 ? 0 : (completed * 100.0) / total)
            .averageRetryCount(avgRetry)
            .averageRollbackCount(avgRollback)
            .averageReplanCount(avgReplan)
            .averageEndToEndLatencyMillis(avgLatency)
            .build();
    }
}
