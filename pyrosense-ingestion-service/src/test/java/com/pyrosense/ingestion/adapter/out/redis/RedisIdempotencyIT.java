package com.pyrosense.ingestion.adapter.out.redis;

import com.pyrosense.ingestion.adapter.out.redis.RedisIdempotencyAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for RedisIdempotencyAdapter using a real Redis container.
 * Verifies idempotency check and TTL expiry behavior.
 */
@SpringBootTest(classes = {
        RedisAutoConfiguration.class,
        RedisIdempotencyAdapter.class
})
@Testcontainers
@ActiveProfiles("test")
class RedisIdempotencyIT {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisIdempotencyAdapter idempotencyAdapter;

    @BeforeEach
    void setUp() {
        // Clear all keys before each test
        var keys = redisTemplate.keys("ingestion:idem:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("Should return false for first occurrence (not a duplicate)")
    void shouldReturnFalseForFirstOccurrence() {
        var key = "device-" + UUID.randomUUID() + "-" + System.currentTimeMillis();

        boolean isDuplicate = idempotencyAdapter.isDuplicate(key);

        assertThat(isDuplicate).isFalse();
    }

    @Test
    @DisplayName("Should return true for second occurrence (is a duplicate)")
    void shouldReturnTrueForSecondOccurrence() {
        var key = "device-" + UUID.randomUUID() + "-" + System.currentTimeMillis();

        // Mark as processed
        idempotencyAdapter.markProcessed(key, Duration.ofMinutes(5));

        // Second check should be duplicate
        boolean isDuplicate = idempotencyAdapter.isDuplicate(key);

        assertThat(isDuplicate).isTrue();
    }

    @Test
    @DisplayName("Should mark processed with TTL and key should eventually expire")
    void shouldExpireAfterTtl() throws InterruptedException {
        var key = "device-ttl-" + UUID.randomUUID();

        // Mark processed with a very short TTL (2 seconds)
        idempotencyAdapter.markProcessed(key, Duration.ofSeconds(2));

        // Immediately should be duplicate
        assertThat(idempotencyAdapter.isDuplicate(key)).isTrue();

        // Wait for expiry
        Thread.sleep(2500);

        // After TTL, should no longer be duplicate
        assertThat(idempotencyAdapter.isDuplicate(key)).isFalse();
    }

    @Test
    @DisplayName("Should handle multiple independent keys without interference")
    void shouldHandleMultipleKeysIndependently() {
        var key1 = "device-a-" + UUID.randomUUID();
        var key2 = "device-b-" + UUID.randomUUID();
        var key3 = "device-c-" + UUID.randomUUID();

        // Mark only key1 as processed
        idempotencyAdapter.markProcessed(key1, Duration.ofMinutes(5));

        assertThat(idempotencyAdapter.isDuplicate(key1)).isTrue();
        assertThat(idempotencyAdapter.isDuplicate(key2)).isFalse();
        assertThat(idempotencyAdapter.isDuplicate(key3)).isFalse();
    }

    @Test
    @DisplayName("Should store correct value in Redis with expected key prefix")
    void shouldStoreWithCorrectKeyPrefix() {
        var key = "verify-prefix-" + UUID.randomUUID();

        idempotencyAdapter.markProcessed(key, Duration.ofMinutes(5));

        // Verify the key is stored with the expected prefix
        var storedValue = redisTemplate.opsForValue().get("ingestion:idem:" + key);
        assertThat(storedValue).isEqualTo("1");
    }

    @Test
    @DisplayName("Should set correct TTL on stored keys")
    void shouldSetCorrectTtl() {
        var key = "verify-ttl-" + UUID.randomUUID();
        var ttl = Duration.ofMinutes(10);

        idempotencyAdapter.markProcessed(key, ttl);

        var remainingTtl = redisTemplate.getExpire("ingestion:idem:" + key);
        assertThat(remainingTtl).isNotNull();
        // TTL should be close to 600 seconds (10 minutes), allow some margin
        assertThat(remainingTtl).isBetween(590L, 600L);
    }
}
