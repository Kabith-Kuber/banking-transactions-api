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
import com.brainridge.banking.store.UpstashRedisClient;
import com.brainridge.banking.util.MoneyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final UpstashRedisClient redisClient;

    /**
     * One lock object per account id (local / in-memory mode only).
     * On Vercel, Redis locks are used instead so every instance cooperates.
     */
    private final ConcurrentHashMap<UUID, Object> accountLocks = new ConcurrentHashMap<>();

    @Autowired
    public TransferService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            @Autowired(required = false) UpstashRedisClient redisClient) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.redisClient = redisClient;
    }

    /**
     * Moves {@code amount} from one account to another and records it.
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
        UUID firstLockId = fromAccountId.compareTo(toAccountId) < 0 ? fromAccountId : toAccountId;
        UUID secondLockId = fromAccountId.compareTo(toAccountId) < 0 ? toAccountId : fromAccountId;

        if (redisClient != null) {
            String lockKey = "banking:lock:" + firstLockId + ":" + secondLockId;
            return redisClient.withLock(lockKey,
                    () -> performTransfer(fromAccountId, toAccountId, normalizedAmount, description));
        }

        synchronized (lockFor(firstLockId)) {
            synchronized (lockFor(secondLockId)) {
                return performTransfer(fromAccountId, toAccountId, normalizedAmount, description);
            }
        }
    }

    private TransferResponse performTransfer(
            UUID fromAccountId,
            UUID toAccountId,
            BigDecimal normalizedAmount,
            String description) {
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new AccountNotFoundException(fromAccountId));
        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new AccountNotFoundException(toAccountId));

        if (fromAccount.getBalance().compareTo(normalizedAmount) < 0) {
            log.warn("Insufficient funds for transfer from={} balance={} amount={}",
                    fromAccountId, fromAccount.getBalance(), normalizedAmount);
            throw new InsufficientFundsException(fromAccountId, fromAccount.getBalance(), normalizedAmount);
        }

        fromAccount.setBalance(MoneyUtils.normalize(fromAccount.getBalance().subtract(normalizedAmount)));
        toAccount.setBalance(MoneyUtils.normalize(toAccount.getBalance().add(normalizedAmount)));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

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

    private Object lockFor(UUID accountId) {
        return accountLocks.computeIfAbsent(accountId, id -> new Object());
    }
}
