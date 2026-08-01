package com.brainridge.banking.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Tiny Upstash Redis REST client.
 *
 * <p>Used on Vercel so every serverless instance shares the same account and
 * transaction data. Locally this bean is not created (no URL configured), and
 * the app keeps using plain in-memory maps.
 */
@Component
@ConditionalOnProperty(prefix = "upstash.redis.rest", name = "url")
public class UpstashRedisClient {

    private static final Logger log = LoggerFactory.getLogger(UpstashRedisClient.class);

    private final String url;
    private final String token;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public UpstashRedisClient(
            @Value("${upstash.redis.rest.url}") String url,
            @Value("${upstash.redis.rest.token}") String token,
            ObjectMapper objectMapper) {
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.token = token;
        this.objectMapper = objectMapper;
        log.info("Shared Redis storage enabled via Upstash");
    }

    public void hset(String key, String field, String value) {
        execute("HSET", key, field, value);
    }

    public Optional<String> hget(String key, String field) {
        JsonNode result = execute("HGET", key, field);
        if (result == null || result.isNull()) {
            return Optional.empty();
        }
        return Optional.of(result.asText());
    }

    public Map<String, String> hgetall(String key) {
        JsonNode result = execute("HGETALL", key);
        Map<String, String> map = new LinkedHashMap<>();
        if (result == null || !result.isArray()) {
            return map;
        }
        // Upstash returns [field, value, field, value, ...]
        for (int i = 0; i + 1 < result.size(); i += 2) {
            map.put(result.get(i).asText(), result.get(i + 1).asText());
        }
        return map;
    }

    /**
     * Runs {@code action} while holding a short-lived Redis lock so transfers
     * stay safe across multiple Vercel instances.
     */
    public <T> T withLock(String lockKey, Supplier<T> action) {
        String owner = UUID.randomUUID().toString();
        for (int attempt = 0; attempt < 40; attempt++) {
            JsonNode result = execute("SET", lockKey, owner, "NX", "EX", "15");
            if (result != null && !result.isNull() && "OK".equalsIgnoreCase(result.asText())) {
                try {
                    return action.get();
                } finally {
                    // Best-effort unlock. Lock also expires via EX if this fails.
                    try {
                        execute("DEL", lockKey);
                    } catch (Exception e) {
                        log.warn("Failed to release lock {}", lockKey, e);
                    }
                }
            }
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for transfer lock", e);
            }
        }
        throw new IllegalStateException("Could not acquire transfer lock");
    }

    private JsonNode execute(Object... command) {
        try {
            List<Object> body = new ArrayList<>(command.length);
            for (Object part : command) {
                body.add(part);
            }
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Redis command failed (" + response.statusCode() + "): " + response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            return root.get("result");
        } catch (Exception e) {
            if (e instanceof IllegalStateException ise) {
                throw ise;
            }
            throw new IllegalStateException("Redis request failed: " + e.getMessage(), e);
        }
    }
}
