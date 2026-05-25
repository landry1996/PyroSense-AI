package com.pyrosense.gateway.support;

import com.pyrosense.gateway.filter.RateLimitStore;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;

public class InMemoryRateLimitStore implements RateLimitStore {

    private long fixedCount = -1;
    private final AtomicLong counter = new AtomicLong(0);
    public String lastKey;

    public void setFixedCount(long count) {
        this.fixedCount = count;
    }

    @Override
    public Mono<Long> increment(String key) {
        this.lastKey = key;
        long value = fixedCount >= 0 ? fixedCount : counter.incrementAndGet();
        return Mono.just(value);
    }
}
