package com.brainridge.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Incoming data for "create an account" (the JSON body of {@code POST /api/v1/accounts}).
 *
 * <p>This is a request DTO: its only job is to carry data <i>into</i> the app.
 * Keeping this separate from the {@link com.brainridge.banking.model.Account}
 * domain object means the outside world can never set fields it shouldn't
 * (like the id or balance directly).
 *
 * <p>The annotations below are <b>validation rules</b>. When a controller
 * method marks this object with {@code @Valid}, Spring checks every rule before
 * running any business logic. If a rule fails, the request is rejected with a
 * {@code 400 Bad Request} automatically.
 */
public class CreateAccountRequest {

    /** Owner's name. {@code @NotBlank} rejects null, empty, and whitespace-only values. */
    @NotBlank(message = "ownerName must not be blank")
    private String ownerName;

    /** Starting balance. Must be provided and cannot be negative. */
    @NotNull(message = "initialBalance must not be null")
    @DecimalMin(value = "0.00", message = "initialBalance must be greater than or equal to 0")
    private BigDecimal initialBalance;

    // Getters and setters are required so Spring can populate this object from JSON.

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }
}
