package com.pyrosense.device.application.usecase;

import com.pyrosense.device.application.port.out.DeviceCredentialRepositoryPort;
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

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RotateDeviceCredentialServiceTest {

    private DeviceRepositoryPort deviceRepository;
    private DeviceCredentialRepositoryPort credentialRepository;
    private DeviceEventPublisherPort eventPublisher;
    private ProvisioningAuditPort audit;
    private RotateDeviceCredentialService service;

    private static final DeviceId DEVICE_ID = DeviceId.generate();

    @BeforeEach
    void setUp() {
        deviceRepository = mock(DeviceRepositoryPort.class);
        credentialRepository = mock(DeviceCredentialRepositoryPort.class);
        eventPublisher = mock(DeviceEventPublisherPort.class);
        audit = mock(ProvisioningAuditPort.class);
        service = new RotateDeviceCredentialService(deviceRepository, credentialRepository,
                eventPublisher, audit);
    }

    private Device activeDevice() {
        return Device.reconstitute(DEVICE_ID, "SN-001", TenantId.generate(), null, null,
                "1.0.0", "rev-A", ConnectivityType.WIFI, DeviceStatus.ACTIVE,
                Instant.now(), Instant.now(), "hash", AuditMetadata.create("admin"));
    }

    @Test
    @DisplayName("Rotation revokes old credential and issues new one")
    void rotationRevokesOldAndIssuesNew() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(activeDevice()));
        var oldCred = DeviceCredential.issue(DEVICE_ID, 1).credential();
        when(credentialRepository.findActiveByDeviceId(DEVICE_ID)).thenReturn(Optional.of(oldCred));
        when(credentialRepository.nextVersionForDevice(DEVICE_ID)).thenReturn(2);

        var result = service.execute(DEVICE_ID, "admin");

        assertThat(result.newHmacKey()).isNotBlank();
        assertThat(result.version()).isEqualTo(2);
        verify(credentialRepository, times(2)).save(any(DeviceCredential.class));
        verify(eventPublisher).publish(anyList());
        verify(audit).logCredentialRotated(anyString(), eq(2), eq("admin"));
    }

    @Test
    @DisplayName("Rotation without existing credential still issues new one")
    void rotationWithoutExisting() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(activeDevice()));
        when(credentialRepository.findActiveByDeviceId(DEVICE_ID)).thenReturn(Optional.empty());
        when(credentialRepository.nextVersionForDevice(DEVICE_ID)).thenReturn(1);

        var result = service.execute(DEVICE_ID, "admin");

        assertThat(result.newHmacKey()).isNotBlank();
        assertThat(result.version()).isEqualTo(1);
        verify(credentialRepository, times(1)).save(any(DeviceCredential.class));
    }

    @Test
    @DisplayName("Rotation fails for revoked device")
    void rotationFailsForRevokedDevice() {
        var revoked = Device.reconstitute(DEVICE_ID, "SN-001", TenantId.generate(), null, null,
                "1.0.0", "rev-A", ConnectivityType.WIFI, DeviceStatus.REVOKED,
                null, null, "hash", AuditMetadata.create("admin"));
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.execute(DEVICE_ID, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DEVICE_REVOKED");
    }

    @Test
    @DisplayName("Rotation fails for unknown device")
    void rotationFailsForUnknownDevice() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(DEVICE_ID, "admin"))
                .isInstanceOf(NotFoundException.class);
    }
}
