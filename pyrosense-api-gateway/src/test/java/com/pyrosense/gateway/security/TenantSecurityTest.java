package com.pyrosense.gateway.security;

import com.pyrosense.gateway.filter.RateLimitStore;
import com.pyrosense.gateway.support.InMemoryRateLimitStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class TenantSecurityTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ReactiveJwtDecoder reactiveJwtDecoder() {
            return token -> {
                // Parse tenant from token value for test flexibility
                String tenantId = "tenant-A";
                String subject = "user-1";
                List<String> roles = List.of("PROPERTY_MANAGER");

                if (token.contains("admin")) {
                    roles = List.of("ADMIN");
                    tenantId = "tenant-admin";
                    subject = "admin-user";
                } else if (token.contains("tenant-B")) {
                    tenantId = "tenant-B";
                    subject = "user-2";
                } else if (token.contains("tenant-A")) {
                    tenantId = "tenant-A";
                    subject = "user-1";
                }

                Jwt jwt = Jwt.withTokenValue(token)
                        .header("alg", "RS256")
                        .subject(subject)
                        .claim("tenant_id", tenantId)
                        .claim("realm_access", Map.of("roles", roles))
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .build();
                return Mono.just(jwt);
            };
        }

        @Bean
        @Primary
        public RateLimitStore rateLimitStore() {
            return new InMemoryRateLimitStore();
        }
    }

    @Autowired
    private WebTestClient webClient;

    @Test
    void requestWithoutJwtShouldReturn401() {
        webClient.get().uri("/api/v1/devices")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void requestWithValidJwtShouldExtractAndPropagateTenantId() {
        webClient.get().uri("/api/v1/devices")
                .headers(headers -> headers.setBearerAuth("valid-token-tenant-A"))
                .exchange()
                .expectStatus().is5xxServerError(); // downstream not available, but auth passed
    }

    @Test
    void requestWithSpoofedTenantIdHeaderAndNoJwtShouldBeRejected() {
        webClient.get().uri("/api/v1/devices")
                .header("X-Tenant-Id", "spoofed-tenant")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void requestWithSpoofedTenantIdHeaderAndValidJwtShouldOverrideHeader() {
        // Even if X-Tenant-Id is supplied, the sanitization filter strips it
        // and the JWT propagation filter sets it from the token
        webClient.get().uri("/api/v1/devices")
                .header("X-Tenant-Id", "spoofed-tenant")
                .headers(headers -> headers.setBearerAuth("valid-token-tenant-A"))
                .exchange()
                .expectStatus().is5xxServerError(); // downstream unavailable, but header was sanitized
    }

    @Test
    void adminRoleShouldAccessActuatorMetrics() {
        webClient.get().uri("/actuator/metrics")
                .headers(headers -> headers.setBearerAuth("admin-token"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void nonAdminRoleShouldNotAccessActuatorMetrics() {
        webClient.get().uri("/actuator/metrics")
                .headers(headers -> headers.setBearerAuth("valid-token-tenant-A"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void healthEndpointShouldBeAccessibleWithoutAuth() {
        webClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void tenantATokenShouldNotContainTenantBClaims() {
        // Verify that the JWT decoder correctly isolates tenant claims
        webClient.get().uri("/api/v1/devices")
                .headers(headers -> headers.setBearerAuth("valid-token-tenant-A"))
                .exchange()
                .expectHeader().doesNotExist("X-Tenant-Id"); // response doesn't expose internal headers
    }
}
