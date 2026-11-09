package com.urlshortener.unit;

import com.urlshortener.messaging.KafkaRedirectEventProducer;
import com.urlshortener.messaging.RedirectEvent;
import com.urlshortener.messaging.RedirectEventConsumer;
import com.urlshortener.model.RedirectLog;
import com.urlshortener.repository.RedirectLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedirectEventMessagingTest {
    @Test
    void producerUsesShortCodeAsPartitionKeyToPreservePerUrlOrdering() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, RedirectEvent> kafkaTemplate = mock(KafkaTemplate.class);
        RedirectEvent event = new RedirectEvent(
            "event-1", "abc123", "user-1", "127.0.0.1", "agent", "referer",
            LocalDateTime.now(), 12L
        );
        when(kafkaTemplate.send("redirects", "abc123", event))
            .thenReturn(new CompletableFuture<>());

        new KafkaRedirectEventProducer(kafkaTemplate, "redirects").publish(event);

        verify(kafkaTemplate).send("redirects", "abc123", event);
    }

    @Test
    void consumerUsesEventIdAsMongoIdForIdempotentRedelivery() {
        RedirectLogRepository repository = mock(RedirectLogRepository.class);
        RedirectEvent event = new RedirectEvent(
            "event-1", "abc123", "user-1", "127.0.0.1", "agent", "referer",
            LocalDateTime.now(), 12L
        );

        new RedirectEventConsumer(repository).consume(event);

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(log ->
            "event-1".equals(log.getId()) && "abc123".equals(log.getShortCode())
        ));
    }

    @Test
    void consumerRejectsEventsWithoutAnId() {
        RedirectLogRepository repository = mock(RedirectLogRepository.class);
        RedirectEvent event = new RedirectEvent(
            " ", "abc123", null, null, null, null, LocalDateTime.now(), 0L
        );

        assertThrows(IllegalArgumentException.class, () -> new RedirectEventConsumer(repository).consume(event));
        verify(repository, org.mockito.Mockito.never()).save(any(RedirectLog.class));
    }
}
