package com.urlshortener.orchestration.dto;

import com.urlshortener.orchestration.model.ScenarioType;
import com.urlshortener.orchestration.model.StageName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** Request to start a new agentic SDLC orchestration run for one requirement. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartRunRequest {
    @NotBlank
    private String requirementText;

    @NotNull
    private ScenarioType scenarioType;

    /**
     * Optional: {"UNIT_TESTING": 2} forces that stage to fail its first 2
     * attempts before succeeding, to demonstrate bounded retry/fallback/rollback.
     */
    private Map<StageName, Integer> simulatedFailures;
}
