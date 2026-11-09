package com.urlshortener.orchestration.executor;

import com.urlshortener.orchestration.model.ScenarioType;
import com.urlshortener.orchestration.model.StageName;
import com.urlshortener.orchestration.model.WorkflowRun;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Architecture/design stage: for brownfield changes, identifies which
 * existing modules are impacted by scanning the requirement text against the
 * known component vocabulary of the URL shortener codebase, demonstrating
 * codebase reasoning rather than treating every request as greenfield.
 */
@Component
public class ArchitectureDesignExecutor extends SimulatableStageExecutor {

    private static final java.util.Map<String, String> MODULE_KEYWORDS = java.util.Map.ofEntries(
        java.util.Map.entry("shorten", "UrlService / UrlController"),
        java.util.Map.entry("redirect", "RedirectController / UrlService"),
        java.util.Map.entry("analytics", "RedirectLog / RedirectLogRepository"),
        java.util.Map.entry("expiry", "UrlMapping.expiresAt"),
        java.util.Map.entry("expire", "UrlMapping.expiresAt"),
        java.util.Map.entry("validation", "UrlValidator"),
        java.util.Map.entry("hash", "HashGenerator"),
        java.util.Map.entry("alias", "UrlMapping / UrlRepository"),
        java.util.Map.entry("custom code", "UrlMapping / UrlRepository"),
        java.util.Map.entry("schema", "UrlMapping (MongoDB document schema)"),
        java.util.Map.entry("field", "UrlMapping (MongoDB document schema)")
    );

    public ArchitectureDesignExecutor() {
        super(StageName.ARCHITECTURE_DESIGN);
    }

    @Override
    protected StageOutcome doExecute(WorkflowRun run, int attemptNumber) {
        String normalized = run.getRequirementText().toLowerCase(Locale.ROOT);
        Set<String> impacted = new LinkedHashSet<>();
        MODULE_KEYWORDS.forEach((keyword, module) -> {
            if (normalized.contains(keyword)) {
                impacted.add(module);
            }
        });

        String decision;
        if (run.getScenarioType() == ScenarioType.GREENFIELD || impacted.isEmpty()) {
            decision = "Greenfield design: new module(s) will be added alongside existing UrlService boundary; "
                + "no existing contracts change.";
        } else {
            decision = "Brownfield design: impacted components = " + impacted
                + ". Change will be scoped to these modules to minimize blast radius.";
        }

        return StageOutcome.builder()
            .success(true)
            .message(decision)
            .artifacts(java.util.Map.of(
                "impactedModules", String.join(",", impacted),
                "designDecision", decision
            ))
            .build();
    }
}
