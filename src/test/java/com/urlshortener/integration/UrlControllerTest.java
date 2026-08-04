package com.urlshortener.integration;

import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.ShortenUrlResponse;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.repository.RedirectLogRepository;
import com.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

    @MockBean
    private UrlRepository urlRepository;

    @MockBean
    private RedirectLogRepository redirectLogRepository;

    private ShortenUrlRequest shortenRequest;

    @BeforeEach
    void setUp() {
        shortenRequest = ShortenUrlRequest.builder()
            .originalUrl("https://example.com/very/long/path/with/query")
            .userId("testuser")
            .build();

        when(urlRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
    @DisplayName("Should redirect successfully for valid short code")
    void shouldRedirectSuccessfully() throws Exception {
        UrlMapping urlMapping = UrlMapping.builder()
            .shortCode("abc123")
            .originalUrl("https://example.com/long/url")
            .userId("testuser")
            .createdAt(LocalDateTime.now())
            .accessCount(0)
            .build();
        when(urlRepository.findById("abc123")).thenReturn(Optional.of(urlMapping));

        mockMvc.perform(get("/api/v1/abc123")
            .header("User-Agent", "Test Browser")
            .header("Referer", "https://example.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("https://example.com/long/url"));
    }

    @Test
    @DisplayName("Should return 404 for non-existent short code")
    void shouldReturn404ForNonExistentShortCode() throws Exception {
        String shortCode = "invalid";
        when(urlRepository.findById(shortCode)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/{shortCode}", shortCode))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should get original URL as JSON")
    void shouldGetOriginalUrlAsJson() throws Exception {
        String shortCode = "abc123";
        String originalUrl = "https://example.com/long/url";
        
        UrlMapping urlMapping = UrlMapping.builder()
            .shortCode(shortCode)
            .originalUrl(originalUrl)
            .userId("testuser")
            .createdAt(LocalDateTime.now())
            .accessCount(0)
            .build();

        when(urlRepository.findById(shortCode)).thenReturn(Optional.of(urlMapping));
        when(urlRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(get("/api/v1/urls/{shortCode}", shortCode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.originalUrl").value(originalUrl));
    }

    @Test
    @DisplayName("Should return health status")
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/v1/urls/health"))
            .andExpect(status().isOk())
            .andExpect(content().string("Service is running"));
    }
}

