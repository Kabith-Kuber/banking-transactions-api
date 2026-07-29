package com.brainridge.banking.service;

import com.brainridge.banking.dto.response.PageResponse;
import com.brainridge.banking.dto.response.TransactionResponse;
import com.brainridge.banking.exception.AccountNotFoundException;
import com.brainridge.banking.model.Transaction;
import com.brainridge.banking.repository.AccountRepository;
import com.brainridge.banking.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public PageResponse<TransactionResponse> getTransactionHistory(UUID accountId, Integer page, Integer size) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        int resolvedPage = page == null ? DEFAULT_PAGE : Math.max(page, 0);
        int resolvedSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);

        List<Transaction> transactions = transactionRepository.findByAccountId(accountId);
        long totalElements = transactions.size();

        int fromIndex = Math.min(resolvedPage * resolvedSize, transactions.size());
        int toIndex = Math.min(fromIndex + resolvedSize, transactions.size());

        List<TransactionResponse> content = transactions.subList(fromIndex, toIndex).stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());

        return new PageResponse<>(content, resolvedPage, resolvedSize, totalElements);
    }
}
