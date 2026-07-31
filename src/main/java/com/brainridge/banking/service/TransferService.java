package com.brainridge.banking.service;

import com.brainridge.banking.dto.response.TransferResponse;
import com.brainridge.banking.exception.AccountNotFoundException;
import com.brainridge.banking.exception.InsufficientFundsException;
import com.brainridge.banking.exception.InvalidTransferException;
import com.brainridge.banking.model.Account;
import com.brainridge.banking.model.Transaction;
import com.brainridge.banking.model.TransactionType;
import com.brainridge.banking.repository.AccountRepository;
import com.brainridge.banking.repository.TransactionRepository;
import com.brainridge.banking.util.MoneyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Business logic for moving money between accounts (the "Service" layer).
 *
 * <p>This is the heart of the app. A transfer must be <b>all-or-nothing</b>:
 * money leaves one account and lands in the other, or nothing happens at all.
 * It must also be safe when many transfers run at the same time. This class
 * handles both concerns before recording the resulting {@link Transaction}.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * One lock object per account id. Two transfers touching the same account
     * must take turns; this map hands out (and remembers) a dedicated lock for
     * each account so we can synchronize on it.
     */
    private final ConcurrentHashMap<UUID, Object> accountLocks = new ConcurrentHashMap<>();

    public TransferService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Moves {@code amount} from one account to another and records it.
     *
     * <p>Steps, in order:
     * <ol>
     *   <li>Reject a transfer to the same account (nonsensical).</li>
     *   <li>Lock both accounts so no other transfer can interfere mid-way.</li>
     *   <li>Confirm both accounts exist.</li>
     *   <li>Confirm the sender has enough money.</li>
     *   <li>Update both balances and save them.</li>
     *   <li>Record the transaction and return a confirmation.</li>
     * </ol>
     *
     * @throws InvalidTransferException   if source and destination are the same
     * @throws AccountNotFoundException   if either account doesn't exist
     * @throws InsufficientFundsException if the sender can't cover the amount
     */
    public TransferResponse transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String description) {
        BigDecimal normalizedAmount = MoneyUtils.normalize(amount);

        if (fromAccountId.equals(toAccountId)) {
            throw new InvalidTransferException("Cannot transfer to the same account");
        }

        // Deadlock avoidance: always lock accounts in a consistent order (by id).
        // If transfer A->B and transfer B->A ran at once and each grabbed one lock
        // first, they could wait on each other forever. Sorting the two ids means
        // every transfer acquires locks in the same order, so that can't happen.
        UUID firstLockId = fromAccountId.compareTo(toAccountId) < 0 ? fromAccountId : toAccountId;
        UUID secondLockId = fromAccountId.compareTo(toAccountId) < 0 ? toAccountId : fromAccountId;

        synchronized (lockFor(firstLockId)) {
            synchronized (lockFor(secondLockId)) {
                // Read both accounts inside the locks so their balances can't change
                // out from under us while we work.
                Account fromAccount = accountRepository.findById(fromAccountId)
                        .orElseThrow(() -> new AccountNotFoundException(fromAccountId));
                Account toAccount = accountRepository.findById(toAccountId)
                        .orElseThrow(() -> new AccountNotFoundException(toAccountId));

                // Business rule: you can't send more than you have.
                if (fromAccount.getBalance().compareTo(normalizedAmount) < 0) {
                    log.warn("Insufficient funds for transfer from={} balance={} amount={}",
                            fromAccountId, fromAccount.getBalance(), normalizedAmount);
                    throw new InsufficientFundsException(fromAccountId, fromAccount.getBalance(), normalizedAmount);
                }

                // Move the money: subtract from sender, add to receiver.
                fromAccount.setBalance(MoneyUtils.normalize(fromAccount.getBalance().subtract(normalizedAmount)));
                toAccount.setBalance(MoneyUtils.normalize(toAccount.getBalance().add(normalizedAmount)));

                accountRepository.save(fromAccount);
                accountRepository.save(toAccount);

                // Record the movement so it shows up in both accounts' history.
                Transaction transaction = new Transaction(
                        UUID.randomUUID(),
                        fromAccountId,
                        toAccountId,
                        normalizedAmount,
                        description,
                        Instant.now(),
                        TransactionType.TRANSFER
                );
                transactionRepository.save(transaction);

                log.info("Transfer completed transactionId={} from={} to={} amount={}",
                        transaction.getId(), fromAccountId, toAccountId, normalizedAmount);

                return TransferResponse.from(transaction);
            }
        }
    }

    /**
     * Returns the shared lock object for an account, creating it on first use.
     * {@code computeIfAbsent} guarantees every caller asking for the same id
     * gets the exact same lock instance.
     */
    private Object lockFor(UUID accountId) {
        return accountLocks.computeIfAbsent(accountId, id -> new Object());
    }
}
