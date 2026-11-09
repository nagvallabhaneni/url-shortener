package com.urlshortener.orchestration.executor;

import com.urlshortener.orchestration.model.StageName;
import com.urlshortener.orchestration.model.WorkflowRun;
import org.springframework.stereotype.Component;

/**
 * Final release-readiness gate. Always requires human approval (see
 * DependencyGraph); by the time this executes, the gate has already been
 * granted, so it simply records the release sign-off.
 */
@Component
public class ReleaseReadinessExecutor extends SimulatableStageExecutor {

    public ReleaseReadinessExecutor() {
        super(StageName.RELEASE_READINESS);
    }

    @Override
    protected StageOutcome doExecute(WorkflowRun run, int attemptNumber) {
        return StageOutcome.builder()
            .success(true)
            .message("Release approved by human reviewer; all artifacts signed off.")
            .build();
    }
}
