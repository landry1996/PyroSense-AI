package com.pyrosense.gateway.filter;

import com.pyrosense.gateway.support.InMemoryRateLimitStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingFilterTest {

    private InMemoryRateLimitStore store;
    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        store = new InMemoryRateLimitStore();
        filter = new RateLimitingFilter(store);
    }

    @Test
    void shouldAllowRequestUnderLimit() {
        store.setFixedCount(1L);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/devices")
                .remoteAddress(new InetSocketAddress("192.168.1.1", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("120");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("119");
    }

    @Test
    void shouldRejectRequestOverDefaultLimit() {
        store.setFixedCount(61L);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test")
                .remoteAddress(new InetSocketAddress("192.168.1.1", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("60");
    }

    @Test
    void shouldApplyStricterLimitForAuthEndpoints() {
        store.setFixedCount(11L);

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login")
                .remoteAddress(new InetSocketAddress("10.0.0.1", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void shouldApplyHigherLimitForDeviceEndpoints() {
        store.setFixedCount(100L);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/devices/123")
                .remoteAddress(new InetSocketAddress("10.0.0.1", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("120");
    }

    @Test
    void shouldExtractIpFromXForwardedFor() {
        store.setFixedCount(1L);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test")
                .header("X-Forwarded-For", "203.0.113.1, 10.0.0.1")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(store.lastKey).contains("203.0.113.1");
    }

    @Test
    void shouldHaveCorrectOrder() {
        assertThat(filter.getOrder()).isEqualTo(0);
    }
}
