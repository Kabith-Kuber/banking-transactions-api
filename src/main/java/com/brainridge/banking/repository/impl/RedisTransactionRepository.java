package com.brainridge.banking.repository.impl;

import com.brainridge.banking.model.Transaction;
import com.brainridge.banking.model.TransactionType;
import com.brainridge.banking.repository.TransactionRepository;
import com.brainridge.banking.store.UpstashRedisClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Shared transaction storage backed by Upstash Redis (used on Vercel).
 */
@Repository
@ConditionalOnProperty(name = "banking.storage", havingValue = "redis")
public class RedisTransactionRepository implements TransactionRepository {

    private static final String KEY = "banking:transactions";

    private final UpstashRedisClient redis;
    private final ObjectMapper objectMapper;

    public RedisTransactionRepository(UpstashRedisClient redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public Transaction save(Transaction transaction) {
        redis.hset(KEY, transaction.getId().toString(), toJson(transaction));
        return transaction;
    }

    @Override
    public List<Transaction> findByAccountId(UUID accountId) {
        return redis.hgetall(KEY).values().stream()
                .map(this::fromJson)
                .filter(transaction -> transaction.involvesAccount(accountId))
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    private String toJson(Transaction transaction) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", transaction.getId().toString());
            node.put("fromAccountId", transaction.getFromAccountId().toString());
            node.put("toAccountId", transaction.getToAccountId().toString());
            node.put("amount", transaction.getAmount().toPlainString());
            if (transaction.getDescription() == null) {
                node.putNull("description");
            } else {
                node.put("description", transaction.getDescription());
            }
            node.put("timestamp", transaction.getTimestamp().toString());
            node.put("type", transaction.getType().name());
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize transaction", e);
        }
    }

    private Transaction fromJson(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode descriptionNode = node.get("description");
            String description = descriptionNode == null || descriptionNode.isNull()
                    ? null
                    : descriptionNode.asText();
            return new Transaction(
                    UUID.fromString(node.get("id").asText()),
                    UUID.fromString(node.get("fromAccountId").asText()),
                    UUID.fromString(node.get("toAccountId").asText()),
                    new BigDecimal(node.get("amount").asText()),
                    description,
                    Instant.parse(node.get("timestamp").asText()),
                    TransactionType.valueOf(node.get("type").asText())
            );
        } catch (Exception e) {
            throw new IllegalStateException("Could not deserialize transaction", e);
        }
    }
}
