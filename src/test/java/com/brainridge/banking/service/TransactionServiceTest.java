package com.brainridge.banking.service;

import com.brainridge.banking.dto.response.PageResponse;
import com.brainridge.banking.dto.response.TransactionResponse;
import com.brainridge.banking.exception.AccountNotFoundException;
import com.brainridge.banking.model.Transaction;
import com.brainridge.banking.model.TransactionType;
import com.brainridge.banking.repository.AccountRepository;
import com.brainridge.banking.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void getTransactionHistory_returnsPaginatedResults() {
        UUID accountId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                accountId,
                UUID.randomUUID(),
                new BigDecimal("10.00"),
                "Test",
                Instant.now(),
                TransactionType.TRANSFER
        );
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(transactionRepository.findByAccountId(accountId)).thenReturn(List.of(transaction));

        PageResponse<TransactionResponse> response = transactionService.getTransactionHistory(accountId, 0, 20);

        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());
        assertEquals(transaction.getId(), response.getContent().get(0).getId());
    }

    @Test
    void getTransactionHistory_throwsWhenAccountMissing() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.existsById(accountId)).thenReturn(false);

        assertThrows(AccountNotFoundException.class,
                () -> transactionService.getTransactionHistory(accountId, 0, 20));
    }
}
