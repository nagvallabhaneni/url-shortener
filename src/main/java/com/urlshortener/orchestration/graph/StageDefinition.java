package com.urlshortener.orchestration.graph;

import com.urlshortener.orchestration.model.StageName;

import java.util.Set;

/**
 * Static definition of one stage's place in the SDLC dependency graph:
 * what it depends on, whether it needs a human approval gate before running,
 * how many bounded retries it gets, and whether a fallback exists in lieu of
 * a hard rollback when retries are exhausted.
 */
public record StageDefinition(
        StageName name,
        Set<StageName> dependsOn,
        boolean requiresApproval,
        int maxRetries,
        boolean hasFallback
) {
}
