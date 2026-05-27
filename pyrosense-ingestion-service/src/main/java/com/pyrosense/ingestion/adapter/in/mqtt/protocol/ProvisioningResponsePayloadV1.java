package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

public record ProvisioningResponsePayloadV1(
        String schemaVersion,
        String status,
        String deviceId,
        String tenantId,
        MqttConfig mqttConfig,
        TelemetryConfig telemetryConfig,
        SecurityConfig securityConfig
) {
    public record MqttConfig(
            String brokerUri,
            int port,
            int keepaliveSec
    ) {}

    public record TelemetryConfig(
            int featureIntervalMs,
            int heartbeatIntervalMs,
            int healthIntervalMs
    ) {}

    public record SecurityConfig(
            String hmacKeyHex,
            boolean signatureRequired
    ) {}
}
