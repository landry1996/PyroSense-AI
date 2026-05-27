package com.pyrosense.device.config;

import com.pyrosense.device.application.port.out.*;
import com.pyrosense.device.application.usecase.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

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

    @Bean
    public GetDeviceStatisticsService getDeviceStatisticsService(DeviceRepositoryPort repository) {
        return new GetDeviceStatisticsService(repository);
    }

    @Bean
    public GetBuildingService getBuildingService(DeviceRepositoryPort repository) {
        return new GetBuildingService(repository);
    }

    @Bean
    public CreateClaimTokenService createClaimTokenService(
            DeviceRepositoryPort deviceRepository,
            ClaimTokenRepositoryPort claimTokenRepository,
            DeviceEventPublisherPort eventPublisher,
            ProvisioningAuditPort audit,
            @Value("${pyrosense.provisioning.claim-token-validity:PT24H}") String validity) {
        return new CreateClaimTokenService(deviceRepository, claimTokenRepository,
                eventPublisher, audit, Duration.parse(validity));
    }

    @Bean
    public DeviceProvisioningService deviceProvisioningService(
            ClaimTokenRepositoryPort claimTokenRepository,
            DeviceRepositoryPort deviceRepository,
            DeviceCredentialRepositoryPort credentialRepository,
            ProvisioningSessionRepositoryPort sessionRepository,
            DeviceEventPublisherPort eventPublisher,
            ProvisioningAuditPort audit,
            @Value("${pyrosense.mqtt.broker-uri:tcp://localhost}") String mqttBrokerUri,
            @Value("${pyrosense.mqtt.port:1883}") int mqttPort) {
        return new DeviceProvisioningService(claimTokenRepository, deviceRepository,
                credentialRepository, sessionRepository, eventPublisher, audit,
                mqttBrokerUri, mqttPort);
    }

    @Bean
    public RotateDeviceCredentialService rotateDeviceCredentialService(
            DeviceRepositoryPort deviceRepository,
            DeviceCredentialRepositoryPort credentialRepository,
            DeviceEventPublisherPort eventPublisher,
            ProvisioningAuditPort audit) {
        return new RotateDeviceCredentialService(deviceRepository, credentialRepository,
                eventPublisher, audit);
    }

    @Bean
    public RevokeDeviceCredentialService revokeDeviceCredentialService(
            DeviceRepositoryPort deviceRepository,
            DeviceCredentialRepositoryPort credentialRepository,
            ClaimTokenRepositoryPort claimTokenRepository,
            DeviceEventPublisherPort eventPublisher,
            ProvisioningAuditPort audit) {
        return new RevokeDeviceCredentialService(deviceRepository, credentialRepository,
                claimTokenRepository, eventPublisher, audit);
    }
}
