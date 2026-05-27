package com.pyrosense.device.application.usecase;

import com.pyrosense.device.application.port.in.CreateClaimTokenUseCase;
import com.pyrosense.device.application.port.out.ClaimTokenRepositoryPort;
import com.pyrosense.device.application.port.out.DeviceEventPublisherPort;
import com.pyrosense.device.application.port.out.DeviceRepositoryPort;
import com.pyrosense.device.application.port.out.ProvisioningAuditPort;
import com.pyrosense.device.domain.model.*;
import com.pyrosense.shared.audit.AuditMetadata;
import com.pyrosense.shared.exception.BusinessException;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreateClaimTokenServiceTest {

    private DeviceRepositoryPort deviceRepository;
    private ClaimTokenRepositoryPort claimTokenRepository;
    private DeviceEventPublisherPort eventPublisher;
    private ProvisioningAuditPort audit;
    private CreateClaimTokenService service;

    @BeforeEach
    void setUp() {
        deviceRepository = mock(DeviceRepositoryPort.class);
        claimTokenRepository = mock(ClaimTokenRepositoryPort.class);
        eventPublisher = mock(DeviceEventPublisherPort.class);
        audit = mock(ProvisioningAuditPort.class);
        service = new CreateClaimTokenService(deviceRepository, claimTokenRepository,
                eventPublisher, audit, Duration.ofHours(24));
    }

    private Device registeredDevice(DeviceId id) {
        return Device.reconstitute(id, "SN-001", null, null, null,
                "1.0.0", "rev-A", ConnectivityType.WIFI, DeviceStatus.REGISTERED,
                null, null, "hash", AuditMetadata.create("admin"));
    }

    @Test
    @DisplayName("Creates claim token for registered device")
    void createsTokenForRegisteredDevice() {
        var deviceId = DeviceId.generate();
        var tenantId = TenantId.generate();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(registeredDevice(deviceId)));

        var result = service.execute(
                new CreateClaimTokenUseCase.CreateClaimTokenCommand(deviceId, tenantId), "admin");

        assertThat(result.token()).isNotBlank();
        assertThat(result.expiresAt()).isAfter(Instant.now());
        verify(claimTokenRepository).invalidateAllForDevice(deviceId);
        verify(claimTokenRepository).save(any(ClaimToken.class));
        verify(eventPublisher).publish(anyList());
        verify(audit).logClaimTokenCreated(anyString(), anyString(), eq("admin"));
    }

    @Test
    @DisplayName("Throws when device not found")
    void throwsWhenDeviceNotFound() {
        var deviceId = DeviceId.generate();
        var tenantId = TenantId.generate();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
                new CreateClaimTokenUseCase.CreateClaimTokenCommand(deviceId, tenantId), "admin"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Throws when device is revoked")
    void throwsWhenDeviceRevoked() {
        var deviceId = DeviceId.generate();
        var tenantId = TenantId.generate();
        var device = Device.reconstitute(deviceId, "SN-001", tenantId, null, null,
                "1.0.0", "rev-A", ConnectivityType.WIFI, DeviceStatus.REVOKED,
                null, null, "hash", AuditMetadata.create("admin"));
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> service.execute(
                new CreateClaimTokenUseCase.CreateClaimTokenCommand(deviceId, tenantId), "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("Throws when device is active (invalid state)")
    void throwsWhenDeviceActive() {
        var deviceId = DeviceId.generate();
        var tenantId = TenantId.generate();
        var device = Device.reconstitute(deviceId, "SN-001", tenantId, null, null,
                "1.0.0", "rev-A", ConnectivityType.WIFI, DeviceStatus.ACTIVE,
                Instant.now(), null, "hash", AuditMetadata.create("admin"));
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> service.execute(
                new CreateClaimTokenUseCase.CreateClaimTokenCommand(deviceId, tenantId), "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("REGISTERED or PROVISIONED");
    }

    @Test
    @DisplayName("Invalidates existing tokens before creating new one")
    void invalidatesExistingTokens() {
        var deviceId = DeviceId.generate();
        var tenantId = TenantId.generate();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(registeredDevice(deviceId)));

        service.execute(new CreateClaimTokenUseCase.CreateClaimTokenCommand(deviceId, tenantId), "admin");

        var inOrder = inOrder(claimTokenRepository);
        inOrder.verify(claimTokenRepository).invalidateAllForDevice(deviceId);
        inOrder.verify(claimTokenRepository).save(any(ClaimToken.class));
    }
}
