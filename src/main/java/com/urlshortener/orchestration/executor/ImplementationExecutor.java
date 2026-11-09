package com.urlshortener.orchestration.executor;

import com.urlshortener.orchestration.model.ScenarioType;
import com.urlshortener.orchestration.model.StageName;
import com.urlshortener.orchestration.model.WorkflowRun;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Implementation stage. Detects, from the requirement text, whether the
 * change touches the persisted data schema; if so it flags
 * {@code triggersReplan} so the engine dynamically inserts a
 * DATA_MIGRATION_REVIEW gate ahead of testing — a concrete example of
 * "re-plan when upstream outputs change."
 */
@Component
public class ImplementationExecutor extends SimulatableStageExecutor {

    private static final java.util.List<String> SCHEMA_MARKERS =
        java.util.List.of("schema", "field", "column", "migrate", "migration");

    public ImplementationExecutor() {
        super(StageName.IMPLEMENTATION);
    }

    @Override
    protected StageOutcome doExecute(WorkflowRun run, int attemptNumber) {
        String normalized = run.getRequirementText().toLowerCase(Locale.ROOT);
        boolean schemaImpact = run.getScenarioType() == ScenarioType.BROWNFIELD
            && SCHEMA_MARKERS.stream().anyMatch(normalized::contains);

        return StageOutcome.builder()
            .success(true)
            .message(schemaImpact
                ? "Implementation touches persisted schema; migration review required before testing."
                : "Implementation complete; no schema impact detected.")
            .artifacts(java.util.Map.of("schemaImpact", String.valueOf(schemaImpact)))
            .triggersReplan(schemaImpact)
            .build();
    }
}
