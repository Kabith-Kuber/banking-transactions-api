package com.brainridge.banking.service;

import com.brainridge.banking.dto.response.AccountResponse;
import com.brainridge.banking.exception.AccountNotFoundException;
import com.brainridge.banking.model.Account;
import com.brainridge.banking.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AccountService}.
 *
 * <p>These tests exercise the service in isolation: the repository is replaced
 * with a Mockito mock ({@code @Mock}), so we test only the service's logic
 * without touching real storage. {@code @InjectMocks} builds the service and
 * passes the mock into its constructor — the same dependency injection the app
 * uses at runtime, just with a fake.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccount_returnsCreatedAccount() {
        AccountResponse response = accountService.createAccount("Jane Doe", new BigDecimal("1000.00"));

        // Capture the Account handed to the repository to confirm what was saved.
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());

        assertEquals("Jane Doe", response.getOwnerName());
        assertEquals(new BigDecimal("1000.00"), response.getBalance());
        assertEquals(captor.getValue().getId(), response.getId());
    }

    @Test
    void getAccount_returnsAccountWhenFound() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account(accountId, "Jane Doe", new BigDecimal("500.00"), Instant.now());
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccount(accountId);

        assertEquals(accountId, response.getId());
        assertEquals("Jane Doe", response.getOwnerName());
    }

    @Test
    void getAccount_throwsWhenNotFound() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getAccount(accountId));
    }
}
