package com.urlshortener.orchestration.model;

/** Category of an audit trail entry, used for filtering and reporting. */
public enum EventType {
    STAGE_STARTED,
    STAGE_PASSED,
    STAGE_FAILED,
    RETRY_SCHEDULED,
    FALLBACK_APPLIED,
    ROLLBACK,
    GATE_BLOCKED,
    APPROVAL_GRANTED,
    APPROVAL_REJECTED,
    GUARDRAIL_VIOLATION,
    RE_PLAN,
    RUN_COMPLETED,
    RUN_STOPPED
}
