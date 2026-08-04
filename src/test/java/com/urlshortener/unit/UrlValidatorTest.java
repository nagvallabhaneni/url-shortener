package com.urlshortener.unit;

import com.urlshortener.validator.UrlValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UrlValidator Tests")
class UrlValidatorTest {
    private UrlValidator urlValidator;

    @BeforeEach
    void setUp() {
        urlValidator = new UrlValidator();
    }

    @Test
    @DisplayName("Should accept valid HTTP URL")
    void shouldAcceptValidHttpUrl() {
        assertTrue(urlValidator.isValid("http://example.com"));
    }

    @Test
    @DisplayName("Should accept valid HTTPS URL")
    void shouldAcceptValidHttpsUrl() {
        assertTrue(urlValidator.isValid("https://example.com/path?query=value"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "not-a-url", "ftp://example.com"})
    @DisplayName("Should reject invalid URLs")
    void shouldRejectInvalidUrls(String url) {
        assertFalse(urlValidator.isValid(url));
    }

    @Test
    @DisplayName("Should reject null URL")
    void shouldRejectNullUrl() {
        assertFalse(urlValidator.isValid(null));
    }

    @Test
    @DisplayName("Should reject URLs exceeding max length")
    void shouldRejectUrlExceedingMaxLength() {
        String longUrl = "https://example.com/" + "a".repeat(2000);
        assertFalse(urlValidator.isValid(longUrl));
    }
}
