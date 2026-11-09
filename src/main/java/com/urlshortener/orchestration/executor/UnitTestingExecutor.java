package com.urlshortener.orchestration.executor;

import com.urlshortener.orchestration.model.StageName;
import com.urlshortener.orchestration.model.WorkflowRun;
import org.springframework.stereotype.Component;

/** Unit testing exit gate: all unit tests must pass before integration testing starts. */
@Component
public class UnitTestingExecutor extends SimulatableStageExecutor {

    public UnitTestingExecutor() {
        super(StageName.UNIT_TESTING);
    }

    @Override
    protected StageOutcome doExecute(WorkflowRun run, int attemptNumber) {
        return StageOutcome.builder()
            .success(true)
            .message("All unit tests passed.")
            .artifacts(java.util.Map.of("testsRun", "42", "testsFailed", "0"))
            .build();
    }
}
