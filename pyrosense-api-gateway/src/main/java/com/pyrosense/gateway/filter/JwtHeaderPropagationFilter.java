package com.pyrosense.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class JwtHeaderPropagationFilter implements GlobalFilter, Ordered {

    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String ROLES_HEADER = "X-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> {
                    Jwt jwt = auth.getToken();
                    ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();

                    String userId = jwt.getSubject();
                    if (userId != null) {
                        requestBuilder.header(USER_ID_HEADER, userId);
                    }

                    String tenantId = extractTenantId(jwt);
                    if (tenantId != null) {
                        requestBuilder.header(TENANT_ID_HEADER, tenantId);
                    }

                    String roles = extractRoles(jwt);
                    if (!roles.isEmpty()) {
                        requestBuilder.header(ROLES_HEADER, roles);
                    }

                    ServerHttpRequest mutated = requestBuilder.build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @SuppressWarnings("unchecked")
    private String extractTenantId(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId != null) return tenantId;

        Map<String, Object> attributes = jwt.getClaimAsMap("attributes");
        if (attributes != null && attributes.containsKey("tenant_id")) {
            return attributes.get("tenant_id").toString();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractRoles(Jwt jwt) {
        Stream<String> roles;
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            roles = ((List<String>) realmAccess.get("roles")).stream();
        } else {
            List<String> directRoles = jwt.getClaimAsStringList("roles");
            roles = directRoles != null ? directRoles.stream() : Stream.empty();
        }
        return String.join(",", roles.toList());
    }

    @Override
    public int getOrder() {
        return -5;
    }
}
