package com.urlshortener.unit;

import com.urlshortener.util.RequestInfoExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RequestInfoExtractor Tests")
class RequestInfoExtractorTest {
    private RequestInfoExtractor extractor;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        extractor = new RequestInfoExtractor();
        request = new MockHttpServletRequest();
    }

    @Test
    @DisplayName("Should extract IP from X-Forwarded-For header")
    void shouldExtractIpFromXForwardedFor() {
        request.addHeader("X-Forwarded-For", "192.168.1.1, 10.0.0.1");
        String ip = extractor.getClientIpAddress(request);
        assertEquals("192.168.1.1", ip);
    }

    @Test
    @DisplayName("Should extract IP from X-Real-IP header when X-Forwarded-For is missing")
    void shouldExtractIpFromXRealIp() {
        request.addHeader("X-Real-IP", "172.16.0.1");
        String ip = extractor.getClientIpAddress(request);
        assertEquals("172.16.0.1", ip);
    }

    @Test
    @DisplayName("Should extract IP from remote address when proxy headers are missing")
    void shouldExtractIpFromRemoteAddress() {
        request.setRemoteAddr("127.0.0.1");
        String ip = extractor.getClientIpAddress(request);
        assertEquals("127.0.0.1", ip);
    }

    @Test
    @DisplayName("Should extract User-Agent from request header")
    void shouldExtractUserAgent() {
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        String userAgent = extractor.getUserAgent(request);
        assertEquals("Mozilla/5.0 (Windows NT 10.0; Win64; x64)", userAgent);
    }

    @Test
    @DisplayName("Should return Unknown when User-Agent is missing")
    void shouldReturnUnknownWhenUserAgentMissing() {
        String userAgent = extractor.getUserAgent(request);
        assertEquals("Unknown", userAgent);
    }

    @Test
    @DisplayName("Should extract Referer from request header")
    void shouldExtractReferer() {
        request.addHeader("Referer", "https://twitter.com/search");
        String referer = extractor.getReferer(request);
        assertEquals("https://twitter.com/search", referer);
    }

    @Test
    @DisplayName("Should return Direct when Referer is missing")
    void shouldReturnDirectWhenRefererMissing() {
        String referer = extractor.getReferer(request);
        assertEquals("Direct", referer);
    }

    @Test
    @DisplayName("Should trim whitespace from X-Forwarded-For")
    void shouldTrimWhitespaceFromXForwardedFor() {
        request.addHeader("X-Forwarded-For", "  192.168.1.1  , 10.0.0.1");
        String ip = extractor.getClientIpAddress(request);
        assertEquals("192.168.1.1", ip);
    }

    @Test
    @DisplayName("Should handle empty X-Forwarded-For header")
    void shouldHandleEmptyXForwardedFor() {
        request.addHeader("X-Forwarded-For", "");
        request.addHeader("X-Real-IP", "172.16.0.1");
        String ip = extractor.getClientIpAddress(request);
        assertEquals("172.16.0.1", ip);
    }

    @Test
    @DisplayName("Should handle empty X-Real-IP header")
    void shouldHandleEmptyXRealIp() {
        request.addHeader("X-Real-IP", "");
        request.setRemoteAddr("127.0.0.1");
        String ip = extractor.getClientIpAddress(request);
        assertEquals("127.0.0.1", ip);
    }
}
