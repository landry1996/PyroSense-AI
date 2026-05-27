package com.pyrosense.ingestion.application.port.out;

import java.util.Optional;

public interface DeviceCapabilityLookupPort {

    record DeviceCapability(
            String deviceId,
            String tenantId,
            String deviceModel,
            String firmwareVersion,
            boolean supportsSignature,
            boolean supportsAntiReplay,
            int maxSamplingRateMs
    ) {}

    Optional<DeviceCapability> getCapability(String deviceId);

    boolean isRealDevice(String deviceId);
}
