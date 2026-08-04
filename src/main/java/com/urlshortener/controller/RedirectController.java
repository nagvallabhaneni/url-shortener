package com.urlshortener.controller;

import com.urlshortener.exception.UrlException;
import com.urlshortener.service.UrlService;
import com.urlshortener.util.RequestInfoExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/")
@Tag(name = "Redirect", description = "Short URL redirection")
public class RedirectController {

    private final UrlService urlService;
    private final RequestInfoExtractor requestInfoExtractor;

    public RedirectController(UrlService urlService, RequestInfoExtractor requestInfoExtractor) {
        this.urlService = urlService;
        this.requestInfoExtractor = requestInfoExtractor;
    }

    @GetMapping("/{shortCode}")
    @Operation(summary = "Redirect short URL",
               description = "Redirects to the original URL for the given short code and logs analytics")
    public void redirectToOriginalUrl(@PathVariable String shortCode,
                                      HttpServletRequest request,
                                      HttpServletResponse response) throws IOException {
        log.info("Redirect request for short code: {}", shortCode);
        try {
            String ipAddress = requestInfoExtractor.getClientIpAddress(request);
            String userAgent = requestInfoExtractor.getUserAgent(request);
            String referer = requestInfoExtractor.getReferer(request);

            String originalUrl = urlService.handleRedirect(shortCode, ipAddress, userAgent, referer);

            log.info("Redirecting short code: {} to: {}", shortCode, originalUrl);
            response.sendRedirect(originalUrl);
        } catch (UrlException e) {
            log.warn("Redirect failed for short code: {}, Error: {}", shortCode, e.getMessage());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("Short code not found: " + shortCode);
        }
    }
}
