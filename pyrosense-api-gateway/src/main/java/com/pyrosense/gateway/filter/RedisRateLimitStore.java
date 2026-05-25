package com.pyrosense.gateway.filter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@ConditionalOnBean(ReactiveStringRedisTemplate.class)
public class RedisRateLimitStore implements RateLimitStore {

    private final ReactiveStringRedisTemplate redisTemplate;

    public RedisRateLimitStore(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Long> increment(String key) {
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        return redisTemplate.expire(key, Duration.ofMinutes(1)).thenReturn(count);
                    }
                    return Mono.just(count);
                });
    }
}
