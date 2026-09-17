package com.hassanusman.harbor.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private BigDecimal creditLimit;

    protected Customer() {
    }

    public Customer(String id, String name, String type, BigDecimal creditLimit) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.creditLimit = creditLimit;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }
}
