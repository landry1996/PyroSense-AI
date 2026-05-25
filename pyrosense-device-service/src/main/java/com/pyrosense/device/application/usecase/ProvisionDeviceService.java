package com.pyrosense.device.application.usecase;

import com.pyrosense.device.application.port.in.ProvisionDeviceUseCase;
import com.pyrosense.device.application.port.out.DeviceEventPublisherPort;
import com.pyrosense.device.application.port.out.DeviceRepositoryPort;
import com.pyrosense.shared.exception.NotFoundException;

public class ProvisionDeviceService implements ProvisionDeviceUseCase {

    private final DeviceRepositoryPort repository;
    private final DeviceEventPublisherPort eventPublisher;

    public ProvisionDeviceService(DeviceRepositoryPort repository,
                                   DeviceEventPublisherPort eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute(ProvisionDeviceCommand command, String actor) {
        var device = repository.findById(command.deviceId())
                .orElseThrow(() -> new NotFoundException("Device", command.deviceId().toString()));

        device.provision(command.tenantId(), command.buildingId(), command.panelId(), actor);

        repository.save(device);
        eventPublisher.publish(device.getDomainEvents());
        device.clearDomainEvents();
    }
}
