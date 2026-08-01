package com.brainridge.banking.repository.impl;

import com.brainridge.banking.model.Account;
import com.brainridge.banking.repository.AccountRepository;
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
import java.util.Optional;
import java.util.UUID;

/**
 * Shared account storage backed by Upstash Redis (used on Vercel).
 */
@Repository
@ConditionalOnProperty(name = "banking.storage", havingValue = "redis")
public class RedisAccountRepository implements AccountRepository {

    private static final String KEY = "banking:accounts";

    private final UpstashRedisClient redis;
    private final ObjectMapper objectMapper;

    public RedisAccountRepository(UpstashRedisClient redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public Account save(Account account) {
        redis.hset(KEY, account.getId().toString(), toJson(account));
        return account;
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return redis.hget(KEY, id.toString()).map(this::fromJson);
    }

    @Override
    public List<Account> findAll() {
        return redis.hgetall(KEY).values().stream()
                .map(this::fromJson)
                .sorted(Comparator.comparing(Account::getCreatedAt))
                .toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return redis.hget(KEY, id.toString()).isPresent();
    }

    private String toJson(Account account) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", account.getId().toString());
            node.put("ownerName", account.getOwnerName());
            node.put("balance", account.getBalance().toPlainString());
            node.put("createdAt", account.getCreatedAt().toString());
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize account", e);
        }
    }

    private Account fromJson(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return new Account(
                    UUID.fromString(node.get("id").asText()),
                    node.get("ownerName").asText(),
                    new BigDecimal(node.get("balance").asText()),
                    Instant.parse(node.get("createdAt").asText())
            );
        } catch (Exception e) {
            throw new IllegalStateException("Could not deserialize account", e);
        }
    }
}
