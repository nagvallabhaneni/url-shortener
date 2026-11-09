package com.urlshortener.orchestration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistent state for one requirement moving through the agentic SDLC
 * orchestration pipeline. This is the aggregate root the engine mutates as it
 * traverses the dependency graph.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "workflow_runs")
public class WorkflowRun {
    @Id
    private String id;

    private String requirementText;
    private ScenarioType scenarioType;
    private RunStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Stage-by-stage execution state. Backed by a {@link java.util.concurrent.ConcurrentHashMap}
     * because independent stages within a batch execute concurrently on the
     * engine's worker pool and may read/write distinct entries at the same time.
     */
    @Builder.Default
    private Map<StageName, StageResult> stageResults = new java.util.concurrent.ConcurrentHashMap<>();

    /** Extra stages injected into the graph at runtime (dynamic re-planning). */
    @Builder.Default
    private List<StageName> dynamicallyAddedStages = new ArrayList<>();

    /** Stage currently blocked awaiting a human decision, if any. */
    private StageName pendingApprovalStage;

    /** Stages whose approval gate has been explicitly granted by a human. */
    @Builder.Default
    private java.util.Set<StageName> approvedStages = new java.util.HashSet<>();

    @Builder.Default
    private List<AuditEvent> auditTrail = new ArrayList<>();

    /** Optional per-stage transient-failure injection used to exercise retry/rollback paths. */
    @Builder.Default
    private Map<StageName, Integer> simulatedFailures = new LinkedHashMap<>();

    // --- reliability metrics ---
    @Builder.Default
    private int retryCount = 0;
    @Builder.Default
    private int rollbackCount = 0;
    @Builder.Default
    private int replanCount = 0;
    private Long endToEndLatencyMillis;

    /**
     * Appends an audit entry. Synchronized because multiple stages in the
     * same parallel batch may record events concurrently; using the same
     * monitor as the engine's per-stage-result critical sections keeps
     * audit ordering consistent with state transitions.
     */
    public synchronized void audit(StageName stage, EventType type, String detail) {
        auditTrail.add(AuditEvent.builder()
            .timestamp(Instant.now())
            .stage(stage)
            .eventType(type)
            .detail(detail)
            .build());
    }
}
