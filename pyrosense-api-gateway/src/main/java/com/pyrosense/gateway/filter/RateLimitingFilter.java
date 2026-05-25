package com.pyrosense.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private static final int DEFAULT_REQUESTS_PER_MINUTE = 60;
    private static final int DEVICE_REQUESTS_PER_MINUTE = 120;
    private static final int AUTH_REQUESTS_PER_MINUTE = 10;

    private final RateLimitStore store;

    public RateLimitingFilter(RateLimitStore store) {
        this.store = store;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = extractClientIp(exchange);
        String path = exchange.getRequest().getPath().value();
        int limit = resolveLimit(path);

        String key = "rate:" + clientIp + ":" + resolveBucket(path);

        return store.increment(key)
                .flatMap(count -> {
                    exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
                    exchange.getResponse().getHeaders().add("X-RateLimit-Remaining",
                            String.valueOf(Math.max(0, limit - count)));

                    if (count > limit) {
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse().getHeaders().add("Retry-After", "60");
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                });
    }

    private String extractClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress();
    }

    private int resolveLimit(String path) {
        if (path.startsWith("/api/v1/auth")) return AUTH_REQUESTS_PER_MINUTE;
        if (path.startsWith("/api/v1/ingestion") || path.startsWith("/api/v1/devices")) return DEVICE_REQUESTS_PER_MINUTE;
        return DEFAULT_REQUESTS_PER_MINUTE;
    }

    private String resolveBucket(String path) {
        if (path.startsWith("/api/v1/auth")) return "auth";
        if (path.startsWith("/api/v1/ingestion")) return "ingestion";
        return "default";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
