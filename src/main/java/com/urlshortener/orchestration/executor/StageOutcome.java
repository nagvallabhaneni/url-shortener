package com.urlshortener.orchestration.executor;

import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/** Result of one attempt at executing a stage. */
@Getter
@Builder
public class StageOutcome {
    private final boolean success;
    private final String message;
    @Builder.Default
    private final Map<String, String> artifacts = new HashMap<>();
    /** True if this stage's output should trigger dynamic re-planning of downstream stages. */
    @Builder.Default
    private final boolean triggersReplan = false;
}
