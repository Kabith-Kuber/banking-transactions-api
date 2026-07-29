package com.brainridge.banking.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Transaction {

    private final UUID id;
    private final UUID fromAccountId;
    private final UUID toAccountId;
    private final BigDecimal amount;
    private final String description;
    private final Instant timestamp;
    private final TransactionType type;

    public Transaction(
            UUID id,
            UUID fromAccountId,
            UUID toAccountId,
            BigDecimal amount,
            String description,
            Instant timestamp,
            TransactionType type) {
        this.id = id;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.description = description;
        this.timestamp = timestamp;
        this.type = type;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFromAccountId() {
        return fromAccountId;
    }

    public UUID getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public TransactionType getType() {
        return type;
    }

    public boolean involvesAccount(UUID accountId) {
        return fromAccountId.equals(accountId) || toAccountId.equals(accountId);
    }
}
