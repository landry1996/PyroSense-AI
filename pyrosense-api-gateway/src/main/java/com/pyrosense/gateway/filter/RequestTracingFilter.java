package com.pyrosense.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class RequestTracingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestTracingFilter.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_ATTR = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        exchange.getAttributes().put(CORRELATION_ID_ATTR, correlationId);

        String finalCorrelationId = correlationId;
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();
        long start = System.currentTimeMillis();

        return chain.filter(exchange.mutate().request(mutated).build())
                .contextWrite(ctx -> ctx.put(CORRELATION_ID_ATTR, finalCorrelationId))
                .doFinally(signal -> {
                    long duration = System.currentTimeMillis() - start;
                    int status = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value() : 0;
                    MDC.put("correlationId", finalCorrelationId);
                    log.info("gateway.request method={} path={} status={} duration={}ms correlationId={}",
                            method, path, status, duration, finalCorrelationId);
                    MDC.remove("correlationId");
                });
    }

    @Override
    public int getOrder() {
        return -10;
    }
}
