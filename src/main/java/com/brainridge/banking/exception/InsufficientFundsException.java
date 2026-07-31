package com.brainridge.banking.exception;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Thrown when a transfer asks for more money than the source account holds.
 *
 * <p>The message captures the balance and requested amount to make debugging
 * and logging easy. The
 * {@link com.brainridge.banking.exception.GlobalExceptionHandler} maps this to
 * a {@code 422 Unprocessable Entity} — the request was well-formed, but the
 * business rule (enough funds) wasn't satisfied.
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(UUID accountId, BigDecimal balance, BigDecimal amount) {
        super("Insufficient funds in account " + accountId + ": balance=" + balance + ", requested=" + amount);
    }
}
