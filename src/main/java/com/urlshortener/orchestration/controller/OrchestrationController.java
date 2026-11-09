package com.urlshortener.orchestration.controller;

import com.urlshortener.exception.UrlException;
import com.urlshortener.orchestration.dto.ApprovalDecisionRequest;
import com.urlshortener.orchestration.dto.MetricsResponse;
import com.urlshortener.orchestration.dto.StartRunRequest;
import com.urlshortener.orchestration.model.StageName;
import com.urlshortener.orchestration.model.WorkflowRun;
import com.urlshortener.orchestration.service.OrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST surface for the agentic SDLC orchestration engine: start a run for a
 * greenfield/brownfield/ambiguous requirement, inspect its dependency-graph
 * state and audit trail, resolve human approval gates, safe-stop a run, and
 * pull aggregate reliability metrics.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orchestration")
@Tag(name = "Orchestration", description = "Agentic SDLC orchestration engine")
public class OrchestrationController {

    private final OrchestrationService service;

    public OrchestrationController(OrchestrationService service) {
        this.service = service;
    }

    @PostMapping("/runs")
    @Operation(summary = "Start a new orchestration run for a requirement")
    public ResponseEntity<WorkflowRun> startRun(@Valid @RequestBody StartRunRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.startRun(request));
    }

    @GetMapping("/runs/{runId}")
    @Operation(summary = "Get the current state, dependency-graph progress, and audit trail of a run")
    public ResponseEntity<WorkflowRun> getRun(@PathVariable String runId) {
        try {
            return ResponseEntity.ok(service.getRun(runId));
        } catch (UrlException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/runs/{runId}/stages/{stage}/approve")
    @Operation(summary = "Grant a pending human approval gate and resume execution")
    public ResponseEntity<WorkflowRun> approve(@PathVariable String runId,
                                                @PathVariable StageName stage,
                                                @RequestBody(required = false) ApprovalDecisionRequest request) {
        try {
            String approver = request != null ? request.getApprover() : null;
            return ResponseEntity.ok(service.approve(runId, stage, approver));
        } catch (UrlException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/runs/{runId}/stages/{stage}/reject")
    @Operation(summary = "Reject a pending human approval gate; rolls back and stops the run")
    public ResponseEntity<WorkflowRun> reject(@PathVariable String runId,
                                               @PathVariable StageName stage,
                                               @RequestBody(required = false) ApprovalDecisionRequest request) {
        try {
            String approver = request != null ? request.getApprover() : null;
            String reason = request != null ? request.getReason() : null;
            return ResponseEntity.ok(service.reject(runId, stage, approver, reason));
        } catch (UrlException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/runs/{runId}/stop")
    @Operation(summary = "Safe-stop a run; already-completed stage results are preserved")
    public ResponseEntity<WorkflowRun> stop(@PathVariable String runId) {
        try {
            return ResponseEntity.ok(service.stop(runId));
        } catch (UrlException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/metrics")
    @Operation(summary = "Aggregate reliability metrics across all orchestration runs")
    public ResponseEntity<MetricsResponse> metrics() {
        return ResponseEntity.ok(service.aggregateMetrics());
    }
}
