package com.urlshortener.unit;

import com.urlshortener.model.UrlMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UrlMapping Model Tests")
class UrlMappingModelTest {
    private UrlMapping urlMapping;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        urlMapping = UrlMapping.builder()
            .shortCode("abc123")
            .originalUrl("https://example.com/very/long/path")
            .userId("user123")
            .accessCount(5)
            .createdAt(now)
            .lastAccessedAt(now.minusDays(1))
            .build();
    }

    @Test
    @DisplayName("Should create UrlMapping with builder")
    void shouldCreateUrlMappingWithBuilder() {
        assertNotNull(urlMapping);
        assertEquals("abc123", urlMapping.getShortCode());
        assertEquals("https://example.com/very/long/path", urlMapping.getOriginalUrl());
        assertEquals("user123", urlMapping.getUserId());
        assertEquals(5, urlMapping.getAccessCount());
        assertEquals(now, urlMapping.getCreatedAt());
    }

    @Test
    @DisplayName("Should get and set short code")
    void shouldGetAndSetShortCode() {
        urlMapping.setShortCode("xyz789");
        assertEquals("xyz789", urlMapping.getShortCode());
    }

    @Test
    @DisplayName("Should get and set original URL")
    void shouldGetAndSetOriginalUrl() {
        String newUrl = "https://github.com/user/repo";
        urlMapping.setOriginalUrl(newUrl);
        assertEquals(newUrl, urlMapping.getOriginalUrl());
    }

    @Test
    @DisplayName("Should get and set user ID")
    void shouldGetAndSetUserId() {
        urlMapping.setUserId("user456");
        assertEquals("user456", urlMapping.getUserId());
    }

    @Test
    @DisplayName("Should get and set access count")
    void shouldGetAndSetAccessCount() {
        urlMapping.setAccessCount(10);
        assertEquals(10, urlMapping.getAccessCount());
    }

    @Test
    @DisplayName("Should increment access count")
    void shouldIncrementAccessCount() {
        long initialCount = urlMapping.getAccessCount();
        urlMapping.setAccessCount(initialCount + 1);
        assertEquals(initialCount + 1, urlMapping.getAccessCount());
    }

    @Test
    @DisplayName("Should get and set created at timestamp")
    void shouldGetAndSetCreatedAt() {
        LocalDateTime newTime = LocalDateTime.now().minusDays(5);
        urlMapping.setCreatedAt(newTime);
        assertEquals(newTime, urlMapping.getCreatedAt());
    }

    @Test
    @DisplayName("Should get and set last accessed at timestamp")
    void shouldGetAndSetLastAccessedAt() {
        LocalDateTime lastAccess = LocalDateTime.now();
        urlMapping.setLastAccessedAt(lastAccess);
        assertEquals(lastAccess, urlMapping.getLastAccessedAt());
    }

    @Test
    @DisplayName("Should handle null last accessed at initially")
    void shouldHandleNullLastAccessedAtInitially() {
        UrlMapping newMapping = UrlMapping.builder()
            .shortCode("new123")
            .originalUrl("https://example.com")
            .userId("user789")
            .accessCount(0)
            .createdAt(LocalDateTime.now())
            .build();

        assertNull(newMapping.getLastAccessedAt());
    }

    @Test
    @DisplayName("Should handle zero access count")
    void shouldHandleZeroAccessCount() {
        urlMapping.setAccessCount(0);
        assertEquals(0, urlMapping.getAccessCount());
    }

    @Test
    @DisplayName("Should handle large access count")
    void shouldHandleLargeAccessCount() {
        urlMapping.setAccessCount(1000000);
        assertEquals(1000000, urlMapping.getAccessCount());
    }

    @Test
    @DisplayName("Should preserve URL with query parameters")
    void shouldPreserveUrlWithQueryParameters() {
        String urlWithQuery = "https://example.com/path?key1=value1&key2=value2&key3=value3";
        urlMapping.setOriginalUrl(urlWithQuery);
        assertEquals(urlWithQuery, urlMapping.getOriginalUrl());
    }

    @Test
    @DisplayName("Should preserve URL with fragments")
    void shouldPreserveUrlWithFragments() {
        String urlWithFragment = "https://example.com/path#section1";
        urlMapping.setOriginalUrl(urlWithFragment);
        assertEquals(urlWithFragment, urlMapping.getOriginalUrl());
    }

    @Test
    @DisplayName("Should handle very long URL")
    void shouldHandleVeryLongUrl() {
        String veryLongUrl = "https://example.com/" + "a".repeat(1500);
        urlMapping.setOriginalUrl(veryLongUrl);
        assertEquals(veryLongUrl, urlMapping.getOriginalUrl());
    }

    @Test
    @DisplayName("Should handle URLs with special characters")
    void shouldHandleUrlsWithSpecialCharacters() {
        String specialUrl = "https://example.com/path?query=%20%21%40%23";
        urlMapping.setOriginalUrl(specialUrl);
        assertEquals(specialUrl, urlMapping.getOriginalUrl());
    }

    @Test
    @DisplayName("Should handle multiple userId formats")
    void shouldHandleMultipleUserIdFormats() {
        urlMapping.setUserId("user-123-456");
        assertEquals("user-123-456", urlMapping.getUserId());

        urlMapping.setUserId("user@example.com");
        assertEquals("user@example.com", urlMapping.getUserId());

        urlMapping.setUserId("12345");
        assertEquals("12345", urlMapping.getUserId());
    }

    @Test
    @DisplayName("Should handle created and accessed at in correct order")
    void shouldHandleCreatedAndAccessedAtInCorrectOrder() {
        LocalDateTime created = LocalDateTime.now().minusDays(10);
        LocalDateTime accessed = LocalDateTime.now();

        urlMapping.setCreatedAt(created);
        urlMapping.setLastAccessedAt(accessed);

        assertTrue(urlMapping.getCreatedAt().isBefore(urlMapping.getLastAccessedAt()));
    }
}
