package com.urlshortener.unit;

import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.ShortenUrlResponse;
import com.urlshortener.exception.UrlException;
import com.urlshortener.messaging.RedirectEvent;
import com.urlshortener.messaging.RedirectEventPublisher;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlService Redirect & Analytics Tests")
class UrlServiceRedirectTest {
    @Mock
    private UrlRepository urlRepository;

    @Mock
    private RedirectEventPublisher redirectEventPublisher;

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        urlService = new UrlService(urlRepository, redirectEventPublisher, 6, "http://localhost:8080/s");
    }

    @Test
    @DisplayName("Should handle redirect with analytics logging")
    void shouldHandleRedirectWithAnalytics() {
        // Arrange
        String shortCode = "abc123";
        String originalUrl = "https://example.com/path";
        UrlMapping urlMapping = UrlMapping.builder()
            .shortCode(shortCode)
            .originalUrl(originalUrl)
            .userId("user123")
            .createdAt(LocalDateTime.now())
            .accessCount(5)
            .build();

        when(urlRepository.findById(shortCode)).thenReturn(Optional.of(urlMapping));
        when(urlRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String result = urlService.handleRedirect(shortCode, "192.168.1.1", "Mozilla/5.0", "https://twitter.com");

        // Assert
        assertEquals(originalUrl, result);
        verify(urlRepository, times(1)).findById(shortCode);
        verify(urlRepository, times(1)).save(any(UrlMapping.class));
        verify(redirectEventPublisher, times(1)).publish(any(RedirectEvent.class));

        // Verify access count was incremented
        ArgumentCaptor<UrlMapping> captor = ArgumentCaptor.forClass(UrlMapping.class);
        verify(urlRepository).save(captor.capture());
        assertEquals(6, captor.getValue().getAccessCount());
    }

    @Test
    @DisplayName("Should throw exception when short code not found in redirect")
    void shouldThrowExceptionWhenShortCodeNotFoundInRedirect() {
        // Arrange
        String shortCode = "invalid";
        when(urlRepository.findById(shortCode)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UrlException.class, 
            () -> urlService.handleRedirect(shortCode, "192.168.1.1", "Mozilla", ""));
    }

    @Test
    @DisplayName("Should update last accessed time on redirect")
    void shouldUpdateLastAccessedTimeOnRedirect() {
        // Arrange
        String shortCode = "abc123";
        UrlMapping urlMapping = UrlMapping.builder()
            .shortCode(shortCode)
            .originalUrl("https://example.com")
            .userId("user123")
            .createdAt(LocalDateTime.now().minusDays(1))
            .accessCount(0)
            .lastAccessedAt(null)
            .build();

        when(urlRepository.findById(shortCode)).thenReturn(Optional.of(urlMapping));
        when(urlRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        urlService.handleRedirect(shortCode, "192.168.1.1", "Mozilla", "Direct");

        // Assert
        ArgumentCaptor<UrlMapping> captor = ArgumentCaptor.forClass(UrlMapping.class);
        verify(urlRepository).save(captor.capture());
        assertNotNull(captor.getValue().getLastAccessedAt());
    }

    @Test
    @DisplayName("Should continue even if analytics logging fails")
    void shouldContinueEvenIfAnalyticsLoggingFails() {
        // Arrange
        String shortCode = "abc123";
        UrlMapping urlMapping = UrlMapping.builder()
            .shortCode(shortCode)
            .originalUrl("https://example.com")
            .userId("user123")
            .createdAt(LocalDateTime.now())
            .accessCount(0)
            .build();

        when(urlRepository.findById(shortCode)).thenReturn(Optional.of(urlMapping));
        when(urlRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("Analytics error")).when(redirectEventPublisher).publish(any());

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> 
            urlService.handleRedirect(shortCode, "192.168.1.1", "Mozilla", "Direct"));

        // Verify URL was still saved
        verify(urlRepository, times(1)).save(any(UrlMapping.class));
    }

    @Test
    @DisplayName("Should publish redirect event with unique ID and analytics details")
    void shouldPublishRedirectEventWithUniqueIdAndAnalyticsDetails() {
        UrlMapping urlMapping = UrlMapping.builder()
            .shortCode("abc123")
            .originalUrl("https://example.com")
            .userId("user123")
            .createdAt(LocalDateTime.now())
            .accessCount(0)
            .build();
        when(urlRepository.findById("abc123")).thenReturn(Optional.of(urlMapping));
        when(urlRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        urlService.handleRedirect("abc123", "192.168.1.1", "Mozilla", "Direct");

        ArgumentCaptor<RedirectEvent> eventCaptor = ArgumentCaptor.forClass(RedirectEvent.class);
        verify(redirectEventPublisher).publish(eventCaptor.capture());
        RedirectEvent event = eventCaptor.getValue();
        assertNotNull(event.eventId());
        assertEquals("abc123", event.shortCode());
        assertEquals("192.168.1.1", event.ipAddress());
        assertEquals("Mozilla", event.userAgent());
        assertEquals("Direct", event.referer());
        assertNotNull(event.redirectedAt());
    }

    @Test
    @DisplayName("Should shorten URL with correct short code length")
    void shouldShortenUrlWithCorrectLength() {
        // Arrange
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl("https://github.com/github/copilot-cli")
            .userId("user123")
            .build();

        when(urlRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ShortenUrlResponse response = urlService.shortenUrl(request);

        // Assert
        assertEquals(6, response.getShortCode().length());
        assertEquals(request.getOriginalUrl(), response.getOriginalUrl());
        assertTrue(response.getShortUrl().contains(response.getShortCode()));
    }

    @Test
    @DisplayName("Should increment access count on getOriginalUrl")
    void shouldIncrementAccessCountOnGetOriginalUrl() {
        // Arrange
        String shortCode = "abc123";
        UrlMapping urlMapping = UrlMapping.builder()
            .shortCode(shortCode)
            .originalUrl("https://example.com")
            .userId("user123")
            .createdAt(LocalDateTime.now())
            .accessCount(10)
            .build();

        when(urlRepository.findById(shortCode)).thenReturn(Optional.of(urlMapping));
        when(urlRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        urlService.getOriginalUrl(shortCode);

        // Assert
        ArgumentCaptor<UrlMapping> captor = ArgumentCaptor.forClass(UrlMapping.class);
        verify(urlRepository).save(captor.capture());
        assertEquals(11, captor.getValue().getAccessCount());
    }

    @Test
    @DisplayName("Should throw UrlException for invalid URL in shortenUrl")
    void shouldThrowUrlExceptionForInvalidUrl() {
        // Arrange
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl("not a valid url")
            .userId("user123")
            .build();

        // Act & Assert
        assertThrows(UrlException.class, () -> urlService.shortenUrl(request));
        verify(urlRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should preserve original URL details on redirect")
    void shouldPreserveOriginalUrlDetailsOnRedirect() {
        // Arrange
        String shortCode = "xyz789";
        String originalUrl = "https://example.com/very/long/path?query=value&foo=bar";
        UrlMapping urlMapping = UrlMapping.builder()
            .shortCode(shortCode)
            .originalUrl(originalUrl)
            .userId("user123")
            .createdAt(LocalDateTime.now())
            .accessCount(0)
            .build();

        when(urlRepository.findById(shortCode)).thenReturn(Optional.of(urlMapping));
        when(urlRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String result = urlService.handleRedirect(shortCode, "192.168.1.1", "Mozilla", "Direct");

        // Assert
        assertEquals(originalUrl, result);
    }
}
