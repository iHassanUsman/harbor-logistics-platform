package com.hassanusman.harbor.shipment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "shipment")
public class Shipment {

    @Id
    private String id;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Column(nullable = false)
    private Instant updatedAt;

    private String rejectReason;

    protected Shipment() {
    }

    public Shipment(String id, String customerId, String origin, String destination, BigDecimal amount) {
        this.id = id;
        this.customerId = customerId;
        this.origin = origin;
        this.destination = destination;
        this.amount = amount;
        this.status = ShipmentStatus.PENDING;
        this.updatedAt = Instant.now();
    }

    public void confirm() {
        this.status = ShipmentStatus.CONFIRMED;
        this.updatedAt = Instant.now();
        this.rejectReason = null;
    }

    public void reject(String reason) {
        this.status = ShipmentStatus.REJECTED;
        this.updatedAt = Instant.now();
        this.rejectReason = reason;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getRejectReason() {
        return rejectReason;
    }
}
