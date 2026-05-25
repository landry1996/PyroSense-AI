package com.pyrosense.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
public class TenantHeaderSanitizationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TenantHeaderSanitizationFilter.class);

    private static final Set<String> PROTECTED_HEADERS = Set.of(
            "x-tenant-id",
            "x-user-id",
            "x-roles"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        boolean hasProtectedHeaders = request.getHeaders().keySet().stream()
                .anyMatch(h -> PROTECTED_HEADERS.contains(h.toLowerCase()));

        if (hasProtectedHeaders) {
            ServerHttpRequest.Builder builder = request.mutate();
            for (String header : PROTECTED_HEADERS) {
                builder.headers(h -> h.remove(header));
                builder.headers(h -> h.remove(header.substring(0, 1).toUpperCase() + header.substring(1)));
            }
            builder.headers(h -> {
                h.remove("X-Tenant-Id");
                h.remove("X-User-Id");
                h.remove("X-Roles");
            });

            log.warn("Stripped protected headers from incoming request: path={}",
                    request.getPath().value());

            return chain.filter(exchange.mutate().request(builder.build()).build());
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -20;
    }
}
