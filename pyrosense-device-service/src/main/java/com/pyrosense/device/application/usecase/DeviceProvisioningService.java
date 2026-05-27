package com.pyrosense.device.application.usecase;

import com.pyrosense.device.application.port.in.DeviceProvisioningUseCase;
import com.pyrosense.device.application.port.out.*;
import com.pyrosense.device.domain.event.DeviceProvisionedEvent;
import com.pyrosense.device.domain.model.DeviceCredential;
import com.pyrosense.device.domain.model.DeviceProvisioningSession;
import com.pyrosense.device.domain.model.DeviceStatus;
import com.pyrosense.shared.exception.BusinessException;
import com.pyrosense.shared.id.DeviceId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class DeviceProvisioningService implements DeviceProvisioningUseCase {

    private static final int MAX_ATTEMPTS_PER_IP = 10;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 15;

    private final ClaimTokenRepositoryPort claimTokenRepository;
    private final DeviceRepositoryPort deviceRepository;
    private final DeviceCredentialRepositoryPort credentialRepository;
    private final ProvisioningSessionRepositoryPort sessionRepository;
    private final DeviceEventPublisherPort eventPublisher;
    private final ProvisioningAuditPort audit;
    private final String mqttBrokerUri;
    private final int mqttPort;

    public DeviceProvisioningService(ClaimTokenRepositoryPort claimTokenRepository,
                                      DeviceRepositoryPort deviceRepository,
                                      DeviceCredentialRepositoryPort credentialRepository,
                                      ProvisioningSessionRepositoryPort sessionRepository,
                                      DeviceEventPublisherPort eventPublisher,
                                      ProvisioningAuditPort audit,
                                      String mqttBrokerUri,
                                      int mqttPort) {
        this.claimTokenRepository = claimTokenRepository;
        this.deviceRepository = deviceRepository;
        this.credentialRepository = credentialRepository;
        this.sessionRepository = sessionRepository;
        this.eventPublisher = eventPublisher;
        this.audit = audit;
        this.mqttBrokerUri = mqttBrokerUri;
        this.mqttPort = mqttPort;
    }

    @Override
    public ProvisioningResult execute(ProvisionWithTokenCommand command) {
        int recentFailed = sessionRepository.countRecentFailedByIp(
                command.sourceIp(), RATE_LIMIT_WINDOW_MINUTES);
        if (recentFailed >= MAX_ATTEMPTS_PER_IP) {
            audit.logProvisioningAttempt("unknown", command.sourceIp(), false, "RATE_LIMITED");
            throw new BusinessException("RATE_LIMITED",
                    "Too many provisioning attempts from this IP");
        }

        var device = deviceRepository.findBySerialNumber(command.deviceSerial())
                .orElseThrow(() -> {
                    audit.logProvisioningAttempt(command.deviceSerial(), command.sourceIp(), false, "DEVICE_NOT_FOUND");
                    return new BusinessException("DEVICE_NOT_FOUND", "Device serial not found");
                });

        DeviceId deviceId = device.getId();

        var claimToken = claimTokenRepository.findActiveByDeviceId(deviceId)
                .orElseThrow(() -> {
                    failSession(deviceId, device.getTenantId(), command, "NO_ACTIVE_TOKEN");
                    return new BusinessException("INVALID_TOKEN", "No active claim token for device");
                });

        if (!claimToken.matches(command.claimToken())) {
            failSession(deviceId, device.getTenantId(), command, "TOKEN_MISMATCH");
            throw new BusinessException("INVALID_TOKEN", "Claim token invalid");
        }

        if (claimToken.isExpired()) {
            failSession(deviceId, device.getTenantId(), command, "TOKEN_EXPIRED");
            throw new BusinessException("TOKEN_EXPIRED", "Claim token has expired");
        }

        if (device.isRevoked()) {
            failSession(deviceId, device.getTenantId(), command, "DEVICE_REVOKED");
            throw new BusinessException("DEVICE_REVOKED", "Device has been revoked");
        }

        claimToken.consume();
        claimTokenRepository.save(claimToken);

        int version = credentialRepository.nextVersionForDevice(deviceId);
        var credentialPair = DeviceCredential.issue(deviceId, version);
        credentialRepository.save(credentialPair.credential());

        if (device.getStatus() == DeviceStatus.REGISTERED) {
            device.provision(claimToken.getTenantId(), device.getBuildingId(), device.getPanelId(), "system-provisioning");
            deviceRepository.save(device);
        }

        var session = DeviceProvisioningSession.create(
                deviceId, claimToken.getTenantId(),
                command.deviceSerial(), command.deviceModel(),
                command.firmwareVersion(), command.sourceIp());
        session.markCompleted();
        sessionRepository.save(session);

        audit.logProvisioningAttempt(deviceId.value().toString(), command.sourceIp(), true, null);

        String topicPrefix = "pyrosense/v1/" + claimToken.getTenantId().value() + "/" + deviceId.value();

        eventPublisher.publish(List.of(new DeviceProvisionedEvent(
                UUID.randomUUID(), Instant.now(), deviceId,
                claimToken.getTenantId(), device.getBuildingId(), device.getPanelId())));

        return new ProvisioningResult(
                deviceId.value().toString(),
                claimToken.getTenantId().value().toString(),
                credentialPair.plainHmacKey(),
                mqttBrokerUri,
                mqttPort,
                topicPrefix
        );
    }

    private void failSession(DeviceId deviceId, com.pyrosense.shared.id.TenantId tenantId,
                             ProvisionWithTokenCommand command, String reason) {
        var session = DeviceProvisioningSession.create(
                deviceId, tenantId != null ? tenantId : new com.pyrosense.shared.id.TenantId(UUID.randomUUID()),
                command.deviceSerial(), command.deviceModel(),
                command.firmwareVersion(), command.sourceIp());
        session.markFailed(reason);
        sessionRepository.save(session);
        audit.logProvisioningAttempt(deviceId.value().toString(), command.sourceIp(), false, reason);
    }
}
