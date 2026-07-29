package com.brainridge.banking.repository.impl;

import com.brainridge.banking.model.Transaction;
import com.brainridge.banking.repository.TransactionRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {

    private final ConcurrentHashMap<UUID, Transaction> transactions = new ConcurrentHashMap<>();

    @Override
    public Transaction save(Transaction transaction) {
        transactions.put(transaction.getId(), transaction);
        return transaction;
    }

    @Override
    public List<Transaction> findByAccountId(UUID accountId) {
        return transactions.values().stream()
                .filter(transaction -> transaction.involvesAccount(accountId))
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .collect(Collectors.toList());
    }
}
