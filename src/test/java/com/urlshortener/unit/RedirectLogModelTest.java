package com.urlshortener.unit;

import com.urlshortener.model.RedirectLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RedirectLog Model Tests")
class RedirectLogModelTest {
    private RedirectLog redirectLog;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        redirectLog = RedirectLog.builder()
            .shortCode("abc123")
            .userId("user123")
            .ipAddress("192.168.1.1")
            .userAgent("Mozilla/5.0")
            .referer("https://example.com")
            .redirectedAt(now)
            .responseTimeMs(45L)
            .build();
    }

    @Test
    @DisplayName("Should create RedirectLog with builder")
    void shouldCreateRedirectLogWithBuilder() {
        assertNotNull(redirectLog);
        assertEquals("abc123", redirectLog.getShortCode());
        assertEquals("user123", redirectLog.getUserId());
        assertEquals("192.168.1.1", redirectLog.getIpAddress());
        assertEquals("Mozilla/5.0", redirectLog.getUserAgent());
        assertEquals("https://example.com", redirectLog.getReferer());
        assertEquals(45L, redirectLog.getResponseTimeMs());
    }

    @Test
    @DisplayName("Should get and set short code")
    void shouldGetAndSetShortCode() {
        redirectLog.setShortCode("xyz789");
        assertEquals("xyz789", redirectLog.getShortCode());
    }

    @Test
    @DisplayName("Should get and set user ID")
    void shouldGetAndSetUserId() {
        redirectLog.setUserId("user456");
        assertEquals("user456", redirectLog.getUserId());
    }

    @Test
    @DisplayName("Should get and set IP address")
    void shouldGetAndSetIpAddress() {
        redirectLog.setIpAddress("10.0.0.1");
        assertEquals("10.0.0.1", redirectLog.getIpAddress());
    }

    @Test
    @DisplayName("Should get and set user agent")
    void shouldGetAndSetUserAgent() {
        redirectLog.setUserAgent("Chrome/91.0");
        assertEquals("Chrome/91.0", redirectLog.getUserAgent());
    }

    @Test
    @DisplayName("Should get and set referer")
    void shouldGetAndSetReferer() {
        redirectLog.setReferer("https://google.com");
        assertEquals("https://google.com", redirectLog.getReferer());
    }

    @Test
    @DisplayName("Should get and set response time")
    void shouldGetAndSetResponseTime() {
        redirectLog.setResponseTimeMs(100L);
        assertEquals(100L, redirectLog.getResponseTimeMs());
    }

    @Test
    @DisplayName("Should get and set redirected at timestamp")
    void shouldGetAndSetRedirectedAt() {
        LocalDateTime newTime = LocalDateTime.now().plusHours(1);
        redirectLog.setRedirectedAt(newTime);
        assertEquals(newTime, redirectLog.getRedirectedAt());
    }

    @Test
    @DisplayName("Should handle null values")
    void shouldHandleNullValues() {
        RedirectLog nullLog = RedirectLog.builder()
            .shortCode("test")
            .userAgent(null)
            .referer(null)
            .build();

        assertNotNull(nullLog.getShortCode());
        assertNull(nullLog.getUserAgent());
        assertNull(nullLog.getReferer());
    }

    @Test
    @DisplayName("Should handle very long IP addresses and user agents")
    void shouldHandleVeryLongIpAddressesAndUserAgents() {
        String veryLongUserAgent = "Mozilla/5.0 " + "x".repeat(500);
        redirectLog.setUserAgent(veryLongUserAgent);
        assertEquals(veryLongUserAgent, redirectLog.getUserAgent());
    }

    @Test
    @DisplayName("Should record fast redirect response time")
    void shouldRecordFastRedirectResponseTime() {
        redirectLog.setResponseTimeMs(1L);
        assertEquals(1L, redirectLog.getResponseTimeMs());
    }

    @Test
    @DisplayName("Should record slow redirect response time")
    void shouldRecordSlowRedirectResponseTime() {
        redirectLog.setResponseTimeMs(5000L);
        assertEquals(5000L, redirectLog.getResponseTimeMs());
    }
}
