package com.urlshortener.orchestration.executor;

import com.urlshortener.orchestration.model.StageName;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/** Wires each {@link StageName} to its concrete {@link StageExecutor} implementation. */
@Component
public class StageExecutorRegistry {

    private final Map<StageName, StageExecutor> executors = new EnumMap<>(StageName.class);

    public StageExecutorRegistry(RequirementsExecutor requirements,
                                  ArchitectureDesignExecutor architectureDesign,
                                  ImplementationExecutor implementation,
                                  SchemaDesignExecutor schemaDesign,
                                  DataMigrationReviewExecutor dataMigrationReview,
                                  UnitTestingExecutor unitTesting,
                                  IntegrationTestingExecutor integrationTesting,
                                  ApiDocumentationExecutor apiDocumentation,
                                  CoverageEnforcementExecutor coverageEnforcement,
                                  ReleaseReadinessExecutor releaseReadiness) {
        executors.put(StageName.REQUIREMENTS, requirements);
        executors.put(StageName.ARCHITECTURE_DESIGN, architectureDesign);
        executors.put(StageName.IMPLEMENTATION, implementation);
        executors.put(StageName.SCHEMA_DESIGN, schemaDesign);
        executors.put(StageName.DATA_MIGRATION_REVIEW, dataMigrationReview);
        executors.put(StageName.UNIT_TESTING, unitTesting);
        executors.put(StageName.INTEGRATION_TESTING, integrationTesting);
        executors.put(StageName.API_DOCUMENTATION, apiDocumentation);
        executors.put(StageName.COVERAGE_ENFORCEMENT, coverageEnforcement);
        executors.put(StageName.RELEASE_READINESS, releaseReadiness);
    }

    public StageExecutor get(StageName stage) {
        StageExecutor executor = executors.get(stage);
        if (executor == null) {
            throw new IllegalStateException("No executor registered for stage: " + stage);
        }
        return executor;
    }
}
