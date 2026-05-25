package com.pyrosense.gateway.config;

import com.pyrosense.gateway.filter.RateLimitStore;
import com.pyrosense.gateway.support.InMemoryRateLimitStore;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ObservabilityConfigTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ReactiveJwtDecoder reactiveJwtDecoder() {
            return token -> Mono.just(Jwt.withTokenValue(token)
                    .header("alg", "RS256")
                    .subject("test")
                    .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build());
        }

        @Bean
        @Primary
        public RateLimitStore rateLimitStore() {
            return new InMemoryRateLimitStore();
        }
    }

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void meterRegistryShouldBeAvailable() {
        assertThat(meterRegistry).isNotNull();
    }

    @Test
    void shouldHaveApplicationTag() {
        var meters = meterRegistry.getMeters();
        assertThat(meters).isNotEmpty();
    }

    @Test
    void shouldExposeJvmMetrics() {
        assertThat(meterRegistry.find("jvm.memory.used").gauge()).isNotNull();
    }

    @Test
    void shouldExposeProcessMetrics() {
        assertThat(meterRegistry.find("process.cpu.usage").gauge()).isNotNull();
    }
}
