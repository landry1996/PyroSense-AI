package com.pyrosense.device.application.usecase;

import com.pyrosense.device.application.port.in.RotateDeviceCredentialUseCase;
import com.pyrosense.device.application.port.out.DeviceCredentialRepositoryPort;
import com.pyrosense.device.application.port.out.DeviceEventPublisherPort;
import com.pyrosense.device.application.port.out.DeviceRepositoryPort;
import com.pyrosense.device.application.port.out.ProvisioningAuditPort;
import com.pyrosense.device.domain.event.DeviceCredentialRotatedEvent;
import com.pyrosense.device.domain.model.DeviceCredential;
import com.pyrosense.shared.exception.BusinessException;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.DeviceId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RotateDeviceCredentialService implements RotateDeviceCredentialUseCase {

    private final DeviceRepositoryPort deviceRepository;
    private final DeviceCredentialRepositoryPort credentialRepository;
    private final DeviceEventPublisherPort eventPublisher;
    private final ProvisioningAuditPort audit;

    public RotateDeviceCredentialService(DeviceRepositoryPort deviceRepository,
                                          DeviceCredentialRepositoryPort credentialRepository,
                                          DeviceEventPublisherPort eventPublisher,
                                          ProvisioningAuditPort audit) {
        this.deviceRepository = deviceRepository;
        this.credentialRepository = credentialRepository;
        this.eventPublisher = eventPublisher;
        this.audit = audit;
    }

    @Override
    public RotateCredentialResult execute(DeviceId deviceId, String actor) {
        var device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device", deviceId.value().toString()));

        if (device.isRevoked()) {
            throw new BusinessException("DEVICE_REVOKED", "Cannot rotate credentials for revoked device");
        }

        var existingCredential = credentialRepository.findActiveByDeviceId(deviceId);
        existingCredential.ifPresent(cred -> {
            cred.revoke();
            credentialRepository.save(cred);
        });

        int newVersion = credentialRepository.nextVersionForDevice(deviceId);
        var credentialPair = DeviceCredential.issue(deviceId, newVersion);
        credentialRepository.save(credentialPair.credential());

        audit.logCredentialRotated(deviceId.value().toString(), newVersion, actor);

        eventPublisher.publish(List.of(new DeviceCredentialRotatedEvent(
                UUID.randomUUID(), Instant.now(), deviceId, newVersion)));

        return new RotateCredentialResult(credentialPair.plainHmacKey(), newVersion);
    }
}
