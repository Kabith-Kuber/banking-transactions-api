package com.brainridge.banking.dto.response;

import com.brainridge.banking.model.Account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Outgoing data describing an account (what the API sends back as JSON).
 *
 * <p>This is a response DTO. We deliberately return this instead of the raw
 * {@link Account} domain object so the API's shape is a conscious decision:
 * if the internal model gains fields we don't want to expose, they won't leak
 * out just because someone added them to {@code Account}.
 */
public class AccountResponse {

    private UUID id;
    private String ownerName;
    private BigDecimal balance;
    private Instant createdAt;

    /**
     * Factory method that copies the relevant fields from a domain
     * {@link Account} into a response object. Keeping this conversion in one
     * place avoids repeating the mapping across controllers/services.
     */
    public static AccountResponse from(Account account) {
        AccountResponse response = new AccountResponse();
        response.id = account.getId();
        response.ownerName = account.getOwnerName();
        response.balance = account.getBalance();
        response.createdAt = account.getCreatedAt();
        return response;
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
