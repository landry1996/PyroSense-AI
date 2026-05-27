package com.pyrosense.ingestion.domain.model;

public enum IngestionRejectionReason {

    INVALID_SIGNATURE("HMAC-SHA256 signature verification failed"),
    REPLAY_DETECTED("Replay attack detected (nonce reuse or duplicate message)"),
    DEVICE_REVOKED("Device has been revoked"),
    UNKNOWN_DEVICE("Device not registered in the system"),
    INVALID_SCHEMA_VERSION("Unsupported protocol schema version"),
    PAYLOAD_TOO_OLD("Timestamp exceeds maximum age threshold"),
    INVALID_MEASUREMENT_RANGE("Measurement values outside acceptable range"),
    LOW_SIGNAL_QUALITY("Signal quality below minimum threshold");

    private final String description;

    IngestionRejectionReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static IngestionRejectionReason fromPipelineCode(String code) {
        return switch (code) {
            case "INVALID_SIGNATURE" -> INVALID_SIGNATURE;
            case "REPLAY_DETECTED", "NONCE_REUSED", "DUPLICATE_MESSAGE", "SEQUENCE_REGRESSION" -> REPLAY_DETECTED;
            case "DEVICE_REVOKED" -> DEVICE_REVOKED;
            case "DEVICE_NOT_FOUND" -> UNKNOWN_DEVICE;
            case "INVALID_SCHEMA_VERSION" -> INVALID_SCHEMA_VERSION;
            case "TIMESTAMP_TOO_OLD", "TIMESTAMP_FUTURE" -> PAYLOAD_TOO_OLD;
            case "FIELD_OUT_OF_RANGE", "TOPIC_PAYLOAD_MISMATCH" -> INVALID_MEASUREMENT_RANGE;
            default -> INVALID_MEASUREMENT_RANGE;
        };
    }
}
