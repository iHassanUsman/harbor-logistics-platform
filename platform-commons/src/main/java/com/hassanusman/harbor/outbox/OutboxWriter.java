package com.hassanusman.harbor.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hassanusman.harbor.event.DomainEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxWriter {

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxWriter(OutboxRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OutboxEvent append(String topic, DomainEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            OutboxEvent row = new OutboxEvent(
                    event.eventId() == null ? UUID.randomUUID() : event.eventId(),
                    topic,
                    event.eventType(),
                    event.aggregateId(),
                    json,
                    Instant.now());
            return repository.save(row);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize domain event", e);
        }
    }
}
