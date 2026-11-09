package com.urlshortener.messaging;

import com.urlshortener.model.RedirectLog;
import com.urlshortener.repository.RedirectLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class RedirectEventConsumer {
    private final RedirectLogRepository redirectLogRepository;

    public RedirectEventConsumer(RedirectLogRepository redirectLogRepository) {
        this.redirectLogRepository = redirectLogRepository;
    }

    @KafkaListener(topics = "${kafka.topics.redirect-events}")
    public void consume(RedirectEvent event) {
        if (!StringUtils.hasText(event.eventId())) {
            throw new IllegalArgumentException("Redirect event must have an event ID");
        }

        RedirectLog redirectLog = RedirectLog.builder()
            .id(event.eventId())
            .shortCode(event.shortCode())
            .userId(event.userId())
            .ipAddress(event.ipAddress())
            .userAgent(event.userAgent())
            .referer(event.referer())
            .redirectedAt(event.redirectedAt())
            .responseTimeMs(event.responseTimeMs())
            .build();

        // The event ID is the Mongo document ID, so Kafka redelivery upserts the same log.
        redirectLogRepository.save(redirectLog);
        log.debug("Processed redirect event {} for short code {}", event.eventId(), event.shortCode());
    }
}
