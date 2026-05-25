package com.pyrosense.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class PayloadSizeLimitFilter implements GlobalFilter, Ordered {

    private final long maxPayloadBytes;

    public PayloadSizeLimitFilter(@Value("${pyrosense.gateway.max-payload-bytes:1048576}") long maxPayloadBytes) {
        this.maxPayloadBytes = maxPayloadBytes;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long contentLength = exchange.getRequest().getHeaders().getContentLength();
        if (contentLength > maxPayloadBytes) {
            exchange.getResponse().setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE);
            exchange.getResponse().getHeaders().add("Content-Type", "application/json");
            byte[] body = "{\"error\":\"Payload too large\",\"maxBytes\":%d}".formatted(maxPayloadBytes).getBytes();
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -15;
    }
}
