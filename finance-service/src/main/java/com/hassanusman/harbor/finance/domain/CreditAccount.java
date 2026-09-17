package com.hassanusman.harbor.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
@Table(name = "credit_account")
public class CreditAccount {

    @Id
    private String customerId;

    @Column(nullable = false)
    private BigDecimal creditLimit;

    @Column(nullable = false)
    private BigDecimal used;

    @Version
    private Long version;

    protected CreditAccount() {
    }

    public CreditAccount(String customerId, BigDecimal creditLimit, BigDecimal used) {
        this.customerId = customerId;
        this.creditLimit = creditLimit;
        this.used = used;
    }

    public boolean reserve(BigDecimal amount) {
        if (used.add(amount).compareTo(creditLimit) > 0) {
            return false;
        }
        used = used.add(amount);
        return true;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public BigDecimal getUsed() {
        return used;
    }

    public BigDecimal available() {
        return creditLimit.subtract(used);
    }
}
