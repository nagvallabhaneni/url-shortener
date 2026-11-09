package com.urlshortener.orchestration.model;

/** Lifecycle status of an individual stage execution within a workflow run. */
public enum StageStatus {
    PENDING,
    RUNNING,
    RETRYING,
    WAITING_APPROVAL,
    PASSED,
    FAILED,
    FALLBACK_APPLIED,
    ROLLED_BACK,
    SKIPPED
}
