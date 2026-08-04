package com.urlshortener.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlMapping implements Serializable {
    private String shortCode;
    private String originalUrl;
    private String userId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private long accessCount;
    private LocalDateTime lastAccessedAt;
}
