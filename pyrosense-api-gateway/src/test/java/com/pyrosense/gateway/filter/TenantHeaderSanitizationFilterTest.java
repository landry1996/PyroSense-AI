package com.pyrosense.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TenantHeaderSanitizationFilterTest {

    private final TenantHeaderSanitizationFilter filter = new TenantHeaderSanitizationFilter();

    @Test
    void shouldStripTenantIdHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test")
                .header("X-Tenant-Id", "spoofed-tenant")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(argThat(ex -> {
            ServerWebExchange e = (ServerWebExchange) ex;
            return e.getRequest().getHeaders().getFirst("X-Tenant-Id") == null;
        }));
    }

    @Test
    void shouldStripUserIdHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test")
                .header("X-User-Id", "fake-user")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(argThat(ex -> {
            ServerWebExchange e = (ServerWebExchange) ex;
            return e.getRequest().getHeaders().getFirst("X-User-Id") == null;
        }));
    }

    @Test
    void shouldStripRolesHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test")
                .header("X-Roles", "ADMIN,SUPER_USER")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(argThat(ex -> {
            ServerWebExchange e = (ServerWebExchange) ex;
            return e.getRequest().getHeaders().getFirst("X-Roles") == null;
        }));
    }

    @Test
    void shouldNotModifyRequestWithoutProtectedHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test")
                .header("Accept", "application/json")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    void shouldRunBeforeOtherFilters() {
        assertThat(filter.getOrder()).isEqualTo(-20);
    }
}
