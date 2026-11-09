package com.urlshortener.orchestration.executor;

import com.urlshortener.orchestration.model.StageName;
import com.urlshortener.orchestration.model.WorkflowRun;
import org.springframework.stereotype.Component;

/** API/schema documentation generation, runs in parallel with coverage enforcement. */
@Component
public class ApiDocumentationExecutor extends SimulatableStageExecutor {

    public ApiDocumentationExecutor() {
        super(StageName.API_DOCUMENTATION);
    }

    @Override
    protected StageOutcome doExecute(WorkflowRun run, int attemptNumber) {
        return StageOutcome.builder()
            .success(true)
            .message("OpenAPI spec and README updated to reflect the change.")
            .build();
    }
}
