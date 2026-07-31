package com.brainridge.banking.model;

/**
 * The kind of transaction being recorded.
 *
 * <p>Right now the app only supports transfers between accounts, so there is a
 * single value. It is modelled as an enum (rather than a plain string) so that
 * adding future types — for example {@code DEPOSIT} or {@code WITHDRAWAL} —
 * is a small, safe change that the compiler can help verify.
 */
public enum TransactionType {
    TRANSFER
}
