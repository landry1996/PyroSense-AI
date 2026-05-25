package com.pyrosense.device.application.usecase;

import com.pyrosense.device.application.port.in.RevokeDeviceUseCase;
import com.pyrosense.device.application.port.out.DeviceEventPublisherPort;
import com.pyrosense.device.application.port.out.DeviceRepositoryPort;
import com.pyrosense.shared.exception.NotFoundException;

public class RevokeDeviceService implements RevokeDeviceUseCase {

    private final DeviceRepositoryPort repository;
    private final DeviceEventPublisherPort eventPublisher;

    public RevokeDeviceService(DeviceRepositoryPort repository,
                                DeviceEventPublisherPort eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute(RevokeDeviceCommand command, String actor) {
        var device = repository.findById(command.deviceId())
                .orElseThrow(() -> new NotFoundException("Device", command.deviceId().toString()));

        device.revoke(command.reason(), actor);

        repository.save(device);
        eventPublisher.publish(device.getDomainEvents());
        device.clearDomainEvents();
    }
}
