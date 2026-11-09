package com.urlshortener.service;

import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.ShortenUrlResponse;
import com.urlshortener.exception.UrlException;
import com.urlshortener.messaging.RedirectEvent;
import com.urlshortener.messaging.RedirectEventPublisher;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.HashGenerator;
import com.urlshortener.validator.UrlValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class UrlService {
    private final UrlRepository urlRepository;
    private final RedirectEventPublisher redirectEventPublisher;
    private final UrlValidator urlValidator;
    private final HashGenerator hashGenerator;
    private final String baseUrl;

    public UrlService(UrlRepository urlRepository,
                     RedirectEventPublisher redirectEventPublisher,
                     @Value("${url.shortener.short-code-length}") int codeLength,
                     @Value("${url.shortener.base-url}") String baseUrl) {
        this.urlRepository = urlRepository;
        this.redirectEventPublisher = redirectEventPublisher;
        this.urlValidator = new UrlValidator();
        this.hashGenerator = new HashGenerator(codeLength);
        this.baseUrl = baseUrl;
    }

    /**
     * Shortens a URL and stores it in MongoDB
     */
    public ShortenUrlResponse shortenUrl(ShortenUrlRequest request) {
        log.debug("Shortening URL: {}", request.getOriginalUrl());

        if (!urlValidator.isValid(request.getOriginalUrl())) {
            throw new UrlException("Invalid URL format: " + request.getOriginalUrl());
        }

        String shortCode = hashGenerator.generate();
        UrlMapping mapping = UrlMapping.builder()
            .shortCode(shortCode)
            .originalUrl(request.getOriginalUrl())
            .userId(request.getUserId())
            .createdAt(LocalDateTime.now())
            .accessCount(0)
            .build();

        urlRepository.save(mapping);
        log.info("URL shortened successfully. ShortCode: {}, OriginalUrl: {}", 
            shortCode, request.getOriginalUrl());

        return ShortenUrlResponse.builder()
            .shortCode(shortCode)
            .shortUrl(baseUrl + "/" + shortCode)
            .originalUrl(request.getOriginalUrl())
            .createdTimestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * Retrieves the original URL for a given short code (used for internal operations)
     */
    public String getOriginalUrl(String shortCode) {
        log.debug("Retrieving URL for short code: {}", shortCode);

        Optional<UrlMapping> mapping = urlRepository.findById(shortCode);
        if (mapping.isEmpty()) {
            throw new UrlException("Short code not found: " + shortCode);
        }

        UrlMapping urlMapping = mapping.get();
        urlMapping.setAccessCount(urlMapping.getAccessCount() + 1);
        urlMapping.setLastAccessedAt(LocalDateTime.now());
        urlRepository.save(urlMapping);

        log.info("URL retrieved successfully. ShortCode: {}, AccessCount: {}", 
            shortCode, urlMapping.getAccessCount());

        return urlMapping.getOriginalUrl();
    }

    /**
     * Handles redirect with analytics logging
     */
    public String handleRedirect(String shortCode, String ipAddress, String userAgent, String referer) {
        log.debug("Handling redirect for short code: {}", shortCode);
        
        long startTime = System.currentTimeMillis();
        
        Optional<UrlMapping> mapping = urlRepository.findById(shortCode);
        if (mapping.isEmpty()) {
            log.warn("Redirect attempt for non-existent short code: {}", shortCode);
            throw new UrlException("Short code not found: " + shortCode);
        }

        UrlMapping urlMapping = mapping.get();
        
        // Update access count and last accessed time
        urlMapping.setAccessCount(urlMapping.getAccessCount() + 1);
        urlMapping.setLastAccessedAt(LocalDateTime.now());
        urlRepository.save(urlMapping);

        // Log the redirect for analytics
        long responseTimeMs = System.currentTimeMillis() - startTime;
        logRedirect(shortCode, urlMapping.getUserId(), ipAddress, userAgent, referer, responseTimeMs);

        log.info("Redirect processed. ShortCode: {}, AccessCount: {}, ResponseTime: {}ms", 
            shortCode, urlMapping.getAccessCount(), responseTimeMs);

        return urlMapping.getOriginalUrl();
    }

    /**
     * Logs redirect request for analytics
     */
    private void logRedirect(String shortCode, String userId, String ipAddress, String userAgent, 
                           String referer, long responseTimeMs) {
        try {
            RedirectEvent event = new RedirectEvent(
                UUID.randomUUID().toString(),
                shortCode,
                userId,
                ipAddress,
                userAgent,
                referer,
                LocalDateTime.now(),
                responseTimeMs
            );

            redirectEventPublisher.publish(event);
            log.info("Redirect event published for short code: {}", shortCode);
        } catch (RuntimeException e) {
            log.error("Failed to publish redirect event for short code: {}", shortCode, e);
        }
    }
}
