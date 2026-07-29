package com.brainridge.banking.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Account {

    private final UUID id;
    private final String ownerName;
    private BigDecimal balance;
    private final Instant createdAt;

    public Account(UUID id, String ownerName, BigDecimal balance, Instant createdAt) {
        this.id = id;
        this.ownerName = ownerName;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
