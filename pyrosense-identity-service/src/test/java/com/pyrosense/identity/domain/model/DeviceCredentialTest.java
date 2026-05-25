package com.pyrosense.identity.domain.model;

import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class DeviceCredentialTest {

    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");

    @BeforeEach
    void setUp() { ClockProvider.setClock(Clock.fixed(now, ZoneId.of("UTC"))); }

    @AfterEach
    void tearDown() { ClockProvider.reset(); }

    @Test
    void shouldBeValidWhenActiveAndNotExpired() {
        DeviceCredential cred = new DeviceCredential(UUID.randomUUID(), DeviceId.generate(),
                TenantId.generate(), "hash", now.plus(Duration.ofDays(90)));

        assertThat(cred.isValid()).isTrue();
    }

    @Test
    void shouldBeInvalidWhenExpired() {
        DeviceCredential cred = new DeviceCredential(UUID.randomUUID(), DeviceId.generate(),
                TenantId.generate(), "hash", now.minus(Duration.ofHours(1)));

        assertThat(cred.isValid()).isFalse();
    }

    @Test
    void shouldBeInvalidWhenRevoked() {
        DeviceCredential cred = new DeviceCredential(UUID.randomUUID(), DeviceId.generate(),
                TenantId.generate(), "hash", now.plus(Duration.ofDays(90)));
        cred.revoke();

        assertThat(cred.isValid()).isFalse();
    }

    @Test
    void shouldRotateToken() {
        DeviceCredential cred = new DeviceCredential(UUID.randomUUID(), DeviceId.generate(),
                TenantId.generate(), "old-hash", now.plus(Duration.ofDays(30)));

        Instant newExpiry = now.plus(Duration.ofDays(90));
        cred.rotate("new-hash", newExpiry);

        assertThat(cred.getTokenHash()).isEqualTo("new-hash");
        assertThat(cred.getExpiresAt()).isEqualTo(newExpiry);
    }

    @Test
    void shouldDetectExpiringSoon() {
        DeviceCredential cred = new DeviceCredential(UUID.randomUUID(), DeviceId.generate(),
                TenantId.generate(), "hash", now.plus(Duration.ofDays(5)));

        assertThat(cred.isExpiringSoon(Duration.ofDays(7))).isTrue();
        assertThat(cred.isExpiringSoon(Duration.ofDays(3))).isFalse();
    }

    @Test
    void shouldRecordAuthentication() {
        DeviceCredential cred = new DeviceCredential(UUID.randomUUID(), DeviceId.generate(),
                TenantId.generate(), "hash", now.plus(Duration.ofDays(90)));

        cred.recordAuthentication();
        cred.recordAuthentication();

        assertThat(cred.getAuthenticationCount()).isEqualTo(2);
        assertThat(cred.getLastAuthenticatedAt()).isEqualTo(now);
    }
}
