package com.urlshortener.orchestration.executor;

import com.urlshortener.orchestration.model.WorkflowRun;

/**
 * Executes one attempt of a single SDLC stage for a given workflow run.
 * Implementations must be deterministic and side-effect-free with respect to
 * the outside world (they only read/derive from the run's state), so retries
 * are safe to re-invoke.
 */
public interface StageExecutor {
    StageOutcome execute(WorkflowRun run, int attemptNumber);
}
