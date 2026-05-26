package com.pyrosense.notification.adapter.out.deduplication;

import com.pyrosense.notification.application.port.out.DeduplicationPort;
import com.pyrosense.notification.domain.model.DeduplicationKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Primary
@Profile("!test")
public class RedisDeduplicationAdapter implements DeduplicationPort {

    private static final String KEY_PREFIX = "notif:dedup:";

    private final StringRedisTemplate redisTemplate;
    private final Duration dedupWindow;

    public RedisDeduplicationAdapter(StringRedisTemplate redisTemplate,
                                     @Value("${pyrosense.notification.deduplication.window-minutes:30}") int windowMinutes) {
        this.redisTemplate = redisTemplate;
        this.dedupWindow = Duration.ofMinutes(windowMinutes);
    }

    @Override
    public boolean isDuplicate(DeduplicationKey key) {
        String redisKey = KEY_PREFIX + key.toKeyString();
        return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
    }

    @Override
    public void markSent(DeduplicationKey key) {
        String redisKey = KEY_PREFIX + key.toKeyString();
        redisTemplate.opsForValue().set(redisKey, "1", dedupWindow);
    }
}
