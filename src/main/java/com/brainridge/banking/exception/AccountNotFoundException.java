package com.brainridge.banking.exception;

import java.util.UUID;

/**
 * Thrown when an operation references an account id that doesn't exist.
 *
 * <p>It extends {@link RuntimeException} so the service layer can throw it
 * without cluttering every method signature with {@code throws}. The
 * {@link com.brainridge.banking.exception.GlobalExceptionHandler} catches it
 * and turns it into a {@code 404 Not Found} response.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID accountId) {
        super("Account not found: " + accountId);
    }
}
