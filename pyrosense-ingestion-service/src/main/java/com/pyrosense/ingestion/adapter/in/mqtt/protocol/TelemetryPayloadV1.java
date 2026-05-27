package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import java.time.Instant;

public record TelemetryPayloadV1(
        String schemaVersion,
        String messageId,
        String deviceId,
        String tenantId,
        Instant timestamp,
        long sequenceNumber,
        String firmwareVersion,
        int samplingWindowMs,
        Boolean isDrain,
        TelemetryFeatures features,
        SecurityBlock security
) {
    public record TelemetryFeatures(
            double rmsCurrent,
            double rmsVoltage,
            double activePower,
            double reactivePower,
            double powerFactor,
            double thd,
            double temperatureCelsius,
            double hfNoiseLevel,
            int microArcCount,
            int transientCount,
            double signalQuality
    ) {}

    public record SecurityBlock(
            String nonce,
            String signature
    ) {}
}
