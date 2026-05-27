package com.pyrosense.device.application.usecase;

import com.pyrosense.device.application.port.in.DeviceProvisioningUseCase;
import com.pyrosense.device.application.port.out.*;
import com.pyrosense.device.domain.model.*;
import com.pyrosense.shared.audit.AuditMetadata;
import com.pyrosense.shared.exception.BusinessException;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;
import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeviceProvisioningServiceTest {

    private ClaimTokenRepositoryPort claimTokenRepository;
    private DeviceRepositoryPort deviceRepository;
    private DeviceCredentialRepositoryPort credentialRepository;
    private ProvisioningSessionRepositoryPort sessionRepository;
    private DeviceEventPublisherPort eventPublisher;
    private ProvisioningAuditPort audit;
    private DeviceProvisioningService service;

    private static final DeviceId DEVICE_ID = DeviceId.generate();
    private static final TenantId TENANT_ID = TenantId.generate();
    private static final String SERIAL = "SN-001";

    @BeforeEach
    void setUp() {
        claimTokenRepository = mock(ClaimTokenRepositoryPort.class);
        deviceRepository = mock(DeviceRepositoryPort.class);
        credentialRepository = mock(DeviceCredentialRepositoryPort.class);
        sessionRepository = mock(ProvisioningSessionRepositoryPort.class);
        eventPublisher = mock(DeviceEventPublisherPort.class);
        audit = mock(ProvisioningAuditPort.class);
        service = new DeviceProvisioningService(claimTokenRepository, deviceRepository,
                credentialRepository, sessionRepository, eventPublisher, audit,
                "tcp://broker.pyrosense.io", 8883);
    }

    private Device registeredDevice() {
        return Device.reconstitute(DEVICE_ID, SERIAL, null,
                BuildingId.generate(), ElectricalPanelId.generate(),
                "1.0.0", "rev-A", ConnectivityType.WIFI, DeviceStatus.REGISTERED,
                null, null, "hash", AuditMetadata.create("admin"));
    }

    private ClaimToken validToken(String plainToken) {
        var pair = ClaimToken.create(DEVICE_ID, TENANT_ID, Duration.ofHours(24), "admin");
        return pair.claimToken();
    }

    @Test
    @DisplayName("Successful provisioning returns credentials and MQTT config")
    void successfulProvisioning() {
        var tokenPair = ClaimToken.create(DEVICE_ID, TENANT_ID, Duration.ofHours(24), "admin");
        when(sessionRepository.countRecentFailedByIp(anyString(), anyInt())).thenReturn(0);
        when(deviceRepository.findBySerialNumber(SERIAL)).thenReturn(Optional.of(registeredDevice()));
        when(claimTokenRepository.findActiveByDeviceId(DEVICE_ID)).thenReturn(Optional.of(tokenPair.claimToken()));
        when(credentialRepository.nextVersionForDevice(DEVICE_ID)).thenReturn(1);

        var command = new DeviceProvisioningUseCase.ProvisionWithTokenCommand(
                tokenPair.plainToken(), SERIAL, "PyroSense-v1", "0.1.0", "192.168.1.100");
        var result = service.execute(command);

        assertThat(result.deviceId()).isEqualTo(DEVICE_ID.value().toString());
        assertThat(result.tenantId()).isEqualTo(TENANT_ID.value().toString());
        assertThat(result.hmacKey()).isNotBlank();
        assertThat(result.mqttBrokerUri()).isEqualTo("tcp://broker.pyrosense.io");
        assertThat(result.mqttPort()).isEqualTo(8883);
        assertThat(result.topicPrefix()).contains("pyrosense/v1/");

        verify(credentialRepository).save(any(DeviceCredential.class));
        verify(sessionRepository).save(any(DeviceProvisioningSession.class));
        verify(eventPublisher).publish(anyList());
    }

    @Test
    @DisplayName("Rate limiting rejects too many attempts from same IP")
    void rateLimitingRejects() {
        when(sessionRepository.countRecentFailedByIp("10.0.0.1", 15)).thenReturn(10);

        var command = new DeviceProvisioningUseCase.ProvisionWithTokenCommand(
                "token", SERIAL, "model", "0.1.0", "10.0.0.1");

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("RATE_LIMITED");
    }

    @Test
    @DisplayName("Unknown device serial is rejected")
    void unknownSerialRejected() {
        when(sessionRepository.countRecentFailedByIp(anyString(), anyInt())).thenReturn(0);
        when(deviceRepository.findBySerialNumber("UNKNOWN")).thenReturn(Optional.empty());

        var command = new DeviceProvisioningUseCase.ProvisionWithTokenCommand(
                "token", "UNKNOWN", "model", "0.1.0", "10.0.0.1");

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DEVICE_NOT_FOUND");
    }

    @Test
    @DisplayName("Invalid claim token is rejected")
    void invalidTokenRejected() {
        var tokenPair = ClaimToken.create(DEVICE_ID, TENANT_ID, Duration.ofHours(24), "admin");
        when(sessionRepository.countRecentFailedByIp(anyString(), anyInt())).thenReturn(0);
        when(deviceRepository.findBySerialNumber(SERIAL)).thenReturn(Optional.of(registeredDevice()));
        when(claimTokenRepository.findActiveByDeviceId(DEVICE_ID)).thenReturn(Optional.of(tokenPair.claimToken()));

        var command = new DeviceProvisioningUseCase.ProvisionWithTokenCommand(
                "wrong-token", SERIAL, "model", "0.1.0", "10.0.0.1");

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("INVALID_TOKEN");
    }

    @Test
    @DisplayName("Expired token is rejected")
    void expiredTokenRejected() {
        var tokenPair = ClaimToken.create(DEVICE_ID, TENANT_ID, Duration.ofMillis(-1), "admin");
        when(sessionRepository.countRecentFailedByIp(anyString(), anyInt())).thenReturn(0);
        when(deviceRepository.findBySerialNumber(SERIAL)).thenReturn(Optional.of(registeredDevice()));
        when(claimTokenRepository.findActiveByDeviceId(DEVICE_ID)).thenReturn(Optional.of(tokenPair.claimToken()));

        var command = new DeviceProvisioningUseCase.ProvisionWithTokenCommand(
                tokenPair.plainToken(), SERIAL, "model", "0.1.0", "10.0.0.1");

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TOKEN_EXPIRED");
    }

    @Test
    @DisplayName("Revoked device is rejected")
    void revokedDeviceRejected() {
        var tokenPair = ClaimToken.create(DEVICE_ID, TENANT_ID, Duration.ofHours(24), "admin");
        var revokedDevice = Device.reconstitute(DEVICE_ID, SERIAL, TENANT_ID,
                BuildingId.generate(), ElectricalPanelId.generate(),
                "1.0.0", "rev-A", ConnectivityType.WIFI, DeviceStatus.REVOKED,
                null, null, "hash", AuditMetadata.create("admin"));
        when(sessionRepository.countRecentFailedByIp(anyString(), anyInt())).thenReturn(0);
        when(deviceRepository.findBySerialNumber(SERIAL)).thenReturn(Optional.of(revokedDevice));
        when(claimTokenRepository.findActiveByDeviceId(DEVICE_ID)).thenReturn(Optional.of(tokenPair.claimToken()));

        var command = new DeviceProvisioningUseCase.ProvisionWithTokenCommand(
                tokenPair.plainToken(), SERIAL, "model", "0.1.0", "10.0.0.1");

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DEVICE_REVOKED");
    }

    @Test
    @DisplayName("No active token returns error")
    void noActiveTokenReturnsError() {
        when(sessionRepository.countRecentFailedByIp(anyString(), anyInt())).thenReturn(0);
        when(deviceRepository.findBySerialNumber(SERIAL)).thenReturn(Optional.of(registeredDevice()));
        when(claimTokenRepository.findActiveByDeviceId(DEVICE_ID)).thenReturn(Optional.empty());

        var command = new DeviceProvisioningUseCase.ProvisionWithTokenCommand(
                "token", SERIAL, "model", "0.1.0", "10.0.0.1");

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("INVALID_TOKEN");
    }

    @Test
    @DisplayName("Failed provisioning creates session with failure reason")
    void failedProvisioningRecordsSession() {
        when(sessionRepository.countRecentFailedByIp(anyString(), anyInt())).thenReturn(0);
        when(deviceRepository.findBySerialNumber(SERIAL)).thenReturn(Optional.of(registeredDevice()));
        when(claimTokenRepository.findActiveByDeviceId(DEVICE_ID)).thenReturn(Optional.empty());

        var command = new DeviceProvisioningUseCase.ProvisionWithTokenCommand(
                "token", SERIAL, "model", "0.1.0", "10.0.0.1");

        assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessException.class);
        verify(sessionRepository).save(argThat(session ->
                session.getStatus() == DeviceProvisioningSession.SessionStatus.FAILED));
    }
}
