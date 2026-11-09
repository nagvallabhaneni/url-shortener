package com.urlshortener.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaRedirectEventProducer implements RedirectEventPublisher {
    private final KafkaTemplate<String, RedirectEvent> kafkaTemplate;
    private final String topic;

    public KafkaRedirectEventProducer(
        KafkaTemplate<String, RedirectEvent> kafkaTemplate,
        @Value("${kafka.topics.redirect-events}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(RedirectEvent event) {
        kafkaTemplate.send(topic, event.shortCode(), event)
            .whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to send redirect event {} to Kafka", event.eventId(), exception);
                }
            });
    }
}
