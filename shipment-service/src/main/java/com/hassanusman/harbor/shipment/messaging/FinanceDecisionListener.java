package com.hassanusman.harbor.shipment.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hassanusman.harbor.event.DomainEvent;
import com.hassanusman.harbor.event.Topics;
import com.hassanusman.harbor.shipment.domain.BookingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FinanceDecisionListener {

    private final BookingService bookings;
    private final ObjectMapper mapper;

    public FinanceDecisionListener(BookingService bookings, ObjectMapper mapper) {
        this.bookings = bookings;
        this.mapper = mapper;
    }

    @KafkaListener(topics = Topics.FINANCE, groupId = "shipment-service")
    public void onFinance(String payload) throws JsonProcessingException {
        DomainEvent event = mapper.readValue(payload, DomainEvent.class);
        bookings.onFinanceDecision(event);
    }
}
