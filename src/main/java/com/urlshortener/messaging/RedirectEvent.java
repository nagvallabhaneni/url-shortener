package com.urlshortener.messaging;

import java.time.LocalDateTime;

public record RedirectEvent(
    String eventId,
    String shortCode,
    String userId,
    String ipAddress,
    String userAgent,
    String referer,
    LocalDateTime redirectedAt,
    long responseTimeMs
) {
}
