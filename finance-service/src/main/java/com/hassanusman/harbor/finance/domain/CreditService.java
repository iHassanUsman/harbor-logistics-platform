package com.hassanusman.harbor.finance.domain;

import com.hassanusman.harbor.event.DomainEvent;
import com.hassanusman.harbor.event.Topics;
import com.hassanusman.harbor.outbox.OutboxWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CreditService {

    private final CreditAccountRepository accounts;
    private final ProcessedEventRepository processed;
    private final OutboxWriter outbox;

    public CreditService(CreditAccountRepository accounts, ProcessedEventRepository processed, OutboxWriter outbox) {
        this.accounts = accounts;
        this.processed = processed;
        this.outbox = outbox;
    }

    @Transactional
    public DomainEvent onShipmentBooked(DomainEvent booked) {
        if (processed.existsById(booked.eventId().toString())) {
            return null;
        }
        CreditAccount account = accounts.findById(booked.customerId())
                .orElseGet(() -> accounts.save(new CreditAccount(booked.customerId(), new BigDecimal("0"), BigDecimal.ZERO)));
        boolean ok = account.reserve(booked.amount());
        accounts.save(account);
        processed.save(new ProcessedEvent(booked.eventId().toString()));
        DomainEvent decision = ok
                ? DomainEvent.of(Topics.CREDIT_RESERVED, booked.aggregateId(), booked.customerId(), booked.amount(), booked.correlationId())
                : DomainEvent.of(Topics.CREDIT_REJECTED, booked.aggregateId(), booked.customerId(), booked.amount(), booked.correlationId())
                .withReason("insufficient credit, available=" + account.available());
        outbox.append(Topics.FINANCE, decision);
        return decision;
    }
}
