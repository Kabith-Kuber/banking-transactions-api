package com.brainridge.banking.exception;

/**
 * Thrown when a transfer is logically invalid even though the input passed
 * basic validation — for example, trying to transfer money to the same account.
 *
 * <p>Maps to a {@code 400 Bad Request} via the
 * {@link com.brainridge.banking.exception.GlobalExceptionHandler}.
 */
public class InvalidTransferException extends RuntimeException {

    public InvalidTransferException(String message) {
        super(message);
    }
}
