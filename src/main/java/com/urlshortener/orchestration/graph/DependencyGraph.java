package com.urlshortener.orchestration.graph;

import com.urlshortener.orchestration.model.StageName;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static com.urlshortener.orchestration.model.StageName.*;

/**
 * The base SDLC dependency graph described in ORCHESTRATION.md, expressed as
 * executable state instead of prose:
 *
 * <pre>
 * REQUIREMENTS -&gt; ARCHITECTURE_DESIGN -&gt; { IMPLEMENTATION, SCHEMA_DESIGN } (parallel)
 *   -&gt; [sync] UNIT_TESTING -&gt; INTEGRATION_TESTING
 *   -&gt; { API_DOCUMENTATION, COVERAGE_ENFORCEMENT } (parallel)
 *   -&gt; [sync] RELEASE_READINESS (human approval gate)
 * </pre>
 *
 * DATA_MIGRATION_REVIEW is intentionally absent here; it is inserted at
 * runtime by the engine for brownfield changes that touch the data schema.
 */
@Component
public final class DependencyGraph {

    private final Map<StageName, StageDefinition> definitions = new EnumMap<>(StageName.class);

    public DependencyGraph() {
        register(REQUIREMENTS, Set.of(), false, 2, false);
        register(ARCHITECTURE_DESIGN, Set.of(REQUIREMENTS), false, 2, false);
        register(IMPLEMENTATION, Set.of(ARCHITECTURE_DESIGN), false, 2, false);
        register(SCHEMA_DESIGN, Set.of(ARCHITECTURE_DESIGN), false, 2, false);
        register(DATA_MIGRATION_REVIEW, Set.of(IMPLEMENTATION, SCHEMA_DESIGN), true, 1, false);
        register(UNIT_TESTING, Set.of(IMPLEMENTATION, SCHEMA_DESIGN), false, 3, false);
        register(INTEGRATION_TESTING, Set.of(UNIT_TESTING), false, 3, false);
        register(API_DOCUMENTATION, Set.of(INTEGRATION_TESTING), false, 2, true);
        register(COVERAGE_ENFORCEMENT, Set.of(INTEGRATION_TESTING), false, 2, true);
        register(RELEASE_READINESS, Set.of(API_DOCUMENTATION, COVERAGE_ENFORCEMENT), true, 1, false);
    }

    private void register(StageName name, Set<StageName> dependsOn, boolean approval, int retries, boolean fallback) {
        definitions.put(name, new StageDefinition(name, dependsOn, approval, retries, fallback));
    }

    public StageDefinition get(StageName name) {
        StageDefinition def = definitions.get(name);
        if (def == null) {
            throw new IllegalArgumentException("Unknown stage: " + name);
        }
        return def;
    }

    public Set<StageName> allStages() {
        return definitions.keySet();
    }

    /**
     * Effective dependency set for a stage within a specific run. This keeps
     * the graph definition itself immutable and shared across runs, while
     * still allowing per-run dynamic re-planning: when a run has injected
     * DATA_MIGRATION_REVIEW (e.g. a brownfield change turned out to impact
     * the schema), UNIT_TESTING gains that extra dependency for that run only.
     */
    public Set<StageName> effectiveDependsOn(StageName stage, java.util.Collection<StageName> dynamicallyAddedStages) {
        Set<StageName> deps = new java.util.HashSet<>(get(stage).dependsOn());
        if (stage == UNIT_TESTING && dynamicallyAddedStages.contains(DATA_MIGRATION_REVIEW)) {
            deps.add(DATA_MIGRATION_REVIEW);
        }
        return deps;
    }
}
