package com.brainridge.banking.repository.impl;

import com.brainridge.banking.model.Account;
import com.brainridge.banking.repository.AccountRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryAccountRepository implements AccountRepository {

    private final ConcurrentHashMap<UUID, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public Account save(Account account) {
        accounts.put(account.getId(), account);
        return account;
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public boolean existsById(UUID id) {
        return accounts.containsKey(id);
    }
}
