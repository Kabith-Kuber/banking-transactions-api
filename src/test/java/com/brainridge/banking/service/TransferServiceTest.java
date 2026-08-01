package com.brainridge.banking.service;

import com.brainridge.banking.dto.response.TransferResponse;
import com.brainridge.banking.exception.AccountNotFoundException;
import com.brainridge.banking.exception.InsufficientFundsException;
import com.brainridge.banking.exception.InvalidTransferException;
import com.brainridge.banking.model.Account;
import com.brainridge.banking.model.Transaction;
import com.brainridge.banking.repository.AccountRepository;
import com.brainridge.banking.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TransferService}, the money-moving logic.
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private TransferService transferService;

    private UUID fromAccountId;
    private UUID toAccountId;
    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        // null redis client => local in-memory locking path
        transferService = new TransferService(accountRepository, transactionRepository, null);

        fromAccountId = UUID.randomUUID();
        toAccountId = UUID.randomUUID();
        fromAccount = new Account(fromAccountId, "Alice", new BigDecimal("100.00"), Instant.now());
        toAccount = new Account(toAccountId, "Bob", new BigDecimal("50.00"), Instant.now());
    }

    @Test
    void transfer_successfullyMovesFunds() {
        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferResponse response = transferService.transfer(fromAccountId, toAccountId, new BigDecimal("25.00"), "Payment");

        assertEquals(fromAccountId, response.getFromAccountId());
        assertEquals(toAccountId, response.getToAccountId());
        assertEquals(new BigDecimal("25.00"), response.getAmount());
        assertEquals(new BigDecimal("75.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("75.00"), toAccount.getBalance());
        verify(accountRepository).save(fromAccount);
        verify(accountRepository).save(toAccount);
    }

    @Test
    void transfer_throwsWhenSameAccount() {
        assertThrows(InvalidTransferException.class,
                () -> transferService.transfer(fromAccountId, fromAccountId, new BigDecimal("10.00"), null));
    }

    @Test
    void transfer_throwsWhenFromAccountMissing() {
        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> transferService.transfer(fromAccountId, toAccountId, new BigDecimal("10.00"), null));
    }

    @Test
    void transfer_throwsWhenToAccountMissing() {
        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> transferService.transfer(fromAccountId, toAccountId, new BigDecimal("10.00"), null));
    }

    @Test
    void transfer_throwsWhenInsufficientFunds() {
        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

        assertThrows(InsufficientFundsException.class,
                () -> transferService.transfer(fromAccountId, toAccountId, new BigDecimal("150.00"), null));
    }
}
