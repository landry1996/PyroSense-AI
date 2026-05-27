package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PayloadValidator {

    public record ValidationResult(boolean valid, List<String> errors, String rejectionCode) {
        public static ValidationResult ok() {
            return new ValidationResult(true, List.of(), null);
        }

        public static ValidationResult rejected(String code, String error) {
            return new ValidationResult(false, List.of(error), code);
        }

        public static ValidationResult rejected(String code, List<String> errors) {
            return new ValidationResult(false, errors, code);
        }
    }

    public ValidationResult validateTelemetry(TelemetryPayloadV1 payload,
                                               String topicDeviceId,
                                               String topicTenantId) {
        List<String> errors = new ArrayList<>();

        // Schema version
        if (payload.schemaVersion() == null ||
            !MqttProtocolConstants.SUPPORTED_SCHEMA_VERSIONS.contains(payload.schemaVersion())) {
            return ValidationResult.rejected("INVALID_SCHEMA_VERSION",
                    "Unsupported schema version: " + payload.schemaVersion());
        }

        // Topic/payload coherence
        if (!topicDeviceId.equals(payload.deviceId())) {
            return ValidationResult.rejected("TOPIC_PAYLOAD_MISMATCH",
                    "deviceId in payload does not match topic");
        }
        if (!topicTenantId.equals(payload.tenantId())) {
            return ValidationResult.rejected("TOPIC_PAYLOAD_MISMATCH",
                    "tenantId in payload does not match topic");
        }

        // Required fields
        if (payload.messageId() == null || payload.messageId().isBlank()) {
            errors.add("messageId is required");
        }
        if (payload.timestamp() == null) {
            errors.add("timestamp is required");
        }
        if (payload.firmwareVersion() == null || payload.firmwareVersion().isBlank()) {
            errors.add("firmwareVersion is required");
        }
        if (payload.features() == null) {
            errors.add("features block is required");
        }
        if (payload.security() == null) {
            errors.add("security block is required");
        }

        if (!errors.isEmpty()) {
            return ValidationResult.rejected("FIELD_OUT_OF_RANGE", errors);
        }

        // Timestamp validation
        String tsError = validateTimestamp(payload.timestamp(),
                Boolean.TRUE.equals(payload.isDrain()));
        if (tsError != null) {
            return ValidationResult.rejected(tsError.startsWith("TIMESTAMP_FUTURE") ?
                    "TIMESTAMP_FUTURE" : "TIMESTAMP_TOO_OLD", tsError);
        }

        // Sequence number
        if (payload.sequenceNumber() < 0) {
            errors.add("sequenceNumber must be >= 0");
        }

        // Sampling window
        if (payload.samplingWindowMs() < 1 || payload.samplingWindowMs() > 60000) {
            errors.add("samplingWindowMs must be 1-60000");
        }

        // Feature ranges
        var f = payload.features();
        if (f.rmsCurrent() < 0 || f.rmsCurrent() > 500) errors.add("rmsCurrent out of range [0,500]");
        if (f.rmsVoltage() < 0 || f.rmsVoltage() > 1000) errors.add("rmsVoltage out of range [0,1000]");
        if (f.activePower() < 0 || f.activePower() > 500000) errors.add("activePower out of range [0,500000]");
        if (f.reactivePower() < 0 || f.reactivePower() > 500000) errors.add("reactivePower out of range [0,500000]");
        if (f.powerFactor() < 0 || f.powerFactor() > 1.0) errors.add("powerFactor out of range [0,1]");
        if (f.thd() < 0 || f.thd() > 100) errors.add("thd out of range [0,100]");
        if (f.temperatureCelsius() < -40 || f.temperatureCelsius() > 200) errors.add("temperature out of range [-40,200]");
        if (f.hfNoiseLevel() < 0 || f.hfNoiseLevel() > 1.0) errors.add("hfNoiseLevel out of range [0,1]");
        if (f.microArcCount() < 0 || f.microArcCount() > 65535) errors.add("microArcCount out of range [0,65535]");
        if (f.transientCount() < 0 || f.transientCount() > 65535) errors.add("transientCount out of range [0,65535]");
        if (f.signalQuality() < 0 || f.signalQuality() > 1.0) errors.add("signalQuality out of range [0,1]");

        // Security block
        var sec = payload.security();
        if (sec.nonce() == null || sec.nonce().length() != MqttProtocolConstants.NONCE_LENGTH) {
            errors.add("nonce must be " + MqttProtocolConstants.NONCE_LENGTH + " hex chars");
        }
        if (sec.signature() == null || sec.signature().isBlank()) {
            errors.add("signature is required");
        }

        if (!errors.isEmpty()) {
            return ValidationResult.rejected("FIELD_OUT_OF_RANGE", errors);
        }

        return ValidationResult.ok();
    }

    public ValidationResult validateHeartbeat(HeartbeatPayloadV1 payload,
                                               String topicDeviceId,
                                               String topicTenantId) {
        if (payload.schemaVersion() == null ||
            !MqttProtocolConstants.SUPPORTED_SCHEMA_VERSIONS.contains(payload.schemaVersion())) {
            return ValidationResult.rejected("INVALID_SCHEMA_VERSION",
                    "Unsupported schema version: " + payload.schemaVersion());
        }

        if (!topicDeviceId.equals(payload.deviceId())) {
            return ValidationResult.rejected("TOPIC_PAYLOAD_MISMATCH", "deviceId mismatch");
        }
        if (!topicTenantId.equals(payload.tenantId())) {
            return ValidationResult.rejected("TOPIC_PAYLOAD_MISMATCH", "tenantId mismatch");
        }

        List<String> errors = new ArrayList<>();
        if (payload.timestamp() == null) errors.add("timestamp required");
        if (payload.firmwareVersion() == null) errors.add("firmwareVersion required");
        if (payload.state() == null) errors.add("state required");
        if (payload.wifiRssiDbm() > 0 || payload.wifiRssiDbm() < -120) errors.add("wifiRssiDbm out of range");
        if (payload.bufferUsagePercent() < 0 || payload.bufferUsagePercent() > 100) errors.add("bufferUsagePercent out of range");
        if (payload.signalQuality() < 0 || payload.signalQuality() > 1.0) errors.add("signalQuality out of range");

        if (!errors.isEmpty()) {
            return ValidationResult.rejected("FIELD_OUT_OF_RANGE", errors);
        }
        return ValidationResult.ok();
    }

    public ValidationResult validateDeviceEvent(DeviceEventPayloadV1 payload,
                                                 String topicDeviceId,
                                                 String topicTenantId) {
        if (payload.schemaVersion() == null ||
            !MqttProtocolConstants.SUPPORTED_SCHEMA_VERSIONS.contains(payload.schemaVersion())) {
            return ValidationResult.rejected("INVALID_SCHEMA_VERSION",
                    "Unsupported schema version: " + payload.schemaVersion());
        }

        if (!topicDeviceId.equals(payload.deviceId())) {
            return ValidationResult.rejected("TOPIC_PAYLOAD_MISMATCH", "deviceId mismatch");
        }
        if (!topicTenantId.equals(payload.tenantId())) {
            return ValidationResult.rejected("TOPIC_PAYLOAD_MISMATCH", "tenantId mismatch");
        }

        List<String> errors = new ArrayList<>();
        if (payload.messageId() == null) errors.add("messageId required");
        if (payload.timestamp() == null) errors.add("timestamp required");
        if (payload.eventType() == null) errors.add("eventType required");
        if (payload.severity() == null) errors.add("severity required");
        if (payload.security() == null) errors.add("security block required");

        try {
            DeviceEventPayloadV1.EventType.valueOf(payload.eventType());
        } catch (IllegalArgumentException e) {
            errors.add("Unknown eventType: " + payload.eventType());
        }

        try {
            DeviceEventPayloadV1.Severity.valueOf(payload.severity());
        } catch (IllegalArgumentException e) {
            errors.add("Unknown severity: " + payload.severity());
        }

        if (!errors.isEmpty()) {
            return ValidationResult.rejected("FIELD_OUT_OF_RANGE", errors);
        }
        return ValidationResult.ok();
    }

    private String validateTimestamp(Instant timestamp, boolean isDrain) {
        Instant now = Instant.now();
        Duration futureOffset = Duration.between(now, timestamp);
        Duration pastOffset = Duration.between(timestamp, now);

        if (futureOffset.getSeconds() > MqttProtocolConstants.MAX_TIMESTAMP_FUTURE_SECONDS) {
            return "TIMESTAMP_FUTURE: " + futureOffset.getSeconds() + "s in the future";
        }

        int maxAge = isDrain ? MqttProtocolConstants.MAX_TIMESTAMP_AGE_SECONDS :
                MqttProtocolConstants.MAX_REALTIME_TIMESTAMP_AGE_SECONDS;
        if (pastOffset.getSeconds() > maxAge) {
            return "TIMESTAMP_TOO_OLD: " + pastOffset.getSeconds() + "s old (max " + maxAge + "s)";
        }

        return null;
    }
}
