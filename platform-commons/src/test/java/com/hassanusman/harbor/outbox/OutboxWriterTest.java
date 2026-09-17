package com.hassanusman.harbor.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hassanusman.harbor.event.DomainEvent;
import com.hassanusman.harbor.event.Topics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxWriterTest {

    @Mock
    OutboxRepository repository;

    @Test
    void persistsSerializedEventUnpublished() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        OutboxWriter writer = new OutboxWriter(repository, mapper);
        DomainEvent event = DomainEvent.of(Topics.SHIPMENT_BOOKED, "S1", "C1", new BigDecimal("250.00"), "corr");
        writer.append(Topics.SHIPMENTS, event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        OutboxEvent row = captor.getValue();
        assertEquals(Topics.SHIPMENTS, row.getTopic());
        assertEquals("S1", row.getAggregateId());
        assertNull(row.getPublishedAt());
        DomainEvent roundTrip = mapper.readValue(row.getPayload(), DomainEvent.class);
        assertEquals("C1", roundTrip.customerId());
        assertEquals(0, event.amount().compareTo(roundTrip.amount()));
    }
}
