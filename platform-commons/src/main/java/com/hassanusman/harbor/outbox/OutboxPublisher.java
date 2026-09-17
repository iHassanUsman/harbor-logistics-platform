package com.hassanusman.harbor.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Polling publisher. Portable across clouds; no Debezium slot required.
 * At-least-once: consumers must be idempotent on eventId.
 */
@Component
@ConditionalOnBean(KafkaTemplate.class)
public class OutboxPublisher {

    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${harbor.outbox.poll-ms:500}")
    @Transactional
    public void publishBatch() {
        List<OutboxEvent> batch = repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (OutboxEvent event : batch) {
            kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload());
            event.markPublished(Instant.now());
        }
    }
}
