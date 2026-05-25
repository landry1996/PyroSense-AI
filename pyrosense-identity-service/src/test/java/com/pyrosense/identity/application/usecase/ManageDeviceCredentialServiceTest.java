package com.pyrosense.identity.application.usecase;

import com.pyrosense.identity.adapter.out.persistence.InMemoryDeviceCredentialRepository;
import com.pyrosense.identity.application.port.in.ManageDeviceCredentialUseCase.DeviceCredentialResult;
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

import static org.assertj.core.api.Assertions.*;

class ManageDeviceCredentialServiceTest {

    private ManageDeviceCredentialService service;
    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");
    private final DeviceId deviceId = DeviceId.generate();
    private final TenantId tenantId = TenantId.generate();

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(now, ZoneId.of("UTC")));
        service = new ManageDeviceCredentialService(new InMemoryDeviceCredentialRepository());
    }

    @AfterEach
    void tearDown() { ClockProvider.reset(); }

    @Test
    void shouldIssueCredential() {
        DeviceCredentialResult result = service.issueCredential(deviceId, tenantId, Duration.ofDays(90));

        assertThat(result.deviceId()).isEqualTo(deviceId);
        assertThat(result.token()).isNotBlank();
        assertThat(result.expiresAt()).isEqualTo(now.plus(Duration.ofDays(90)));
    }

    @Test
    void shouldValidateCorrectToken() {
        DeviceCredentialResult result = service.issueCredential(deviceId, tenantId, Duration.ofDays(90));

        assertThat(service.validateToken(deviceId, result.token())).isTrue();
    }

    @Test
    void shouldRejectIncorrectToken() {
        service.issueCredential(deviceId, tenantId, Duration.ofDays(90));

        assertThat(service.validateToken(deviceId, "wrong-token")).isFalse();
    }

    @Test
    void shouldRotateCredential() {
        DeviceCredentialResult original = service.issueCredential(deviceId, tenantId, Duration.ofDays(30));
        DeviceCredentialResult rotated = service.rotateCredential(deviceId);

        assertThat(rotated.token()).isNotEqualTo(original.token());
        assertThat(service.validateToken(deviceId, rotated.token())).isTrue();
        assertThat(service.validateToken(deviceId, original.token())).isFalse();
    }

    @Test
    void shouldRevokeCredential() {
        DeviceCredentialResult result = service.issueCredential(deviceId, tenantId, Duration.ofDays(90));

        service.revokeCredential(deviceId);

        assertThat(service.validateToken(deviceId, result.token())).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() {
        service.issueCredential(deviceId, tenantId, Duration.ofDays(1));
        ClockProvider.setClock(Clock.fixed(now.plus(Duration.ofDays(2)), ZoneId.of("UTC")));

        assertThat(service.validateToken(deviceId, "any-token")).isFalse();
    }
}
