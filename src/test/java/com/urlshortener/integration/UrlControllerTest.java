package com.urlshortener.integration;

import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.ShortenUrlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UrlController Integration Tests")
class UrlControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ShortenUrlRequest shortenRequest;

    @BeforeEach
    void setUp() {
        shortenRequest = ShortenUrlRequest.builder()
            .originalUrl("https://example.com/very/long/path/with/query")
            .userId("testuser")
            .build();
    }

    @Test
    @DisplayName("Should shorten URL successfully")
    void shouldShortenUrlSuccessfully() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/urls/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(shortenRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shortCode").isNotEmpty())
            .andExpect(jsonPath("$.shortUrl").isNotEmpty())
            .andExpect(jsonPath("$.originalUrl").value(shortenRequest.getOriginalUrl()))
            .andReturn();

        ShortenUrlResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), 
            ShortenUrlResponse.class
        );
        assertEquals(6, response.getShortCode().length());
    }

    @Test
    @DisplayName("Should return 400 for invalid URL")
    void shouldReturn400ForInvalidUrl() throws Exception {
        ShortenUrlRequest invalidRequest = ShortenUrlRequest.builder()
            .originalUrl("invalid-url")
            .build();

        mockMvc.perform(post("/api/v1/urls/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return health status")
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/v1/urls/health"))
            .andExpect(status().isOk())
            .andExpect(content().string("Service is running"));
    }
}
