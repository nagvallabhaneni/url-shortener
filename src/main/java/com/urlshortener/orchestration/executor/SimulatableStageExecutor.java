package com.urlshortener.orchestration.executor;

import com.urlshortener.orchestration.model.StageName;
import com.urlshortener.orchestration.model.WorkflowRun;

/**
 * Base class handling the shared "simulated transient failure" concern so
 * individual stage executors only implement their real logic. A run can
 * request that a given stage fail its first N attempts (via
 * {@code WorkflowRun#simulatedFailures}) to exercise the engine's bounded
 * retry, fallback, and rollback behavior without needing genuinely flaky
 * infrastructure in a demo/prototype environment.
 */
public abstract class SimulatableStageExecutor implements StageExecutor {

    private final StageName stage;

    protected SimulatableStageExecutor(StageName stage) {
        this.stage = stage;
    }

    @Override
    public final StageOutcome execute(WorkflowRun run, int attemptNumber) {
        int forcedFailures = run.getSimulatedFailures().getOrDefault(stage, 0);
        if (attemptNumber <= forcedFailures) {
            return StageOutcome.builder()
                .success(false)
                .message("Simulated transient failure (attempt " + attemptNumber + " of " + forcedFailures + " forced failures)")
                .build();
        }
        return doExecute(run, attemptNumber);
    }

    protected abstract StageOutcome doExecute(WorkflowRun run, int attemptNumber);
}
