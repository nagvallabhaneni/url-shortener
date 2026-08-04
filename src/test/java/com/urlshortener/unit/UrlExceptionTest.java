package com.urlshortener.unit;

import com.urlshortener.exception.UrlException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UrlException Tests")
class UrlExceptionTest {

    @Test
    @DisplayName("Should create exception with message")
    void shouldCreateWithMessage() {
        UrlException ex = new UrlException("test error");
        assertEquals("test error", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateWithMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        UrlException ex = new UrlException("test error", cause);
        assertEquals("test error", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    @DisplayName("Should be instance of RuntimeException")
    void shouldBeRuntimeException() {
        UrlException ex = new UrlException("msg");
        assertInstanceOf(RuntimeException.class, ex);
    }
}
