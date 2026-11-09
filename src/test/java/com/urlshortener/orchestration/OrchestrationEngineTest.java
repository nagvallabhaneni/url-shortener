package com.urlshortener.orchestration;

import com.urlshortener.orchestration.engine.OrchestrationEngine;
import com.urlshortener.orchestration.executor.ApiDocumentationExecutor;
import com.urlshortener.orchestration.executor.ArchitectureDesignExecutor;
import com.urlshortener.orchestration.executor.CoverageEnforcementExecutor;
import com.urlshortener.orchestration.executor.DataMigrationReviewExecutor;
import com.urlshortener.orchestration.executor.ImplementationExecutor;
import com.urlshortener.orchestration.executor.IntegrationTestingExecutor;
import com.urlshortener.orchestration.executor.ReleaseReadinessExecutor;
import com.urlshortener.orchestration.executor.RequirementsExecutor;
import com.urlshortener.orchestration.executor.SchemaDesignExecutor;
import com.urlshortener.orchestration.executor.StageExecutorRegistry;
import com.urlshortener.orchestration.executor.UnitTestingExecutor;
import com.urlshortener.orchestration.graph.DependencyGraph;
import com.urlshortener.orchestration.model.EventType;
import com.urlshortener.orchestration.model.RunStatus;
import com.urlshortener.orchestration.model.ScenarioType;
import com.urlshortener.orchestration.model.StageName;
import com.urlshortener.orchestration.model.StageStatus;
import com.urlshortener.orchestration.model.WorkflowRun;
import com.urlshortener.orchestration.policy.GuardrailPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OrchestrationEngine Tests")
class OrchestrationEngineTest {

    private OrchestrationEngine engine;

    @BeforeEach
    void setUp() {
        DependencyGraph graph = new DependencyGraph();
        StageExecutorRegistry registry = new StageExecutorRegistry(
            new RequirementsExecutor(),
            new ArchitectureDesignExecutor(),
            new ImplementationExecutor(),
            new SchemaDesignExecutor(),
            new DataMigrationReviewExecutor(),
            new UnitTestingExecutor(),
            new IntegrationTestingExecutor(),
            new ApiDocumentationExecutor(),
            new CoverageEnforcementExecutor(),
            new ReleaseReadinessExecutor()
        );
        engine = new OrchestrationEngine(graph, registry, new GuardrailPolicy());
    }

    private WorkflowRun newRun(String requirement, ScenarioType type, Map<StageName, Integer> simulatedFailures) {
        WorkflowRun run = WorkflowRun.builder()
            .id(UUID.randomUUID().toString())
            .requirementText(requirement)
            .scenarioType(type)
            .status(RunStatus.CREATED)
            .createdAt(Instant.now())
            .build();
        if (simulatedFailures != null) {
            run.setSimulatedFailures(new java.util.LinkedHashMap<>(simulatedFailures));
        }
        return run;
    }

    @Test
    @DisplayName("Greenfield run executes non-linearly and pauses only at the release approval gate")
    void greenfieldRunPausesAtReleaseGate() {
        WorkflowRun run = newRun("Add a new bulk-import endpoint", ScenarioType.GREENFIELD, null);
        engine.start(run);

        assertEquals(RunStatus.WAITING_APPROVAL, run.getStatus());
        assertEquals(StageName.RELEASE_READINESS, run.getPendingApprovalStage());
        assertEquals(StageStatus.PASSED, run.getStageResults().get(StageName.UNIT_TESTING).getStatus());
        assertEquals(StageStatus.PASSED, run.getStageResults().get(StageName.INTEGRATION_TESTING).getStatus());
        // Parallel branches both completed before the synchronization gate.
        assertEquals(StageStatus.PASSED, run.getStageResults().get(StageName.API_DOCUMENTATION).getStatus());
        assertEquals(StageStatus.PASSED, run.getStageResults().get(StageName.COVERAGE_ENFORCEMENT).getStatus());

        engine.approve(run, StageName.RELEASE_READINESS, "reviewer1");
        assertEquals(RunStatus.COMPLETED, run.getStatus());
        assertTrue(run.getAuditTrail().stream().anyMatch(e -> e.getEventType() == EventType.RUN_COMPLETED));
    }

    @Test
    @DisplayName("Brownfield schema change triggers dynamic re-plan with an extra approval gate")
    void brownfieldSchemaChangeTriggersReplan() {
        WorkflowRun run = newRun("Add a new field to the URL schema for tags", ScenarioType.BROWNFIELD, null);
        engine.start(run);

        assertEquals(RunStatus.WAITING_APPROVAL, run.getStatus());
        assertEquals(StageName.DATA_MIGRATION_REVIEW, run.getPendingApprovalStage());
        assertEquals(1, run.getReplanCount());
        assertTrue(run.getAuditTrail().stream().anyMatch(e -> e.getEventType() == EventType.RE_PLAN));
        // Unit testing must not have started yet - it now depends on the migration review.
        assertEquals(StageStatus.PENDING, run.getStageResults().get(StageName.UNIT_TESTING).getStatus());

        engine.approve(run, StageName.DATA_MIGRATION_REVIEW, "dba-team");
        assertEquals(RunStatus.WAITING_APPROVAL, run.getStatus());
        assertEquals(StageName.RELEASE_READINESS, run.getPendingApprovalStage());
        assertEquals(StageStatus.PASSED, run.getStageResults().get(StageName.UNIT_TESTING).getStatus());

        engine.approve(run, StageName.RELEASE_READINESS, "reviewer1");
        assertEquals(RunStatus.COMPLETED, run.getStatus());
    }

