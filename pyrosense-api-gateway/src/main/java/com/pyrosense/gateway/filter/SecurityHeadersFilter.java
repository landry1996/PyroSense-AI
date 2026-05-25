package com.pyrosense.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            var headers = response.getHeaders();
            headers.add("X-Content-Type-Options", "nosniff");
            headers.add("X-XSS-Protection", "0");
            headers.add("Referrer-Policy", "strict-origin-when-cross-origin");
            headers.add("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
            headers.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        }));
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
