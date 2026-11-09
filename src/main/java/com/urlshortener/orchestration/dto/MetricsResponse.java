package com.urlshortener.orchestration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Aggregate reliability metrics across all workflow runs. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsResponse {
    private long totalRuns;
    private long completedRuns;
    private long failedOrRolledBackRuns;
    private double successRatePercent;
    private double averageRetryCount;
    private double averageRollbackCount;
    private double averageReplanCount;
    private double averageEndToEndLatencyMillis;
}
