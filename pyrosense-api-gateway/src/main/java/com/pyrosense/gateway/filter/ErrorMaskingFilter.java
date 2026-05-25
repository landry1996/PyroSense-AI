package com.pyrosense.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-1)
public class ErrorMaskingFilter implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorMaskingFilter.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status;
        String message;

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = switch (status) {
                case NOT_FOUND -> "Resource not found";
                case UNAUTHORIZED -> "Authentication required";
                case FORBIDDEN -> "Access denied";
                case TOO_MANY_REQUESTS -> "Rate limit exceeded";
                case SERVICE_UNAVAILABLE -> "Service temporarily unavailable";
                case BAD_GATEWAY -> "Service unavailable";
                case GATEWAY_TIMEOUT -> "Request timeout";
                default -> status.is5xxServerError() ? "Internal server error" : rse.getReason();
            };
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Internal server error";
        }

        String correlationId = (String) exchange.getAttributes().get(RequestTracingFilter.CORRELATION_ID_ATTR);
        log.error("Gateway error: status={} path={} correlationId={} error={}",
                status.value(), exchange.getRequest().getPath().value(),
                correlationId, ex.getMessage());

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"status":%d,"error":"%s","message":"%s","correlationId":"%s"}"""
                .formatted(status.value(), status.getReasonPhrase(), message,
                        correlationId != null ? correlationId : "unknown");

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes())));
    }
}
