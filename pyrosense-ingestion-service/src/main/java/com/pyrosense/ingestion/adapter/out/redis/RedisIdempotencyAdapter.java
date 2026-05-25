package com.pyrosense.ingestion.adapter.out.redis;

import com.pyrosense.ingestion.application.port.out.IdempotencyPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisIdempotencyAdapter implements IdempotencyPort {

    private static final String KEY_PREFIX = "ingestion:idem:";

    private final StringRedisTemplate redisTemplate;

    public RedisIdempotencyAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isDuplicate(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + key));
    }

    @Override
    public void markProcessed(String key, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + key, "1", ttl);
    }
}
