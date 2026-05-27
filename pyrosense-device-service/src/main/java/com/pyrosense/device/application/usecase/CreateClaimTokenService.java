package com.pyrosense.device.application.usecase;

import com.pyrosense.device.application.port.in.CreateClaimTokenUseCase;
import com.pyrosense.device.application.port.out.ClaimTokenRepositoryPort;
import com.pyrosense.device.application.port.out.DeviceEventPublisherPort;
import com.pyrosense.device.application.port.out.DeviceRepositoryPort;
import com.pyrosense.device.application.port.out.ProvisioningAuditPort;
import com.pyrosense.device.domain.event.DeviceClaimTokenCreatedEvent;
import com.pyrosense.device.domain.model.ClaimToken;
import com.pyrosense.device.domain.model.DeviceStatus;
import com.pyrosense.shared.exception.BusinessException;
import com.pyrosense.shared.exception.NotFoundException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CreateClaimTokenService implements CreateClaimTokenUseCase {

    private final DeviceRepositoryPort deviceRepository;
    private final ClaimTokenRepositoryPort claimTokenRepository;
    private final DeviceEventPublisherPort eventPublisher;
    private final ProvisioningAuditPort audit;
    private final Duration tokenValidity;

    public CreateClaimTokenService(DeviceRepositoryPort deviceRepository,
                                    ClaimTokenRepositoryPort claimTokenRepository,
                                    DeviceEventPublisherPort eventPublisher,
                                    ProvisioningAuditPort audit,
                                    Duration tokenValidity) {
        this.deviceRepository = deviceRepository;
        this.claimTokenRepository = claimTokenRepository;
        this.eventPublisher = eventPublisher;
        this.audit = audit;
        this.tokenValidity = tokenValidity;
    }

    @Override
    public ClaimTokenResult execute(CreateClaimTokenCommand command, String actor) {
        var device = deviceRepository.findById(command.deviceId())
                .orElseThrow(() -> new NotFoundException("Device", command.deviceId().value().toString()));

        if (device.getStatus() == DeviceStatus.REVOKED) {
            throw new BusinessException("DEVICE_REVOKED", "Cannot create claim token for revoked device");
        }
        if (device.getStatus() != DeviceStatus.REGISTERED && device.getStatus() != DeviceStatus.PROVISIONED) {
            throw new BusinessException("INVALID_STATE",
                    "Device must be in REGISTERED or PROVISIONED state to create claim token");
        }

        claimTokenRepository.invalidateAllForDevice(command.deviceId());

        var tokenPair = ClaimToken.create(command.deviceId(), command.tenantId(), tokenValidity, actor);
        claimTokenRepository.save(tokenPair.claimToken());

        audit.logClaimTokenCreated(
                command.deviceId().value().toString(),
                command.tenantId().value().toString(),
                actor);

        eventPublisher.publish(List.of(new DeviceClaimTokenCreatedEvent(
                UUID.randomUUID(), Instant.now(),
                command.deviceId(), command.tenantId(),
                tokenPair.claimToken().getExpiresAt())));

        return new ClaimTokenResult(tokenPair.plainToken(), tokenPair.claimToken().getExpiresAt());
    }
}
