package com.urlshortener.orchestration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** A single immutable entry in a workflow run's audit trail. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    private Instant timestamp;
    private StageName stage;
    private EventType eventType;
    private String detail;
}
