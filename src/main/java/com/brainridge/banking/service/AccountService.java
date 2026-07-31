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

/**
 * Business logic for accounts (the "Service" layer).
 *
 * <p>The service sits between the controllers (which speak HTTP) and the
 * repositories (which speak storage). Controllers stay thin and just forward
 * requests here; this class owns the actual rules for creating and reading
 * accounts.
 *
 * <p><b>Dependency injection:</b> the {@link AccountRepository} is passed into
 * the constructor rather than created here. Spring supplies the right
 * implementation automatically, which keeps this class easy to test (a fake
 * repository can be injected instead).
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Creates and stores a brand-new account.
     *
     * <p>The server generates the id and creation time — the caller only
     * supplies the owner's name and starting balance. The balance is normalized
     * to two decimal places so it is stored consistently.
     *
     * @return a DTO describing the account that was created
     */
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

    /**
     * Looks up a single account by id.
     *
     * @throws AccountNotFoundException if no account has that id, which the
     *         global handler turns into a 404 response
     */
    public AccountResponse getAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return AccountResponse.from(account);
    }
}
