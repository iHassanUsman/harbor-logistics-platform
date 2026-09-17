package com.hassanusman.harbor.shipment;

import com.hassanusman.harbor.event.DomainEvent;
import com.hassanusman.harbor.event.Topics;
import com.hassanusman.harbor.outbox.OutboxEvent;
import com.hassanusman.harbor.outbox.OutboxWriter;
import com.hassanusman.harbor.shipment.cache.InMemoryTrackingCache;
import com.hassanusman.harbor.shipment.client.CustomerClient;
import com.hassanusman.harbor.shipment.domain.BookingService;
import com.hassanusman.harbor.shipment.domain.Shipment;
import com.hassanusman.harbor.shipment.domain.ShipmentRepository;
import com.hassanusman.harbor.shipment.domain.ShipmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingSagaTest {

    @Mock
    ShipmentRepository shipments;
    @Mock
    CustomerClient customers;
    @Mock
    OutboxWriter outbox;

    BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService(shipments, customers, outbox, new InMemoryTrackingCache());
        org.mockito.Mockito.lenient().when(shipments.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void bookingWritesShipmentBookedOutbox() {
        when(customers.get("C-ACME")).thenReturn(new CustomerClient.CustomerView("C-ACME", "Acme", "AGENT", new BigDecimal("10000")));
        Shipment booked = service.book("C-ACME", "DXB", "LHR", new BigDecimal("500"));
        assertEquals(ShipmentStatus.PENDING, booked.getStatus());
        verify(outbox).append(eq(Topics.SHIPMENTS), any(DomainEvent.class));
    }

    @Test
    void creditReservedConfirmsPendingShipment() {
        Shipment pending = new Shipment("SHP-1", "C-ACME", "DXB", "LHR", new BigDecimal("500"));
        when(shipments.findById("SHP-1")).thenReturn(java.util.Optional.of(pending));
        service.onFinanceDecision(DomainEvent.of(Topics.CREDIT_RESERVED, "SHP-1", "C-ACME", new BigDecimal("500"), "c"));
        assertEquals(ShipmentStatus.CONFIRMED, pending.getStatus());
        verify(outbox).append(eq(Topics.SHIPMENTS), any(DomainEvent.class));
    }

    @Test
    void creditRejectedCompensates() {
        Shipment pending = new Shipment("SHP-1", "C-POOR", "DXB", "LHR", new BigDecimal("500"));
        when(shipments.findById("SHP-1")).thenReturn(java.util.Optional.of(pending));
        service.onFinanceDecision(DomainEvent.of(Topics.CREDIT_REJECTED, "SHP-1", "C-POOR", new BigDecimal("500"), "c")
                .withReason("insufficient credit"));
        assertEquals(ShipmentStatus.REJECTED, pending.getStatus());
        assertEquals("insufficient credit", pending.getRejectReason());
    }

    @Test
    void duplicateFinanceEventIsIdempotent() {
        Shipment confirmed = new Shipment("SHP-1", "C-ACME", "DXB", "LHR", new BigDecimal("500"));
        confirmed.confirm();
        when(shipments.findById("SHP-1")).thenReturn(java.util.Optional.of(confirmed));
        service.onFinanceDecision(DomainEvent.of(Topics.CREDIT_RESERVED, "SHP-1", "C-ACME", new BigDecimal("500"), "c"));
        assertEquals(ShipmentStatus.CONFIRMED, confirmed.getStatus());
        org.mockito.Mockito.verifyNoInteractions(outbox);
    }
}
