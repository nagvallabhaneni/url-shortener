package com.urlshortener.orchestration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Execution record for one stage within one workflow run. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageResult {
    private StageName stage;
    private StageStatus status;
    private int attempts;
    private Instant firstAttemptAt;
    private Instant completedAt;
    private String message;
    @Builder.Default
    private Map<String, String> artifacts = new HashMap<>();

    /** Milliseconds between the first attempt and final resolution (used for MTTR). */
    public long durationMillis() {
        if (firstAttemptAt == null || completedAt == null) {
            return 0L;
        }
        return completedAt.toEpochMilli() - firstAttemptAt.toEpochMilli();
    }
}
