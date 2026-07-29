package com.brainridge.banking.dto.response;

import com.brainridge.banking.model.Transaction;
import com.brainridge.banking.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TransactionResponse {

    private UUID id;
    private UUID fromAccountId;
    private UUID toAccountId;
    private BigDecimal amount;
    private String description;
    private Instant timestamp;
    private TransactionType type;

    public static TransactionResponse from(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.id = transaction.getId();
        response.fromAccountId = transaction.getFromAccountId();
        response.toAccountId = transaction.getToAccountId();
        response.amount = transaction.getAmount();
        response.description = transaction.getDescription();
        response.timestamp = transaction.getTimestamp();
        response.type = transaction.getType();
        return response;
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
}
