package com.urlshortener.controller;

import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.ShortenUrlResponse;
import com.urlshortener.exception.UrlException;
import com.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/urls")
@Validated
@Tag(name = "URL Shortener", description = "URL shortening and redirection APIs")
public class UrlController {
    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    /**
     * Shortens a URL
     */
    @PostMapping("/shorten")
    @Operation(summary = "Shorten a URL", description = "Converts a long URL into a short code")
    public ResponseEntity<ShortenUrlResponse> shortenUrl(@Valid @RequestBody ShortenUrlRequest request) {
        log.info("Received request to shorten URL: {}", request.getOriginalUrl());
        try {
            ShortenUrlResponse response = urlService.shortenUrl(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (UrlException e) {
            log.error("Error shortening URL: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Retrieves the original URL for a short code (JSON response, no redirect)
     */
    @GetMapping("/{shortCode}")
    @Operation(summary = "Get original URL", description = "Returns the original URL as JSON (does not redirect)")
    public ResponseEntity<?> getOriginalUrl(@PathVariable String shortCode) {
        log.info("Received request to get URL for short code: {}", shortCode);
        try {
            String originalUrl = urlService.getOriginalUrl(shortCode);
            return ResponseEntity.ok().body(new OriginalUrlResponse(originalUrl));
        } catch (UrlException e) {
            log.error("Short code not found: {}", shortCode);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                   .body(new ErrorResponse("URL not found for short code: " + shortCode));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns service health status")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Service is running");
    }

    /**
     * Response DTOs
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class OriginalUrlResponse {
        private String originalUrl;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String error;
    }
}
