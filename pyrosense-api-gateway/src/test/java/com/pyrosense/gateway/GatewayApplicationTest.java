package com.pyrosense.gateway;

import com.pyrosense.gateway.filter.RateLimitStore;
import com.pyrosense.gateway.support.InMemoryRateLimitStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class GatewayApplicationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ReactiveJwtDecoder reactiveJwtDecoder() {
            return token -> {
                Jwt jwt = Jwt.withTokenValue(token)
                        .header("alg", "RS256")
                        .subject("test-user")
                        .claim("tenant_id", "test-tenant")
                        .claim("realm_access", Map.of("roles", List.of("ADMIN")))
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
    private ApplicationContext context;

    @Autowired
    private WebTestClient webClient;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    void healthEndpointShouldBeAccessible() {
        webClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void unauthenticatedRequestShouldReturn401() {
        webClient.get().uri("/api/v1/devices")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void infoEndpointShouldBePublic() {
        webClient.get().uri("/actuator/info")
                .exchange()
                .expectStatus().isOk();
    }
}
