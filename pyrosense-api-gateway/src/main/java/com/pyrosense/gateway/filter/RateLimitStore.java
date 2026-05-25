package com.pyrosense.gateway.filter;

import reactor.core.publisher.Mono;

public interface RateLimitStore {
    Mono<Long> increment(String key);
}
