package com.brainridge.banking.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A record of money moving from one account to another.
 *
 * <p>Every successful transfer creates exactly one {@code Transaction}. These
 * records are what power the "transaction history" endpoint. All fields are
 * {@code final} — a transaction is a historical fact and must never be edited
 * after it happens.
 */
public class Transaction {

    /** Unique identifier for this transaction record. */
    private final UUID id;

    /** The account the money came from. */
    private final UUID fromAccountId;

    /** The account the money went to. */
    private final UUID toAccountId;

    /** How much money moved (always positive). */
    private final BigDecimal amount;

    /** Optional free-text note, e.g. "Rent payment". May be {@code null}. */
    private final String description;

    /** When the transfer happened. */
    private final Instant timestamp;

    /** What kind of transaction this is (currently only {@code TRANSFER}). */
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

    /**
     * Returns {@code true} if the given account is either the sender or the
     * receiver of this transaction.
     *
     * <p>Used when building an account's history: a transaction belongs in an
     * account's history whether that account sent the money or received it.
     */
    public boolean involvesAccount(UUID accountId) {
        return fromAccountId.equals(accountId) || toAccountId.equals(accountId);
    }
}
