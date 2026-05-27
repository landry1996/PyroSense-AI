package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import java.time.Instant;

public record CommandAckPayloadV1(
        String schemaVersion,
        String commandId,
        String deviceId,
        String tenantId,
        Instant timestamp,
        String status,
        String message
) {
    public enum AckStatus {
        ACCEPTED, COMPLETED, REJECTED, FAILED
    }
}
