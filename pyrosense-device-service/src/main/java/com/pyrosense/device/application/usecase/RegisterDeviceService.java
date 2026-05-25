package com.pyrosense.device.application.usecase;

import com.pyrosense.device.application.port.in.RegisterDeviceUseCase;
import com.pyrosense.device.application.port.out.DeviceEventPublisherPort;
import com.pyrosense.device.application.port.out.DeviceRepositoryPort;
import com.pyrosense.device.application.port.out.EnrollmentKeyGeneratorPort;
import com.pyrosense.device.domain.model.Device;
import com.pyrosense.shared.exception.BusinessException;
import com.pyrosense.shared.exception.ErrorCode;

public class RegisterDeviceService implements RegisterDeviceUseCase {

    private final DeviceRepositoryPort repository;
    private final DeviceEventPublisherPort eventPublisher;
    private final EnrollmentKeyGeneratorPort keyGenerator;

    public RegisterDeviceService(DeviceRepositoryPort repository,
                                  DeviceEventPublisherPort eventPublisher,
                                  EnrollmentKeyGeneratorPort keyGenerator) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.keyGenerator = keyGenerator;
    }

    @Override
    public RegisterDeviceResult execute(RegisterDeviceCommand command, String actor) {
        if (repository.existsBySerialNumber(command.serialNumber())) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Device with serial number '%s' already exists".formatted(command.serialNumber()));
        }

        var keyPair = keyGenerator.generate();

        var device = Device.register(
                command.serialNumber(),
                command.firmwareVersion(),
                command.hardwareRevision(),
                command.connectivityType(),
                keyPair.keyHash(),
                actor
        );

        repository.save(device);
        eventPublisher.publish(device.getDomainEvents());
        device.clearDomainEvents();

        return new RegisterDeviceResult(device.getId(), keyPair.plainKey());
    }
}
