package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadValidatorTest {

    private PayloadValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PayloadValidator();
    }

    private TelemetryPayloadV1 validTelemetry() {
        return new TelemetryPayloadV1(
                "1.0",
                "550e8400-e29b-41d4-a716-446655440000",
                "device-001",
                "tenant-001",
                Instant.now(),
                42L,
                "0.1.0",
                1000,
                false,
                new TelemetryPayloadV1.TelemetryFeatures(
                        14.5, 230.0, 3200.0, 300.0, 0.94,
                        4.7, 39.5, 0.18, 0, 1, 0.97),
                new TelemetryPayloadV1.SecurityBlock(
                        "6839ab0100001069a3f2bc01",
                        "7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069")
        );
    }

    @Nested
    @DisplayName("Telemetry validation")
    class TelemetryValidation {

        @Test
        @DisplayName("Valid telemetry payload passes validation")
        void validPayload() {
            var result = validator.validateTelemetry(validTelemetry(), "device-001", "tenant-001");
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("Unknown schema version is rejected")
        void unknownSchemaVersion() {
            var payload = new TelemetryPayloadV1(
                    "2.0", "msg-1", "device-001", "tenant-001",
                    Instant.now(), 1, "0.1.0", 1000, false,
                    validTelemetry().features(), validTelemetry().security());

            var result = validator.validateTelemetry(payload, "device-001", "tenant-001");
            assertThat(result.valid()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("INVALID_SCHEMA_VERSION");
        }

        @Test
        @DisplayName("DeviceId mismatch between topic and payload is rejected")
        void deviceIdMismatch() {
            var result = validator.validateTelemetry(validTelemetry(), "other-device", "tenant-001");
            assertThat(result.valid()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("TOPIC_PAYLOAD_MISMATCH");
        }

        @Test
        @DisplayName("TenantId mismatch between topic and payload is rejected")
        void tenantIdMismatch() {
            var result = validator.validateTelemetry(validTelemetry(), "device-001", "other-tenant");
            assertThat(result.valid()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("TOPIC_PAYLOAD_MISMATCH");
        }

        @Test
        @DisplayName("Timestamp too far in the future is rejected")
        void timestampFuture() {
            var payload = new TelemetryPayloadV1(
                    "1.0", "msg-1", "device-001", "tenant-001",
                    Instant.now().plusSeconds(600), 1, "0.1.0", 1000, false,
                    validTelemetry().features(), validTelemetry().security());

            var result = validator.validateTelemetry(payload, "device-001", "tenant-001");
            assertThat(result.valid()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("TIMESTAMP_FUTURE");
        }

        @Test
        @DisplayName("Very old timestamp is rejected for non-drain messages")
        void timestampTooOld() {
            var payload = new TelemetryPayloadV1(
                    "1.0", "msg-1", "device-001", "tenant-001",
                    Instant.now().minusSeconds(400), 1, "0.1.0", 1000, false,
                    validTelemetry().features(), validTelemetry().security());

            var result = validator.validateTelemetry(payload, "device-001", "tenant-001");
            assertThat(result.valid()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("TIMESTAMP_TOO_OLD");
        }

        @Test
        @DisplayName("Old timestamp is accepted for drain messages (72h window)")
        void drainTimestampAccepted() {
            var payload = new TelemetryPayloadV1(
                    "1.0", "msg-1", "device-001", "tenant-001",
                    Instant.now().minusSeconds(3600), 1, "0.1.0", 1000, true,
                    validTelemetry().features(), validTelemetry().security());

            var result = validator.validateTelemetry(payload, "device-001", "tenant-001");
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("Current out of range is rejected")
        void currentOutOfRange() {
            var features = new TelemetryPayloadV1.TelemetryFeatures(
                    600.0, 230.0, 3200.0, 300.0, 0.94,
                    4.7, 39.5, 0.18, 0, 1, 0.97);
            var payload = new TelemetryPayloadV1(
                    "1.0", "msg-1", "device-001", "tenant-001",
                    Instant.now(), 1, "0.1.0", 1000, false,
                    features, validTelemetry().security());

            var result = validator.validateTelemetry(payload, "device-001", "tenant-001");
            assertThat(result.valid()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("FIELD_OUT_OF_RANGE");
        }

        @Test
        @DisplayName("Nonce wrong length is rejected")
        void nonceWrongLength() {
            var sec = new TelemetryPayloadV1.SecurityBlock("short", "abcdef1234567890");
            var payload = new TelemetryPayloadV1(
                    "1.0", "msg-1", "device-001", "tenant-001",
                    Instant.now(), 1, "0.1.0", 1000, false,
                    validTelemetry().features(), sec);

            var result = validator.validateTelemetry(payload, "device-001", "tenant-001");
            assertThat(result.valid()).isFalse();
        }
    }

    @Nested
    @DisplayName("Heartbeat validation")
    class HeartbeatValidation {

        @Test
        @DisplayName("Valid heartbeat passes")
        void validHeartbeat() {
            var hb = new HeartbeatPayloadV1(
                    "1.0", "device-001", "tenant-001",
                    Instant.now(), 100, "0.1.0", "ACTIVE",
                    3600, 180000, -42, 5, 0.95, 20, 45);

            var result = validator.validateHeartbeat(hb, "device-001", "tenant-001");
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("Heartbeat with invalid RSSI is rejected")
        void invalidRssi() {
            var hb = new HeartbeatPayloadV1(
                    "1.0", "device-001", "tenant-001",
                    Instant.now(), 100, "0.1.0", "ACTIVE",
                    3600, 180000, 10, 5, 0.95, null, null);

            var result = validator.validateHeartbeat(hb, "device-001", "tenant-001");
            assertThat(result.valid()).isFalse();
        }
    }

    @Nested
    @DisplayName("Device event validation")
    class EventValidation {

        @Test
        @DisplayName("Valid event passes")
        void validEvent() {
            var event = new DeviceEventPayloadV1(
                    "1.0", "msg-1", "device-001", "tenant-001",
                    Instant.now(), 100, "0.1.0",
                    "ARC_DETECTED", "HIGH",
                    java.util.Map.of("arcEnergy", 0.45),
                    new DeviceEventPayloadV1.EventContext(14.8, 38.2, 0.92),
                    new TelemetryPayloadV1.SecurityBlock(
                            "6839ab0200001070b4e3cd02",
                            "a1b2c3d4e5f6"));

            var result = validator.validateDeviceEvent(event, "device-001", "tenant-001");
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("Unknown event type is rejected")
        void unknownEventType() {
            var event = new DeviceEventPayloadV1(
                    "1.0", "msg-1", "device-001", "tenant-001",
                    Instant.now(), 100, "0.1.0",
                    "UNKNOWN_TYPE", "HIGH",
                    java.util.Map.of(), null,
                    new TelemetryPayloadV1.SecurityBlock("6839ab0200001070b4e3cd02", "sig"));

            var result = validator.validateDeviceEvent(event, "device-001", "tenant-001");
            assertThat(result.valid()).isFalse();
        }
    }
}
