package com.urlshortener.unit;

import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.ShortenUrlResponse;
import com.urlshortener.exception.UrlException;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlService Tests")
class UrlServiceTest {
    @Mock
    private UrlRepository urlRepository;

    @Mock
    private com.urlshortener.repository.RedirectLogRepository redirectLogRepository;

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        urlService = new UrlService(urlRepository, redirectLogRepository, 6, "http://localhost:8080/s");
    }

    @Test
    @DisplayName("Should shorten valid URL successfully")
    void shouldShortenValidUrlSuccessfully() {
        // Arrange
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl("https://example.com/very/long/url")
            .userId("user123")
            .build();

        // Act
        ShortenUrlResponse response = urlService.shortenUrl(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getShortCode());
        assertEquals(6, response.getShortCode().length());
        assertEquals(request.getOriginalUrl(), response.getOriginalUrl());
        assertTrue(response.getShortUrl().contains("http://localhost:8080/s"));
        verify(urlRepository, times(1)).save(any(UrlMapping.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid URL")
    void shouldThrowExceptionForInvalidUrl() {
        // Arrange
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl("not-a-valid-url")
            .build();

        // Act & Assert
        assertThrows(UrlException.class, () -> urlService.shortenUrl(request));
        verify(urlRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve original URL successfully")
    void shouldRetrieveOriginalUrlSuccessfully() {
        // Arrange
        String shortCode = "abc123";
        UrlMapping urlMapping = UrlMapping.builder()
            .shortCode(shortCode)
            .originalUrl("https://example.com/path")
            .accessCount(0)
            .build();

        when(urlRepository.findById(shortCode)).thenReturn(Optional.of(urlMapping));

        // Act
        String result = urlService.getOriginalUrl(shortCode);

        // Assert
        assertEquals("https://example.com/path", result);
        verify(urlRepository, times(1)).findById(shortCode);
        verify(urlRepository, times(1)).save(any(UrlMapping.class));
    }

    @Test
    @DisplayName("Should throw exception when short code not found")
    void shouldThrowExceptionWhenShortCodeNotFound() {
        // Arrange
        String shortCode = "invalid";
        when(urlRepository.findById(shortCode)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UrlException.class, () -> urlService.getOriginalUrl(shortCode));
    }
}
