package com.brainridge.banking.repository.impl;

import com.brainridge.banking.model.Transaction;
import com.brainridge.banking.repository.TransactionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of {@link TransactionRepository}.
 *
 * <p>Transactions are stored in a {@link ConcurrentHashMap} keyed by
 * transaction id, following the same in-memory, thread-safe approach as
 * {@link InMemoryAccountRepository}.
 */
@Repository
@ConditionalOnProperty(name = "banking.storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryTransactionRepository implements TransactionRepository {

    /** The actual storage: transaction id -> transaction. */
    private final ConcurrentHashMap<UUID, Transaction> transactions = new ConcurrentHashMap<>();

    @Override
    public Transaction save(Transaction transaction) {
        transactions.put(transaction.getId(), transaction);
        return transaction;
    }

    @Override
    public List<Transaction> findByAccountId(UUID accountId) {
        return transactions.values().stream()
                // Keep only transactions where this account sent or received money.
                .filter(transaction -> transaction.involvesAccount(accountId))
                // Newest first, so the most recent activity shows at the top.
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .collect(Collectors.toList());
    }
}
