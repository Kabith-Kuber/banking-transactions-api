package com.brainridge.banking.service;

import com.brainridge.banking.dto.response.AccountResponse;
import com.brainridge.banking.exception.AccountNotFoundException;
import com.brainridge.banking.model.Account;
import com.brainridge.banking.repository.AccountRepository;
import com.brainridge.banking.util.MoneyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponse createAccount(String ownerName, BigDecimal initialBalance) {
        Account account = new Account(
                UUID.randomUUID(),
                ownerName.trim(),
                MoneyUtils.normalize(initialBalance),
                Instant.now()
        );
        accountRepository.save(account);
        log.info("Created account id={} owner={} balance={}", account.getId(), account.getOwnerName(), account.getBalance());
        return AccountResponse.from(account);
    }

    public AccountResponse getAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return AccountResponse.from(account);
    }

    public void ensureAccountExists(UUID accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
    }
}
