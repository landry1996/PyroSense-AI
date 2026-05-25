package com.pyrosense.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PayloadSizeLimitFilterTest {

    private final PayloadSizeLimitFilter filter = new PayloadSizeLimitFilter(1024);

    @Test
    void shouldRejectOversizedPayload() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/test")
                .header("Content-Length", "2048")
                .body("x".repeat(2048));
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        verifyNoInteractions(chain);
    }

    @Test
    void shouldAllowPayloadWithinLimit() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/test")
                .header("Content-Length", "512")
                .body("x".repeat(512));
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    void shouldAllowRequestWithoutContentLength() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    void shouldRunEarly() {
        assertThat(filter.getOrder()).isEqualTo(-15);
    }
}
