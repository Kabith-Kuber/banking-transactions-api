package com.brainridge.banking.dto.response;

import com.brainridge.banking.model.Account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AccountResponse {

    private UUID id;
    private String ownerName;
    private BigDecimal balance;
    private Instant createdAt;

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
