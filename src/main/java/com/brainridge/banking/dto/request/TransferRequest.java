package com.brainridge.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Incoming data for "transfer money" (the JSON body of {@code POST /api/v1/transfers}).
 *
 * <p>The validation annotations guarantee the basics before any money logic
 * runs: both account ids are present, the amount is a positive value of at
 * least one cent, and the description isn't unreasonably long. Business rules
 * that need the actual account data — such as "you can't send more than you
 * have" — are checked later in the service layer.
 */
public class TransferRequest {

    /** Account to take money from. Required. */
    @NotNull(message = "fromAccountId must not be null")
    private UUID fromAccountId;

    /** Account to send money to. Required. */
    @NotNull(message = "toAccountId must not be null")
    private UUID toAccountId;

    /** Amount to move. Must be at least 0.01 so zero/negative transfers are rejected. */
    @NotNull(message = "amount must not be null")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    /** Optional note. Capped at 255 characters to keep records tidy. */
    @Size(max = 255, message = "description must be at most 255 characters")
    private String description;

    // Getters and setters let Spring build this object from the request JSON.

    public UUID getFromAccountId() {
        return fromAccountId;
    }

    public void setFromAccountId(UUID fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public UUID getToAccountId() {
        return toAccountId;
    }

    public void setToAccountId(UUID toAccountId) {
        this.toAccountId = toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
