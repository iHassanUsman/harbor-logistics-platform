package com.hassanusman.harbor.finance.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hassanusman.harbor.event.DomainEvent;
import com.hassanusman.harbor.event.Topics;
import com.hassanusman.harbor.finance.domain.CreditService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ShipmentBookedListener {

    private final CreditService creditService;
    private final ObjectMapper mapper;

    public ShipmentBookedListener(CreditService creditService, ObjectMapper mapper) {
        this.creditService = creditService;
        this.mapper = mapper;
    }

    @KafkaListener(topics = Topics.SHIPMENTS, groupId = "finance-service")
    public void onShipment(String payload) throws JsonProcessingException {
        DomainEvent event = mapper.readValue(payload, DomainEvent.class);
        if (Topics.SHIPMENT_BOOKED.equals(event.eventType())) {
            creditService.onShipmentBooked(event);
        }
    }
}
