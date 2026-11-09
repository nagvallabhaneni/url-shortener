package com.urlshortener.orchestration.executor;

import com.urlshortener.orchestration.model.StageName;
import com.urlshortener.orchestration.model.WorkflowRun;
import org.springframework.stereotype.Component;

/** Schema/data-model design stage, runs in parallel with implementation. */
@Component
public class SchemaDesignExecutor extends SimulatableStageExecutor {

    public SchemaDesignExecutor() {
        super(StageName.SCHEMA_DESIGN);
    }

    @Override
    protected StageOutcome doExecute(WorkflowRun run, int attemptNumber) {
        return StageOutcome.builder()
            .success(true)
            .message("Data model reviewed; no breaking changes to existing MongoDB document shape.")
            .build();
    }
}
