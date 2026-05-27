package com.pyrosense.device.application.usecase;

import com.pyrosense.device.application.port.in.RevokeDeviceCredentialUseCase;
import com.pyrosense.device.application.port.out.*;
import com.pyrosense.device.domain.event.DeviceRevokedEvent;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.DeviceId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RevokeDeviceCredentialService implements RevokeDeviceCredentialUseCase {

    private final DeviceRepositoryPort deviceRepository;
    private final DeviceCredentialRepositoryPort credentialRepository;
    private final ClaimTokenRepositoryPort claimTokenRepository;
    private final DeviceEventPublisherPort eventPublisher;
    private final ProvisioningAuditPort audit;

    public RevokeDeviceCredentialService(DeviceRepositoryPort deviceRepository,
                                          DeviceCredentialRepositoryPort credentialRepository,
                                          ClaimTokenRepositoryPort claimTokenRepository,
                                          DeviceEventPublisherPort eventPublisher,
                                          ProvisioningAuditPort audit) {
        this.deviceRepository = deviceRepository;
        this.credentialRepository = credentialRepository;
        this.claimTokenRepository = claimTokenRepository;
        this.eventPublisher = eventPublisher;
        this.audit = audit;
    }

    @Override
    public void execute(DeviceId deviceId, String reason, String actor) {
        var device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device", deviceId.value().toString()));

        var credentials = credentialRepository.findAllByDeviceId(deviceId);
        for (var cred : credentials) {
            if (cred.isActive()) {
                cred.revoke();
                credentialRepository.save(cred);
            }
        }

        claimTokenRepository.invalidateAllForDevice(deviceId);

        device.revoke(reason, actor);
        deviceRepository.save(device);

        audit.logDeviceRevoked(deviceId.value().toString(), reason, actor);

        eventPublisher.publish(List.of(new DeviceRevokedEvent(
                UUID.randomUUID(), Instant.now(), deviceId, reason)));
    }
}
