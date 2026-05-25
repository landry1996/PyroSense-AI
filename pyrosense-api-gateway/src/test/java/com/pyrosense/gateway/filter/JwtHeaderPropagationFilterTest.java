package com.pyrosense.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtHeaderPropagationFilterTest {

    private final JwtHeaderPropagationFilter filter = new JwtHeaderPropagationFilter();

    @Test
    void shouldPropagateUserIdFromJwt() {
        Jwt jwt = buildJwt("user-123", "tenant-abc", List.of("ADMIN"));
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block();

        verify(chain).filter(argThat(ex -> {
            ServerWebExchange e = (ServerWebExchange) ex;
            return "user-123".equals(e.getRequest().getHeaders().getFirst("X-User-Id"));
        }));
    }

    @Test
    void shouldPropagateTenantIdFromJwt() {
        Jwt jwt = buildJwt("user-1", "tenant-xyz", List.of("PROPERTY_MANAGER"));
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block();

        verify(chain).filter(argThat(ex -> {
            ServerWebExchange e = (ServerWebExchange) ex;
            return "tenant-xyz".equals(e.getRequest().getHeaders().getFirst("X-Tenant-Id"));
        }));
    }

    @Test
    void shouldPropagateRolesFromJwt() {
        Jwt jwt = buildJwtWithRealmAccess("user-1", "tenant-1", List.of("ADMIN", "ELECTRICIAN"));
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .block();

        verify(chain).filter(argThat(ex -> {
            ServerWebExchange e = (ServerWebExchange) ex;
            String roles = e.getRequest().getHeaders().getFirst("X-Roles");
            return roles != null && roles.contains("ADMIN") && roles.contains("ELECTRICIAN");
        }));
    }

    @Test
    void shouldPassThroughWhenNoAuthentication() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    void shouldHaveCorrectOrder() {
        assertThat(filter.getOrder()).isEqualTo(-5);
    }

    private Jwt buildJwt(String subject, String tenantId, List<String> roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .claim("tenant_id", tenantId)
                .claim("roles", roles)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private Jwt buildJwtWithRealmAccess(String subject, String tenantId, List<String> roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .claim("tenant_id", tenantId)
                .claim("realm_access", Map.of("roles", roles))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
