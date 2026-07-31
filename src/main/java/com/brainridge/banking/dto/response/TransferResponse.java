package com.brainridge.banking.dto.response;

import com.brainridge.banking.model.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Outgoing confirmation returned right after a successful transfer.
 *
 * <p>It echoes back the key facts a caller wants immediately: the new
 * transaction's id, who paid whom, how much, and when. (The full record,
 * including the description, is available later via the history endpoint.)
 */
public class TransferResponse {

    private UUID transactionId;
    private UUID fromAccountId;
    private UUID toAccountId;
    private BigDecimal amount;
    private Instant timestamp;

    /** Builds the confirmation from the {@link Transaction} that was just recorded. */
    public static TransferResponse from(Transaction transaction) {
        TransferResponse response = new TransferResponse();
        response.transactionId = transaction.getId();
        response.fromAccountId = transaction.getFromAccountId();
        response.toAccountId = transaction.getToAccountId();
        response.amount = transaction.getAmount();
        response.timestamp = transaction.getTimestamp();
        return response;
    }

    public UUID getTransactionId() {
        return transactionId;
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

    public Instant getTimestamp() {
        return timestamp;
    }
}
