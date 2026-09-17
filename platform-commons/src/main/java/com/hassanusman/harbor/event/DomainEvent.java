package com.hassanusman.harbor.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DomainEvent(
        UUID eventId,
        String eventType,
        String aggregateId,
        String customerId,
        BigDecimal amount,
        String currency,
        String correlationId,
        Instant occurredAt,
        String reason
) {
    public static DomainEvent of(String type, String aggregateId, String customerId, BigDecimal amount, String correlationId) {
        return new DomainEvent(UUID.randomUUID(), type, aggregateId, customerId, amount, "USD", correlationId, Instant.now(), null);
    }

    public DomainEvent withReason(String rejectedReason) {
        return new DomainEvent(eventId, eventType, aggregateId, customerId, amount, currency, correlationId, occurredAt, rejectedReason);
    }
}