    @Test
    @DisplayName("Bounded retries recover a transiently-failing stage without rollback")
    void boundedRetriesRecoverTransientFailure() {
        WorkflowRun run = newRun("Improve redirect performance", ScenarioType.GREENFIELD,
            Map.of(StageName.UNIT_TESTING, 2));
        engine.start(run);

        assertEquals(StageStatus.PASSED, run.getStageResults().get(StageName.UNIT_TESTING).getStatus());
        assertEquals(3, run.getStageResults().get(StageName.UNIT_TESTING).getAttempts());
        assertTrue(run.getRetryCount() >= 2);
        assertTrue(run.getAuditTrail().stream().anyMatch(e -> e.getEventType() == EventType.RETRY_SCHEDULED));
    }

    @Test
    @DisplayName("Exhausted retries with no fallback and no upstream to invalidate simply fail the run")
    void exhaustedRetriesWithNoFallbackFailTheRun() {
        WorkflowRun run = newRun("Improve redirect performance", ScenarioType.GREENFIELD,
            Map.of(StageName.REQUIREMENTS, 10));
        engine.start(run);

        assertEquals(StageStatus.FAILED, run.getStageResults().get(StageName.REQUIREMENTS).getStatus());
        assertEquals(RunStatus.FAILED, run.getStatus());
        assertEquals(StageStatus.PENDING, run.getStageResults().get(StageName.ARCHITECTURE_DESIGN).getStatus());
    }

    @Test
    @DisplayName("Exhausted retries on a stage with a fallback apply the fallback instead of failing")
    void exhaustedRetriesWithFallbackApplyFallback() {
        WorkflowRun run = newRun("Add a new bulk-import endpoint", ScenarioType.GREENFIELD,
            Map.of(StageName.COVERAGE_ENFORCEMENT, 10));
        engine.start(run);

        assertEquals(StageStatus.FALLBACK_APPLIED, run.getStageResults().get(StageName.COVERAGE_ENFORCEMENT).getStatus());
        assertEquals(RunStatus.WAITING_APPROVAL, run.getStatus());
        assertEquals(StageName.RELEASE_READINESS, run.getPendingApprovalStage());
    }

    @Test
    @DisplayName("Permanent stage failure rolls back its already-passed upstream dependency")
    void rollbackOfPassedDependents() {
        WorkflowRun run = newRun("Add a new bulk-import endpoint", ScenarioType.GREENFIELD,
            Map.of(StageName.INTEGRATION_TESTING, 10));
        engine.start(run);

        assertEquals(RunStatus.ROLLED_BACK, run.getStatus());
        assertEquals(StageStatus.FAILED, run.getStageResults().get(StageName.INTEGRATION_TESTING).getStatus());
        assertEquals(StageStatus.ROLLED_BACK, run.getStageResults().get(StageName.UNIT_TESTING).getStatus());
        assertTrue(run.getRollbackCount() >= 1);
    }

    @Test
    @DisplayName("Security guardrail violation blocks implementation and requires human review")
    void guardrailViolationBlocksImplementation() {
        WorkflowRun run = newRun("Disable auth for the admin endpoint to speed up testing",
            ScenarioType.GREENFIELD, null);
        engine.start(run);

        assertEquals(RunStatus.WAITING_APPROVAL, run.getStatus());
        assertEquals(StageName.IMPLEMENTATION, run.getPendingApprovalStage());
        assertTrue(run.getAuditTrail().stream().anyMatch(e -> e.getEventType() == EventType.GUARDRAIL_VIOLATION));

        engine.reject(run, StageName.IMPLEMENTATION, "security-team", "violates auth policy");
        assertEquals(RunStatus.ROLLED_BACK, run.getStatus());
        assertEquals(StageStatus.ROLLED_BACK, run.getStageResults().get(StageName.IMPLEMENTATION).getStatus());
    }

    @Test
    @DisplayName("Safe-stop halts a run and preserves already-completed stage state")
    void safeStopHaltsRun() {
        WorkflowRun run = newRun("Add a new bulk-import endpoint", ScenarioType.GREENFIELD, null);
        engine.start(run);
        assertEquals(RunStatus.WAITING_APPROVAL, run.getStatus());

        engine.stop(run);
        assertEquals(RunStatus.STOPPED, run.getStatus());
        assertEquals(StageStatus.PASSED, run.getStageResults().get(StageName.UNIT_TESTING).getStatus());
    }
}
