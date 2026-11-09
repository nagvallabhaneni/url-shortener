package com.urlshortener.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.orchestration.dto.ApprovalDecisionRequest;
import com.urlshortener.orchestration.dto.StartRunRequest;
import com.urlshortener.orchestration.model.RunStatus;
import com.urlshortener.orchestration.model.ScenarioType;
import com.urlshortener.orchestration.model.StageName;
import com.urlshortener.orchestration.model.WorkflowRun;
import com.urlshortener.orchestration.repository.WorkflowRunRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("OrchestrationController Integration Tests")
class OrchestrationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkflowRunRepository workflowRunRepository;

    private void stubSaveAndFind() {
        when(workflowRunRepository.save(any(WorkflowRun.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Should start a greenfield run and reach the release approval gate")
    void shouldStartGreenfieldRunAndReachReleaseGate() throws Exception {
        stubSaveAndFind();
        StartRunRequest request = new StartRunRequest("Add a bulk-import endpoint", ScenarioType.GREENFIELD, null);

        mockMvc.perform(post("/api/v1/orchestration/runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(RunStatus.WAITING_APPROVAL.name()))
            .andExpect(jsonPath("$.pendingApprovalStage").value(StageName.RELEASE_READINESS.name()));
    }

    @Test
    @DisplayName("Should approve a pending gate and complete the run")
    void shouldApproveGateAndCompleteRun() throws Exception {
        stubSaveAndFind();
        StartRunRequest request = new StartRunRequest("Add a bulk-import endpoint", ScenarioType.GREENFIELD, null);

        String body = mockMvc.perform(post("/api/v1/orchestration/runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn().getResponse().getContentAsString();
        WorkflowRun created = objectMapper.readValue(body, WorkflowRun.class);

        when(workflowRunRepository.findById(created.getId())).thenReturn(java.util.Optional.of(created));

        mockMvc.perform(post("/api/v1/orchestration/runs/{runId}/stages/{stage}/approve",
                created.getId(), StageName.RELEASE_READINESS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ApprovalDecisionRequest("reviewer1", null))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(RunStatus.COMPLETED.name()));
    }

    @Test
    @DisplayName("Should return 404 for an unknown run")
    void shouldReturn404ForUnknownRun() throws Exception {
        when(workflowRunRepository.findById("missing")).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/v1/orchestration/runs/{runId}", "missing"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return metrics with zero runs when repository is empty")
    void shouldReturnZeroMetricsWhenEmpty() throws Exception {
        when(workflowRunRepository.findAll()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/orchestration/metrics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRuns").value(0));
    }

    @Test
    @DisplayName("Should reject a run with blank requirement text")
    void shouldRejectBlankRequirement() throws Exception {
        StartRunRequest request = new StartRunRequest("", ScenarioType.GREENFIELD, null);

        mockMvc.perform(post("/api/v1/orchestration/runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
