package com.brainridge.banking.repository.impl;

import com.brainridge.banking.model.Account;
import com.brainridge.banking.repository.AccountRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link AccountRepository}.
 *
 * <p>The assignment asks for in-memory storage (no database), so accounts live
 * in a single {@link ConcurrentHashMap} keyed by account id. This means all
 * data is lost when the app restarts — that trade-off is intentional and
 * documented in the README.
 *
 * <p>A {@code ConcurrentHashMap} (rather than a plain {@code HashMap}) is used
 * because a web server handles requests on many threads at once; this map lets
 * those threads read and write safely without corrupting the data.
 *
 * <p>{@code @Repository} marks this as a Spring-managed component, so Spring
 * creates one instance and injects it wherever an {@code AccountRepository} is
 * needed.
 */
@Repository
@ConditionalOnProperty(name = "banking.storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryAccountRepository implements AccountRepository {

    /** The actual storage: account id -> account. */
    private final ConcurrentHashMap<UUID, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public Account save(Account account) {
        accounts.put(account.getId(), account);
        return account;
    }

    @Override
    public Optional<Account> findById(UUID id) {
        // ofNullable turns a possible null (missing key) into an empty Optional.
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public List<Account> findAll() {
        return accounts.values().stream()
                .sorted(Comparator.comparing(Account::getCreatedAt))
                .toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return accounts.containsKey(id);
    }
}
