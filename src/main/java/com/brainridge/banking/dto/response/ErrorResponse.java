package com.brainridge.banking.dto.response;

import java.time.Instant;

/**
 * The consistent JSON shape returned whenever a request fails.
 *
 * <p>Every error — validation failure, missing account, insufficient funds —
 * comes back in this same format (built by
 * {@link com.brainridge.banking.exception.GlobalExceptionHandler}). A single,
 * predictable error shape makes the API far easier for clients to handle.
 *
 * <p>Example:
 * <pre>{@code
 * {
 *   "timestamp": "2026-07-31T16:00:00Z",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Account not found: 1234...",
 *   "path": "/api/v1/accounts/1234..."
 * }
 * }</pre>
 */
public class ErrorResponse {

    /** When the error happened. */
    private Instant timestamp;

    /** HTTP status code, e.g. 400, 404, 422. */
    private int status;

    /** Short status label, e.g. "Bad Request". */
    private String error;

    /** Human-readable explanation of what went wrong. */
    private String message;

    /** The request path that produced the error. */
    private String path;

    public ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }
}
