package com.urlshortener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("UrlShortenerApplication Tests")
class UrlShortenerApplicationTest {

    @Test
    @DisplayName("Should load application context")
    void contextLoads() {
        // Verifies the full Spring context starts successfully
    }
}
