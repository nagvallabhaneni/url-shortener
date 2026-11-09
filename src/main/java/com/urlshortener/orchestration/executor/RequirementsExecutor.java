package com.urlshortener.orchestration.executor;

import com.urlshortener.orchestration.model.ScenarioType;
import com.urlshortener.orchestration.model.StageName;
import com.urlshortener.orchestration.model.WorkflowRun;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Requirements stage: interprets intent, flags ambiguous phrasing, and
 * normalizes the requirement into a concrete engineering statement. For
 * AMBIGUOUS scenarios it records the default assumption the agent applied so
 * a human reviewer can see exactly what was inferred (decision lineage).
 */
@Component
public class RequirementsExecutor extends SimulatableStageExecutor {

    private static final List<String> AMBIGUITY_MARKERS = List.of(
        "improve", "better", "faster", "some", "maybe", "etc", "and so on", "user-friendly", "optimize"
    );

    public RequirementsExecutor() {
        super(StageName.REQUIREMENTS);
    }

    @Override
    protected StageOutcome doExecute(WorkflowRun run, int attemptNumber) {
        String text = run.getRequirementText();
        String normalized = text.toLowerCase(Locale.ROOT);
        boolean ambiguous = run.getScenarioType() == ScenarioType.AMBIGUOUS
            || AMBIGUITY_MARKERS.stream().anyMatch(normalized::contains);

        StageOutcome.StageOutcomeBuilder outcome = StageOutcome.builder().success(true);
        if (ambiguous) {
            String assumption = "Interpreted as: implement the smallest change that satisfies '" + text
                + "' while preserving existing API contracts; non-functional adjectives resolved to"
                + " current performance targets defined in guidelines.md.";
            outcome.artifacts(java.util.Map.of(
                "ambiguityDetected", "true",
                "resolvedAssumption", assumption
            ));
            outcome.message("Requirement contains ambiguous phrasing; normalized with a documented assumption.");
        } else {
            outcome.artifacts(java.util.Map.of("ambiguityDetected", "false"));
            outcome.message("Requirement is well-defined; no clarification needed.");
        }
        return outcome.build();
    }
}
