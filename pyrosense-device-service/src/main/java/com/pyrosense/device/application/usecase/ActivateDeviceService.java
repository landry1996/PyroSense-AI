package com.pyrosense.device.application.usecase;

import com.pyrosense.device.application.port.in.ActivateDeviceUseCase;
import com.pyrosense.device.application.port.out.DeviceEventPublisherPort;
import com.pyrosense.device.application.port.out.DeviceRepositoryPort;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.DeviceId;

public class ActivateDeviceService implements ActivateDeviceUseCase {

    private final DeviceRepositoryPort repository;
    private final DeviceEventPublisherPort eventPublisher;

    public ActivateDeviceService(DeviceRepositoryPort repository,
                                  DeviceEventPublisherPort eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute(DeviceId deviceId, String actor) {
        var device = repository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device", deviceId.toString()));

        device.activate(actor);

        repository.save(device);
        eventPublisher.publish(device.getDomainEvents());
        device.clearDomainEvents();
    }
}
