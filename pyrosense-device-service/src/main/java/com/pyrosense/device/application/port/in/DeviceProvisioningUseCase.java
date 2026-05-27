package com.pyrosense.device.application.port.in;

public interface DeviceProvisioningUseCase {

    record ProvisionWithTokenCommand(
            String claimToken,
            String deviceSerial,
            String deviceModel,
            String firmwareVersion,
            String sourceIp
    ) {}

    record ProvisioningResult(
            String deviceId,
            String tenantId,
            String hmacKey,
            String mqttBrokerUri,
            int mqttPort,
            String topicPrefix
    ) {}

    ProvisioningResult execute(ProvisionWithTokenCommand command);
}
