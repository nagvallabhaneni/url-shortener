package com.urlshortener.orchestration.model;

/**
 * The set of SDLC stages the orchestration engine coordinates.
 * DATA_MIGRATION_REVIEW is not part of the base graph; it is injected
 * dynamically by the engine when a brownfield change is detected to impact
 * the data schema (see OrchestrationEngine#replanIfNeeded).
 */
public enum StageName {
    REQUIREMENTS,
    ARCHITECTURE_DESIGN,
    IMPLEMENTATION,
    SCHEMA_DESIGN,
    DATA_MIGRATION_REVIEW,
    UNIT_TESTING,
    INTEGRATION_TESTING,
    API_DOCUMENTATION,
    COVERAGE_ENFORCEMENT,
    RELEASE_READINESS
}
