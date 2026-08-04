package com.urlshortener.integration;

import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.ShortenUrlResponse;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.repository.RedirectLogRepository;
import com.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UrlController Additional Integration Tests")
class UrlControllerAdditionalTest {
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

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "   ", "ftp://example.com", "htp://example.com"})
    @DisplayName("Should reject various invalid URLs")
    void shouldRejectVariousInvalidUrls(String invalidUrl) throws Exception {
        ShortenUrlRequest invalidRequest = ShortenUrlRequest.builder()
            .originalUrl(invalidUrl)
            .build();

        mockMvc.perform(post("/api/v1/urls/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle redirect with various User-Agent headers")
    void shouldHandleRedirectWithVariousUserAgents() throws Exception {
        UrlMapping urlMapping = UrlMapping.builder()
            .shortCode("abc123")
            .originalUrl("https://example.com/long/url")
            .userId("testuser")
            .createdAt(LocalDateTime.now())
            .accessCount(0)
            .build();
        when(urlRepository.findById("abc123")).thenReturn(Optional.of(urlMapping));

        mockMvc.perform(get("/api/v1/abc123")
            .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("https://example.com/long/url"));
    }

    @Test
    @DisplayName("Should handle redirect without optional headers")
    void shouldHandleRedirectWithoutOptionalHeaders() throws Exception {
        UrlMapping urlMapping = UrlMapping.builder()
            .shortCode("xyz789")
            .originalUrl("https://github.com")
            .userId("testuser")
            .createdAt(LocalDateTime.now())
            .accessCount(0)
            .build();
        when(urlRepository.findById("xyz789")).thenReturn(Optional.of(urlMapping));

        mockMvc.perform(get("/api/v1/xyz789"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("https://github.com"));
    }

    @Test
    @DisplayName("Should return 404 for API endpoint with non-existent short code")
    void shouldReturn404ForApiEndpointWithNonExistentShortCode() throws Exception {
        String shortCode = "nonexistent";
        when(urlRepository.findById(shortCode)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/urls/{shortCode}", shortCode))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Should accept valid HTTPS URLs")
    void shouldAcceptValidHttpsUrls() throws Exception {
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl("https://example.com/path?query=value&foo=bar")
            .userId("user123")
            .build();

        mockMvc.perform(post("/api/v1/urls/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shortCode").isNotEmpty())
            .andExpect(jsonPath("$.shortUrl").isNotEmpty());
    }

    @Test
    @DisplayName("Should accept valid HTTP URLs")
    void shouldAcceptValidHttpUrls() throws Exception {
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl("http://example.com")
            .userId("user123")
            .build();

        mockMvc.perform(post("/api/v1/urls/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shortCode").isNotEmpty());
    }

    @Test
    @DisplayName("Should include userId in shorten request")
    void shouldIncludeUserIdInShortenRequest() throws Exception {
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl("https://example.com")
            .userId("specific-user-123")
            .build();

        mockMvc.perform(post("/api/v1/urls/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.originalUrl").value(request.getOriginalUrl()));
    }

    @Test
    @DisplayName("Should return valid short URL format")
    void shouldReturnValidShortUrlFormat() throws Exception {
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl("https://example.com/very/long/url")
            .userId("user123")
            .build();

        mockMvc.perform(post("/api/v1/urls/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shortUrl").value(org.hamcrest.Matchers.startsWith("http://localhost:8080")))
            .andExpect(jsonPath("$.createdTimestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Should handle redirect with X-Forwarded-For header")
    void shouldHandleRedirectWithXForwardedForHeader() throws Exception {
        UrlMapping urlMapping = UrlMapping.builder()
            .shortCode("abc123")
            .originalUrl("https://example.com/path")
            .userId("testuser")
            .createdAt(LocalDateTime.now())
            .accessCount(0)
            .build();
        when(urlRepository.findById("abc123")).thenReturn(Optional.of(urlMapping));

        mockMvc.perform(get("/api/v1/abc123")
            .header("X-Forwarded-For", "192.168.1.100, 10.0.0.1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("https://example.com/path"));
    }

    @Test
    @DisplayName("Should handle multiple redirects for same short code")
    void shouldHandleMultipleRedirectsForSameShortCode() throws Exception {
        UrlMapping urlMapping = UrlMapping.builder()
            .shortCode("multi123")
            .originalUrl("https://example.com")
            .userId("testuser")
            .createdAt(LocalDateTime.now())
            .accessCount(0)
            .build();
        when(urlRepository.findById("multi123")).thenReturn(Optional.of(urlMapping));

        // First redirect
        mockMvc.perform(get("/api/v1/multi123"))
            .andExpect(status().is3xxRedirection());

        // Second redirect
        mockMvc.perform(get("/api/v1/multi123"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Should handle special characters in URL")
    void shouldHandleSpecialCharactersInUrl() throws Exception {
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl("https://example.com/path?query=value&special=%20%21%40")
            .userId("user123")
            .build();

        mockMvc.perform(post("/api/v1/urls/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shortCode").isNotEmpty());
    }
}
