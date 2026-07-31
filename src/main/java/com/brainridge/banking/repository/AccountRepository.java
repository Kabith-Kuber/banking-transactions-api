package com.brainridge.banking.repository;

import com.brainridge.banking.model.Account;

import java.util.Optional;
import java.util.UUID;

/**
 * Data-access contract for accounts (the "Repository" layer).
 *
 * <p>This is an <b>interface</b>, so the services depend on <i>what</i> storage
 * can do, not <i>how</i> it does it. Today the implementation keeps data in
 * memory ({@link com.brainridge.banking.repository.impl.InMemoryAccountRepository});
 * swapping in a database later would mean writing a new implementation of this
 * interface, with zero changes to the service layer.
 */
public interface AccountRepository {

    /** Stores a new account or overwrites an existing one with the same id. */
    Account save(Account account);

    /**
     * Looks up an account by id.
     *
     * @return an {@link Optional} that is empty when no account matches — this
     *         forces callers to handle the "not found" case explicitly instead
     *         of risking a {@code null}.
     */
    Optional<Account> findById(UUID id);

    /** Cheap existence check used before reading an account's history. */
    boolean existsById(UUID id);
}
