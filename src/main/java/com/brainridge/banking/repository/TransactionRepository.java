package com.brainridge.banking.repository;

import com.brainridge.banking.model.Transaction;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    List<Transaction> findByAccountId(UUID accountId);
}
