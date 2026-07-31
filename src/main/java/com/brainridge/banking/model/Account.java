package com.brainridge.banking.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A bank account — one of the two core things this app stores.
 *
 * <p>This is a plain domain object (it lives entirely in memory). Most fields
 * are {@code final} because they never change once the account is created; the
 * only value that changes over time is {@link #balance}, which goes up or down
 * as money is transferred.
 *
 * <p>Design notes:
 * <ul>
 *   <li><b>{@code UUID id}</b> — a random, unique identifier. Using UUIDs
 *       (instead of 1, 2, 3…) means two accounts created at the same time can
 *       never clash, and IDs don't leak how many accounts exist.</li>
 *   <li><b>{@code BigDecimal balance}</b> — money is stored as {@code BigDecimal},
 *       never {@code double}. Doubles lose precision on values like {@code 0.1},
 *       which is unacceptable for currency.</li>
 * </ul>
 */
public class Account {

    /** Unique, server-generated identifier for this account. */
    private final UUID id;

    /** Name of the person who owns the account. */
    private final String ownerName;

    /** Current amount of money in the account. Changes on every transfer. */
    private BigDecimal balance;

    /** When the account was opened. */
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

    /** Updates the balance. Called by the transfer logic when money moves. */
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
