package com.hassanusman.harbor.shipment.domain;

import com.hassanusman.harbor.event.DomainEvent;
import com.hassanusman.harbor.event.Topics;
import com.hassanusman.harbor.outbox.OutboxWriter;
import com.hassanusman.harbor.shipment.cache.TrackingCachePort;
import com.hassanusman.harbor.shipment.client.CustomerClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class BookingService {

    private final ShipmentRepository shipments;
    private final CustomerClient customers;
    private final OutboxWriter outbox;
    private final TrackingCachePort cache;

    public BookingService(ShipmentRepository shipments,
                          CustomerClient customers,
                          OutboxWriter outbox,
                          TrackingCachePort cache) {
        this.shipments = shipments;
        this.customers = customers;
        this.outbox = outbox;
        this.cache = cache;
    }

    @Transactional
    public Shipment book(String customerId, String origin, String destination, BigDecimal amount) {
        try {
            customers.get(customerId);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown customer " + customerId);
        }
        Shipment shipment = new Shipment(
                "SHP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                customerId, origin, destination, amount);
        shipments.save(shipment);
        outbox.append(Topics.SHIPMENTS, DomainEvent.of(
                Topics.SHIPMENT_BOOKED, shipment.getId(), customerId, amount, shipment.getId()));
        cache.put(shipment);
        return shipment;
    }

    @Transactional
    public void onFinanceDecision(DomainEvent event) {
        Shipment shipment = shipments.findById(event.aggregateId()).orElse(null);
        if (shipment == null || shipment.getStatus() != ShipmentStatus.PENDING) {
            return;
        }
        if (Topics.CREDIT_RESERVED.equals(event.eventType())) {
            shipment.confirm();
            shipments.save(shipment);
            outbox.append(Topics.SHIPMENTS, DomainEvent.of(
                    Topics.SHIPMENT_CONFIRMED, shipment.getId(), shipment.getCustomerId(), shipment.getAmount(), event.correlationId()));
        } else if (Topics.CREDIT_REJECTED.equals(event.eventType())) {
            shipment.reject(event.reason() == null ? "credit rejected" : event.reason());
            shipments.save(shipment);
            outbox.append(Topics.SHIPMENTS, DomainEvent.of(
                    Topics.SHIPMENT_REJECTED, shipment.getId(), shipment.getCustomerId(), shipment.getAmount(), event.correlationId())
                    .withReason(shipment.getRejectReason()));
        }
        cache.put(shipment);
    }

    public Shipment require(String id) {
        return shipments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment not found"));
    }
}
