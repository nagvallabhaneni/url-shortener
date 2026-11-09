package com.urlshortener.orchestration.model;

/** Overall status of a workflow run (a single requirement moving through the SDLC). */
public enum RunStatus {
    CREATED,
    RUNNING,
    WAITING_APPROVAL,
    COMPLETED,
    FAILED,
    ROLLED_BACK,
    STOPPED
}
