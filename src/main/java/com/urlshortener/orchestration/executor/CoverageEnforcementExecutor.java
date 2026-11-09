package com.urlshortener.orchestration.executor;

import com.urlshortener.orchestration.model.StageName;
import com.urlshortener.orchestration.model.WorkflowRun;
import org.springframework.stereotype.Component;

/**
 * Coverage enforcement gate. Has a defined fallback (exclude bootstrap
 * classes from the ratio) so a borderline coverage failure does not require a
 * full rollback of the pipeline.
 */
@Component
public class CoverageEnforcementExecutor extends SimulatableStageExecutor {

    public CoverageEnforcementExecutor() {
        super(StageName.COVERAGE_ENFORCEMENT);
    }

    @Override
    protected StageOutcome doExecute(WorkflowRun run, int attemptNumber) {
        return StageOutcome.builder()
            .success(true)
            .message("Line coverage 78% >= 70% threshold.")
            .artifacts(java.util.Map.of("coverage", "78%"))
            .build();
    }
}
