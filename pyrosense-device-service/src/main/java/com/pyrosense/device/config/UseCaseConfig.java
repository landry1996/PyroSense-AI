package com.pyrosense.device.config;

import com.pyrosense.device.application.port.out.DeviceEventPublisherPort;
import com.pyrosense.device.application.port.out.DeviceRepositoryPort;
import com.pyrosense.device.application.port.out.EnrollmentKeyGeneratorPort;
import com.pyrosense.device.application.usecase.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public RegisterDeviceService registerDeviceService(
            DeviceRepositoryPort repository,
            DeviceEventPublisherPort eventPublisher,
            EnrollmentKeyGeneratorPort keyGenerator) {
        return new RegisterDeviceService(repository, eventPublisher, keyGenerator);
    }

    @Bean
    public ProvisionDeviceService provisionDeviceService(
            DeviceRepositoryPort repository,
            DeviceEventPublisherPort eventPublisher) {
        return new ProvisionDeviceService(repository, eventPublisher);
    }

    @Bean
    public ActivateDeviceService activateDeviceService(
            DeviceRepositoryPort repository,
            DeviceEventPublisherPort eventPublisher) {
        return new ActivateDeviceService(repository, eventPublisher);
    }

    @Bean
    public RevokeDeviceService revokeDeviceService(
            DeviceRepositoryPort repository,
            DeviceEventPublisherPort eventPublisher) {
        return new RevokeDeviceService(repository, eventPublisher);
    }

    @Bean
    public GetDeviceService getDeviceService(DeviceRepositoryPort repository) {
        return new GetDeviceService(repository);
    }

    @Bean
    public RecordHeartbeatService recordHeartbeatService(DeviceRepositoryPort repository) {
        return new RecordHeartbeatService(repository);
    }
}
