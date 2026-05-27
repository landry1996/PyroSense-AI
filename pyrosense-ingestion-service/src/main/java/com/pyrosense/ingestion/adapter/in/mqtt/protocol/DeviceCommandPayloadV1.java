package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import java.time.Instant;
import java.util.Map;

public record DeviceCommandPayloadV1(
        String schemaVersion,
        String commandId,
        String commandType,
        Instant issuedAt,
        Instant expiresAt,
        Map<String, Object> payload
) {
    public enum CommandType {
        UPDATE_CONFIG, REBOOT, OTA_AVAILABLE, ROTATE_CERT, REVOKE, SET_CALIBRATION
    }
}
