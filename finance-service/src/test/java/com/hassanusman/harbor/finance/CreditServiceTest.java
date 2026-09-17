package com.hassanusman.harbor.finance;

import com.hassanusman.harbor.event.DomainEvent;
import com.hassanusman.harbor.event.Topics;
import com.hassanusman.harbor.finance.domain.CreditAccount;
import com.hassanusman.harbor.finance.domain.CreditAccountRepository;
import com.hassanusman.harbor.finance.domain.CreditService;
import com.hassanusman.harbor.finance.domain.ProcessedEventRepository;
import com.hassanusman.harbor.outbox.OutboxWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditServiceTest {

    @Mock
    CreditAccountRepository accounts;
    @Mock
    ProcessedEventRepository processed;
    @Mock
    OutboxWriter outbox;

    CreditService service;

    @BeforeEach
    void setUp() {
        service = new CreditService(accounts, processed, outbox);
        org.mockito.Mockito.lenient().when(processed.existsById(any())).thenReturn(false);
        org.mockito.Mockito.lenient().when(accounts.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void reservesWhenLimitAllows() {
        when(accounts.findById("C-ACME")).thenReturn(Optional.of(new CreditAccount("C-ACME", new BigDecimal("10000"), BigDecimal.ZERO)));
        DomainEvent booked = DomainEvent.of(Topics.SHIPMENT_BOOKED, "SHP-1", "C-ACME", new BigDecimal("500"), "c");
        DomainEvent decision = service.onShipmentBooked(booked);
        assertEquals(Topics.CREDIT_RESERVED, decision.eventType());
        verify(outbox).append(org.mockito.ArgumentMatchers.eq(Topics.FINANCE), any());
    }

    @Test
    void rejectsWhenInsufficient() {
        when(accounts.findById("C-POOR")).thenReturn(Optional.of(new CreditAccount("C-POOR", new BigDecimal("100"), BigDecimal.ZERO)));
        DomainEvent booked = DomainEvent.of(Topics.SHIPMENT_BOOKED, "SHP-2", "C-POOR", new BigDecimal("500"), "c");
        DomainEvent decision = service.onShipmentBooked(booked);
        assertEquals(Topics.CREDIT_REJECTED, decision.eventType());
        assertEquals("insufficient credit, available=100", decision.reason());
    }

    @Test
    void ignoresDuplicateEventIds() {
        DomainEvent booked = DomainEvent.of(Topics.SHIPMENT_BOOKED, "SHP-1", "C-ACME", new BigDecimal("500"), "c");
        when(processed.existsById(booked.eventId().toString())).thenReturn(true);
        assertNull(service.onShipmentBooked(booked));
        verify(outbox, never()).append(any(), any());
    }
}
