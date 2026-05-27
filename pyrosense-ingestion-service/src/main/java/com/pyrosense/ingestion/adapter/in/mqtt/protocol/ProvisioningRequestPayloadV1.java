package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import java.util.List;

public record ProvisioningRequestPayloadV1(
        String schemaVersion,
        String claimToken,
        String deviceSerial,
        String firmwareVersion,
        DeviceCapabilities capabilities
) {
    public record DeviceCapabilities(
            List<String> sensors,
            List<String> features,
            String payloadVersion,
            int bufferHours,
            boolean compressionSupported
    ) {}
}
