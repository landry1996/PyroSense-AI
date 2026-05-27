package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import java.time.Instant;
import java.util.Map;

public record DeviceEventPayloadV1(
        String schemaVersion,
        String messageId,
        String deviceId,
        String tenantId,
        Instant timestamp,
        long sequenceNumber,
        String firmwareVersion,
        String eventType,
        String severity,
        Map<String, Object> data,
        EventContext context,
        TelemetryPayloadV1.SecurityBlock security
) {
    public record EventContext(
            Double rmsCurrent,
            Double temperatureCelsius,
            Double signalQuality
    ) {}

    public enum EventType {
        ARC_DETECTED, OVERLOAD, OVERHEATING, VOLTAGE_SAG, VOLTAGE_SWELL, PHASE_LOSS
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
