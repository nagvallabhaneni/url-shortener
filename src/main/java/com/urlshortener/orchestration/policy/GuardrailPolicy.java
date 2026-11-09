package com.urlshortener.orchestration.policy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Security/compliance/change-control guardrails evaluated before the
 * IMPLEMENTATION stage is allowed to run. A violation blocks the stage
 * outright (no retries) and forces a human decision, matching the
 * "policy guardrails" requirement rather than silently failing tests later.
 */
@Component
public class GuardrailPolicy {

    private static final List<String> BLOCKED_PHRASES = List.of(
        "disable auth",
        "disable authentication",
        "hardcode secret",
        "hard-code secret",
        "hardcoded credential",
        "skip validation",
        "bypass security",
        "store password in plain text",
        "plaintext password"
    );

    /** Returns a violation reason if the requirement text breaches a guardrail, empty otherwise. */
    public Optional<String> evaluate(String requirementText) {
        if (requirementText == null) {
            return Optional.empty();
        }
        String normalized = requirementText.toLowerCase(Locale.ROOT);
        return BLOCKED_PHRASES.stream()
            .filter(normalized::contains)
            .findFirst()
            .map(phrase -> "Requirement text violates security/compliance guardrail: contains '" + phrase + "'");
    }
}
