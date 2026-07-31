package com.brainridge.banking.repository;

import com.brainridge.banking.model.Transaction;

import java.util.List;
import java.util.UUID;

/**
 * Data-access contract for transactions (the "Repository" layer).
 *
 * <p>Like {@link AccountRepository}, this hides the storage details behind an
 * interface so the rest of the app never depends on how transactions are kept.
 */
public interface TransactionRepository {

    /** Stores a transaction record. */
    Transaction save(Transaction transaction);

    /**
     * Returns every transaction that involves the given account — whether the
     * account was the sender or the receiver — newest first.
     */
    List<Transaction> findByAccountId(UUID accountId);
}
