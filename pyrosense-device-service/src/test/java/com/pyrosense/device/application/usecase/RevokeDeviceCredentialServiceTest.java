package com.pyrosense.device.application.usecase;

import com.pyrosense.device.application.port.out.*;
import com.pyrosense.device.domain.model.*;
import com.pyrosense.shared.audit.AuditMetadata;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RevokeDeviceCredentialServiceTest {

    private DeviceRepositoryPort deviceRepository;
    private DeviceCredentialRepositoryPort credentialRepository;
    private ClaimTokenRepositoryPort claimTokenRepository;
    private DeviceEventPublisherPort eventPublisher;
    private ProvisioningAuditPort audit;
    private RevokeDeviceCredentialService service;

    private static final DeviceId DEVICE_ID = DeviceId.generate();

    @BeforeEach
    void setUp() {
        deviceRepository = mock(DeviceRepositoryPort.class);
        credentialRepository = mock(DeviceCredentialRepositoryPort.class);
        claimTokenRepository = mock(ClaimTokenRepositoryPort.class);
        eventPublisher = mock(DeviceEventPublisherPort.class);
        audit = mock(ProvisioningAuditPort.class);
        service = new RevokeDeviceCredentialService(deviceRepository, credentialRepository,
                claimTokenRepository, eventPublisher, audit);
    }

    private Device activeDevice() {
        return Device.reconstitute(DEVICE_ID, "SN-001", TenantId.generate(), null, null,
                "1.0.0", "rev-A", ConnectivityType.WIFI, DeviceStatus.ACTIVE,
                Instant.now(), Instant.now(), "hash", AuditMetadata.create("admin"));
    }

    @Test
    @DisplayName("Revocation revokes all credentials and the device")
    void revocationRevokesAll() {
        var device = activeDevice();
        var cred1 = DeviceCredential.issue(DEVICE_ID, 1).credential();
        var cred2 = DeviceCredential.issue(DEVICE_ID, 2).credential();
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
        when(credentialRepository.findAllByDeviceId(DEVICE_ID)).thenReturn(List.of(cred1, cred2));

        service.execute(DEVICE_ID, "compromised", "admin");

        verify(credentialRepository, times(2)).save(any(DeviceCredential.class));
        verify(claimTokenRepository).invalidateAllForDevice(DEVICE_ID);
        verify(deviceRepository).save(device);
        verify(eventPublisher).publish(anyList());
        verify(audit).logDeviceRevoked(anyString(), eq("compromised"), eq("admin"));
    }

    @Test
    @DisplayName("Revocation fails for unknown device")
    void revocationFailsForUnknown() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(DEVICE_ID, "reason", "admin"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Revocation is idempotent for already revoked device")
    void revocationIdempotent() {
        var device = Device.reconstitute(DEVICE_ID, "SN-001", TenantId.generate(), null, null,
                "1.0.0", "rev-A", ConnectivityType.WIFI, DeviceStatus.REVOKED,
                null, null, "hash", AuditMetadata.create("admin"));
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
        when(credentialRepository.findAllByDeviceId(DEVICE_ID)).thenReturn(List.of());

        service.execute(DEVICE_ID, "reason", "admin");

        verify(deviceRepository).save(device);
        verify(audit).logDeviceRevoked(anyString(), eq("reason"), eq("admin"));
    }

    @Test
    @DisplayName("Revocation invalidates pending claim tokens")
    void revocationInvalidatesClaimTokens() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(activeDevice()));
        when(credentialRepository.findAllByDeviceId(DEVICE_ID)).thenReturn(List.of());

        service.execute(DEVICE_ID, "reason", "admin");

        verify(claimTokenRepository).invalidateAllForDevice(DEVICE_ID);
    }
}
