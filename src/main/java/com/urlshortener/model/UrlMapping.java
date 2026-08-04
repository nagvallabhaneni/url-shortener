package com.urlshortener.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "url_mappings")
public class UrlMapping implements Serializable {
    @Id
    private String shortCode;
    private String originalUrl;
    private String userId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private long accessCount;
    private LocalDateTime lastAccessedAt;
}
