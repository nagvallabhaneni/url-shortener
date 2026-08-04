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
@Document(collection = "redirect_logs")
public class RedirectLog implements Serializable {
    @Id
    private String id;
    private String shortCode;
    private String userId;
    private String userAgent;
    private String ipAddress;
    private String referer;
    private LocalDateTime redirectedAt;
    private long responseTimeMs;
}
