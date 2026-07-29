package com.brainridge.banking.repository;

import com.brainridge.banking.model.Account;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(UUID id);

    boolean existsById(UUID id);
}
