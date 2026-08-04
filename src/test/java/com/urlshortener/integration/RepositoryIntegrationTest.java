package com.urlshortener.integration;

import com.urlshortener.model.RedirectLog;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.repository.RedirectLogRepository;
import com.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Repository Integration Tests")
class RepositoryIntegrationTest {
    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private RedirectLogRepository redirectLogRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private UrlMapping testUrlMapping;
    private RedirectLog testRedirectLog;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection("url_mappings");
        mongoTemplate.dropCollection("redirect_logs");

        testUrlMapping = UrlMapping.builder()
            .shortCode("test123")
            .originalUrl("https://example.com/long/path")
            .userId("user123")
            .accessCount(0)
            .createdAt(LocalDateTime.now())
            .build();

        testRedirectLog = RedirectLog.builder()
            .shortCode("test123")
            .userId("user123")
            .ipAddress("192.168.1.1")
            .userAgent("Mozilla/5.0")
            .referer("https://example.com")
            .redirectedAt(LocalDateTime.now())
            .responseTimeMs(50L)
            .build();
    }

    @Test
    @DisplayName("Should save and retrieve URL mapping by short code")
    void shouldSaveAndRetrieveUrlMappingByShortCode() {
        // Arrange & Act
        UrlMapping saved = urlRepository.save(testUrlMapping);
        Optional<UrlMapping> retrieved = urlRepository.findById("test123");

        // Assert
        assertTrue(retrieved.isPresent());
        assertEquals(saved.getShortCode(), retrieved.get().getShortCode());
        assertEquals(saved.getOriginalUrl(), retrieved.get().getOriginalUrl());
        assertEquals(saved.getUserId(), retrieved.get().getUserId());
    }

    @Test
    @DisplayName("Should return empty optional when URL mapping not found")
    void shouldReturnEmptyOptionalWhenUrlMappingNotFound() {
        // Act
        Optional<UrlMapping> retrieved = urlRepository.findById("nonexistent");

        // Assert
        assertFalse(retrieved.isPresent());
    }

    @Test
    @DisplayName("Should find URL mappings by user ID")
    void shouldFindUrlMappingsByUserId() {
        // Arrange
        UrlMapping mapping1 = UrlMapping.builder()
            .shortCode("code1")
            .originalUrl("https://example.com/1")
            .userId("user123")
            .accessCount(0)
            .createdAt(LocalDateTime.now())
            .build();

        UrlMapping mapping2 = UrlMapping.builder()
            .shortCode("code2")
            .originalUrl("https://example.com/2")
            .userId("user123")
            .accessCount(0)
            .createdAt(LocalDateTime.now())
            .build();

        urlRepository.save(mapping1);
        urlRepository.save(mapping2);

        // Act
        List<UrlMapping> mappings = urlRepository.findByUserId("user123");

        // Assert
        assertEquals(2, mappings.size());
    }

    @Test
    @DisplayName("Should return empty list when no URLs found for user")
    void shouldReturnEmptyListWhenNoUrlsFoundForUser() {
        // Act
        List<UrlMapping> mappings = urlRepository.findByUserId("nonexistentuser");

        // Assert
        assertTrue(mappings.isEmpty());
    }

    @Test
    @DisplayName("Should save and retrieve redirect log")
    void shouldSaveAndRetrieveRedirectLog() {
        // Act
        RedirectLog saved = redirectLogRepository.save(testRedirectLog);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(testRedirectLog.getShortCode(), saved.getShortCode());
        assertEquals(testRedirectLog.getUserId(), saved.getUserId());
        assertEquals(testRedirectLog.getIpAddress(), saved.getIpAddress());
    }

    @Test
    @DisplayName("Should find redirect logs by short code")
    void shouldFindRedirectLogsByShortCode() {
        // Arrange
        RedirectLog log1 = RedirectLog.builder()
            .shortCode("abc123")
            .userId("user1")
            .ipAddress("192.168.1.1")
            .userAgent("Chrome")
            .referer("google.com")
            .redirectedAt(LocalDateTime.now())
            .responseTimeMs(50L)
            .build();

        RedirectLog log2 = RedirectLog.builder()
            .shortCode("abc123")
            .userId("user2")
            .ipAddress("192.168.1.2")
            .userAgent("Firefox")
            .referer("bing.com")
            .redirectedAt(LocalDateTime.now())
            .responseTimeMs(60L)
            .build();

        redirectLogRepository.save(log1);
        redirectLogRepository.save(log2);

        // Act
        List<RedirectLog> logs = redirectLogRepository.findByShortCode("abc123");

        // Assert
        assertEquals(2, logs.size());
    }

    @Test
    @DisplayName("Should find redirect logs by user ID")
    void shouldFindRedirectLogsByUserId() {
        // Arrange
        RedirectLog log1 = RedirectLog.builder()
            .shortCode("abc123")
            .userId("user123")
            .ipAddress("192.168.1.1")
            .userAgent("Chrome")
            .referer("google.com")
            .redirectedAt(LocalDateTime.now())
            .responseTimeMs(50L)
            .build();

        RedirectLog log2 = RedirectLog.builder()
            .shortCode("xyz789")
            .userId("user123")
            .ipAddress("192.168.1.2")
            .userAgent("Firefox")
            .referer("bing.com")
            .redirectedAt(LocalDateTime.now())
            .responseTimeMs(60L)
            .build();

        redirectLogRepository.save(log1);
        redirectLogRepository.save(log2);

        // Act
        List<RedirectLog> logs = redirectLogRepository.findByUserId("user123");

        // Assert
        assertEquals(2, logs.size());
    }

    @Test
    @DisplayName("Should return empty list when no redirect logs found for user")
    void shouldReturnEmptyListWhenNoRedirectLogsFoundForUser() {
        // Act
        List<RedirectLog> logs = redirectLogRepository.findByUserId("nonexistentuser");

        // Assert
        assertTrue(logs.isEmpty());
    }

    @Test
    @DisplayName("Should update URL mapping access count")
    void shouldUpdateUrlMappingAccessCount() {
        // Arrange
        UrlMapping saved = urlRepository.save(testUrlMapping);
        saved.setAccessCount(10);

        // Act
        UrlMapping updated = urlRepository.save(saved);

        // Assert
        Optional<UrlMapping> retrieved = urlRepository.findById("test123");
        assertTrue(retrieved.isPresent());
        assertEquals(10, retrieved.get().getAccessCount());
    }

    @Test
    @DisplayName("Should handle concurrent URL mappings from different users")
    void shouldHandleConcurrentUrlMappingsFromDifferentUsers() {
        // Arrange
        UrlMapping mapping1 = UrlMapping.builder()
            .shortCode("user1code")
            .originalUrl("https://example.com/1")
            .userId("user1")
            .accessCount(0)
            .createdAt(LocalDateTime.now())
            .build();

        UrlMapping mapping2 = UrlMapping.builder()
            .shortCode("user2code")
            .originalUrl("https://example.com/2")
            .userId("user2")
            .accessCount(0)
            .createdAt(LocalDateTime.now())
            .build();

        // Act
        urlRepository.save(mapping1);
        urlRepository.save(mapping2);

        // Assert
        List<UrlMapping> user1Mappings = urlRepository.findByUserId("user1");
        List<UrlMapping> user2Mappings = urlRepository.findByUserId("user2");

        assertEquals(1, user1Mappings.size());
        assertEquals(1, user2Mappings.size());
    }

    @Test
    @DisplayName("Should preserve URL with complex query parameters in repository")
    void shouldPreserveUrlWithComplexQueryParametersInRepository() {
        // Arrange
        String complexUrl = "https://example.com/path?key1=value1&key2=value with spaces&key3=%20%21%40";
        UrlMapping mapping = UrlMapping.builder()
            .shortCode("complex")
            .originalUrl(complexUrl)
            .userId("user123")
            .accessCount(0)
            .createdAt(LocalDateTime.now())
            .build();

        // Act
        urlRepository.save(mapping);
        Optional<UrlMapping> retrieved = urlRepository.findById("complex");

        // Assert
        assertTrue(retrieved.isPresent());
        assertEquals(complexUrl, retrieved.get().getOriginalUrl());
    }

    @Test
    @DisplayName("Should handle redirect logs with various response times")
    void shouldHandleRedirectLogsWithVariousResponseTimes() {
        // Arrange
        RedirectLog fastLog = RedirectLog.builder()
            .shortCode("fast")
            .userId("user123")
            .ipAddress("192.168.1.1")
            .userAgent("Chrome")
            .referer("google.com")
            .redirectedAt(LocalDateTime.now())
            .responseTimeMs(5L)
            .build();

        RedirectLog slowLog = RedirectLog.builder()
            .shortCode("slow")
            .userId("user123")
            .ipAddress("192.168.1.2")
            .userAgent("Firefox")
            .referer("bing.com")
            .redirectedAt(LocalDateTime.now())
            .responseTimeMs(1000L)
            .build();

        // Act
        redirectLogRepository.save(fastLog);
        redirectLogRepository.save(slowLog);

        // Assert
        List<RedirectLog> logs = redirectLogRepository.findByUserId("user123");
        assertEquals(2, logs.size());
        assertTrue(logs.stream().anyMatch(l -> l.getResponseTimeMs() == 5L));
        assertTrue(logs.stream().anyMatch(l -> l.getResponseTimeMs() == 1000L));
    }
}
