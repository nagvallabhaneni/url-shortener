package com.urlshortener.orchestration.executor;

import com.urlshortener.orchestration.model.StageName;
import com.urlshortener.orchestration.model.WorkflowRun;
import org.springframework.stereotype.Component;

/**
 * Dynamically-injected gate: reviews the migration plan for schema-impacting
 * brownfield changes. Always requires human approval (see DependencyGraph)
 * before unit testing can proceed against the new schema.
 */
@Component
public class DataMigrationReviewExecutor extends SimulatableStageExecutor {

    public DataMigrationReviewExecutor() {
        super(StageName.DATA_MIGRATION_REVIEW);
    }

    @Override
    protected StageOutcome doExecute(WorkflowRun run, int attemptNumber) {
        return StageOutcome.builder()
            .success(true)
            .message("Migration plan reviewed and approved by human checkpoint; safe to proceed to testing.")
            .build();
    }
}
