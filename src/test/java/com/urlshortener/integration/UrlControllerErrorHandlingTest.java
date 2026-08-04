package com.urlshortener.integration;

import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.repository.RedirectLogRepository;
import com.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
@DisplayName("UrlController Error Handling Tests")
class UrlControllerErrorHandlingTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UrlRepository urlRepository;

    @MockBean
    private RedirectLogRepository redirectLogRepository;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        when(urlRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should return 400 when URL is empty")
    void shouldReturn400WhenUrlIsEmpty() throws Exception {
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl("")
            .userId("user123")
            .build();

        mockMvc.perform(post("/api/v1/urls/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when URL is whitespace only")
    void shouldReturn400WhenUrlIsWhitespaceOnly() throws Exception {
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl("   ")
            .userId("user123")
            .build();

        mockMvc.perform(post("/api/v1/urls/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for URL with invalid protocol")
    void shouldReturn400ForUrlWithInvalidProtocol() throws Exception {
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl("ftp://example.com")
            .userId("user123")
            .build();

        mockMvc.perform(post("/api/v1/urls/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for malformed URL")
    void shouldReturn400ForMalformedUrl() throws Exception {
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl("htp://example.com")
            .userId("user123")
            .build();

        mockMvc.perform(post("/api/v1/urls/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when short code is null in request body")
    void shouldReturn400WhenShortCodeIsNullInRequest() throws Exception {
        mockMvc.perform(post("/api/v1/urls/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"originalUrl\": null, \"userId\": \"user123\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 with error response when URL not found in API endpoint")
    void shouldReturn404WithErrorResponseWhenUrlNotFoundInApiEndpoint() throws Exception {
        String shortCode = "notfound";
        when(urlRepository.findById(shortCode)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/urls/{shortCode}", shortCode))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.error").value("URL not found for short code: " + shortCode));
    }

    @Test
    @DisplayName("Should return 404 when redirect URL not found")
    void shouldReturn404WhenRedirectUrlNotFound() throws Exception {
        String shortCode = "notfound";
        when(urlRepository.findById(shortCode)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/{shortCode}", shortCode))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should handle special short codes")
    void shouldHandleSpecialShortCodes() throws Exception {
        UrlMapping urlMapping = UrlMapping.builder()
            .shortCode("abc-123")
            .originalUrl("https://example.com")
            .userId("user123")
            .createdAt(LocalDateTime.now())
            .accessCount(0)
            .build();
        when(urlRepository.findById("abc-123")).thenReturn(Optional.of(urlMapping));

        mockMvc.perform(get("/api/v1/abc-123"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Should handle URL with long path segments")
    void shouldHandleUrlWithLongPathSegments() throws Exception {
        String longPath = "https://example.com/segment1/segment2/segment3/segment4/segment5/segment6/segment7";
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl(longPath)
            .userId("user123")
            .build();

        when(urlRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/urls/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shortCode").isNotEmpty());
    }

    @Test
    @DisplayName("Should handle URL with many query parameters")
    void shouldHandleUrlWithManyQueryParameters() throws Exception {
        String urlWithManyParams = "https://example.com/path?p1=v1&p2=v2&p3=v3&p4=v4&p5=v5&p6=v6&p7=v7&p8=v8";
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl(urlWithManyParams)
            .userId("user123")
            .build();

        when(urlRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/urls/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should handle redirect with complex User-Agent header")
    void shouldHandleRedirectWithComplexUserAgentHeader() throws Exception {
        UrlMapping urlMapping = UrlMapping.builder()
            .shortCode("abc123")
            .originalUrl("https://example.com/path")
            .userId("user123")
            .createdAt(LocalDateTime.now())
            .accessCount(0)
            .build();
        when(urlRepository.findById("abc123")).thenReturn(Optional.of(urlMapping));

        String complexUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
        
        mockMvc.perform(get("/api/v1/abc123")
            .header("User-Agent", complexUserAgent))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("https://example.com/path"));
    }

    @Test
    @DisplayName("Should handle redirect with multiple Referer headers")
    void shouldHandleRedirectWithMultipleHeaders() throws Exception {
        UrlMapping urlMapping = UrlMapping.builder()
            .shortCode("xyz789")
            .originalUrl("https://example.com")
            .userId("user123")
            .createdAt(LocalDateTime.now())
            .accessCount(0)
            .build();
        when(urlRepository.findById("xyz789")).thenReturn(Optional.of(urlMapping));

        mockMvc.perform(get("/api/v1/xyz789")
            .header("Referer", "https://google.com")
            .header("User-Agent", "Safari/537.36")
            .header("Accept-Language", "en-US,en;q=0.9"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Should accept localhost URLs in testing")
    void shouldAcceptLocalhostUrlsInTesting() throws Exception {
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl("http://localhost:3000/path")
            .userId("user123")
            .build();

        when(urlRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/urls/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should accept URLs with port numbers")
    void shouldAcceptUrlsWithPortNumbers() throws Exception {
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl("https://example.com:8443/path")
            .userId("user123")
            .build();

        when(urlRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/urls/shorten")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }
}
