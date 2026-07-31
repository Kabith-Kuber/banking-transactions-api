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

/**
 * Business logic for reading an account's transaction history (the "Service" layer).
 *
 * <p>Its main job is to return history one page at a time so responses stay
 * small even when an account has many transactions. It depends on both
 * repositories: the account repository to confirm the account exists, and the
 * transaction repository to fetch the records.
 */
@Service
public class TransactionService {

    /** Page shown when the caller doesn't specify one. */
    private static final int DEFAULT_PAGE = 0;

    /** Number of records per page when the caller doesn't specify a size. */
    private static final int DEFAULT_SIZE = 20;

    /** Upper limit on page size, so a single request can't ask for everything at once. */
    private static final int MAX_SIZE = 100;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Returns one page of an account's transaction history, newest first.
     *
     * @param accountId the account whose history to read
     * @param page      which page to return (0-based); {@code null} defaults to page 0
     * @param size      how many records per page; {@code null} defaults to 20, capped at 100
     * @throws AccountNotFoundException if the account doesn't exist
     */
    public PageResponse<TransactionResponse> getTransactionHistory(UUID accountId, Integer page, Integer size) {
        // Fail fast with a clear 404 if the account isn't real.
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        // Clamp the paging inputs into safe ranges (no negative page, sensible size).
        int resolvedPage = page == null ? DEFAULT_PAGE : Math.max(page, 0);
        int resolvedSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);

        // The repository already returns the full list sorted newest-first.
        List<Transaction> transactions = transactionRepository.findByAccountId(accountId);
        long totalElements = transactions.size();

        // Work out the slice for the requested page, staying inside the list bounds.
        int fromIndex = Math.min(resolvedPage * resolvedSize, transactions.size());
        int toIndex = Math.min(fromIndex + resolvedSize, transactions.size());

        // Convert only the records on this page into response DTOs.
        List<TransactionResponse> content = transactions.subList(fromIndex, toIndex).stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());

        return new PageResponse<>(content, resolvedPage, resolvedSize, totalElements);
    }
}
