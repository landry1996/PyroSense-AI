package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolValidationPipelineTest {

    private ProtocolValidationPipeline pipeline;
    private FakeDeviceStatusChecker deviceChecker;

    @BeforeEach
    void setUp() {
        var validator = new PayloadValidator();
        var signatureVerifier = new SignatureVerifier();
        var nonceStore = new AntiReplayGuardTest.InMemoryNonceStore();
        var sequenceStore = new AntiReplayGuardTest.InMemorySequenceStore();
        var messageIdStore = new AntiReplayGuardTest.InMemoryMessageIdStore();
        var antiReplay = new AntiReplayGuard(nonceStore, sequenceStore, messageIdStore);
        deviceChecker = new FakeDeviceStatusChecker();
        deviceChecker.registerDevice("device-001", "tenant-001");

        pipeline = new ProtocolValidationPipeline(validator, signatureVerifier, antiReplay, deviceChecker);
    }

    private TelemetryPayloadV1 validPayload(long sequence, String nonce) {
        return new TelemetryPayloadV1(
                "1.0",
                "msg-" + sequence,
                "device-001",
                "tenant-001",
                Instant.now(),
                sequence,
                "0.1.0",
                1000,
                false,
                new TelemetryPayloadV1.TelemetryFeatures(
                        14.5, 230.0, 3200.0, 300.0, 0.94,
                        4.7, 39.5, 0.18, 0, 1, 0.97),
                new TelemetryPayloadV1.SecurityBlock(nonce, "disabled")
        );
    }

    @Test
    @DisplayName("Valid telemetry is accepted")
    void validTelemetryAccepted() {
        var payload = validPayload(1, "6839ab0100001069a3f2bc01");
        var result = pipeline.validateTelemetry(payload, "{}", "device-001", "tenant-001");
        assertThat(result.accepted()).isTrue();
    }

    @Test
    @DisplayName("Revoked device is rejected")
    void revokedDeviceRejected() {
        deviceChecker.revokeDevice("device-001");
        var payload = validPayload(1, "6839ab0100001069a3f2bc01");
        var result = pipeline.validateTelemetry(payload, "{}", "device-001", "tenant-001");
        assertThat(result.accepted()).isFalse();
        assertThat(result.rejectionCode()).isEqualTo("DEVICE_REVOKED");
    }

    @Test
    @DisplayName("Unknown device is rejected")
    void unknownDeviceRejected() {
        var payload = new TelemetryPayloadV1(
                "1.0", "msg-1", "unknown-device", "tenant-001",
                Instant.now(), 1, "0.1.0", 1000, false,
                validPayload(1, "x").features(),
                new TelemetryPayloadV1.SecurityBlock("6839ab0100001069a3f2bc01", "disabled"));
        var result = pipeline.validateTelemetry(payload, "{}", "unknown-device", "tenant-001");
        assertThat(result.accepted()).isFalse();
        assertThat(result.rejectionCode()).isEqualTo("DEVICE_NOT_FOUND");
    }

    @Test
    @DisplayName("Replay attack (duplicate nonce) is rejected")
    void replayAttackRejected() {
        var p1 = validPayload(1, "6839ab0100001069a3f2bc01");
        pipeline.validateTelemetry(p1, "{}", "device-001", "tenant-001");

        var p2 = validPayload(2, "6839ab0100001069a3f2bc01"); // same nonce
        var result = pipeline.validateTelemetry(p2, "{}", "device-001", "tenant-001");
        assertThat(result.accepted()).isFalse();
        assertThat(result.rejectionCode()).isEqualTo("REPLAY_DETECTED");
    }

    @Test
    @DisplayName("Duplicate message (same messageId) is rejected")
    void duplicateMessageRejected() {
        var p1 = new TelemetryPayloadV1(
                "1.0", "same-msg-id", "device-001", "tenant-001",
                Instant.now(), 1, "0.1.0", 1000, false,
                validPayload(1, "x").features(),
                new TelemetryPayloadV1.SecurityBlock("aaaaaa0100001069a3f2bc01", "disabled"));
        pipeline.validateTelemetry(p1, "{}", "device-001", "tenant-001");

        var p2 = new TelemetryPayloadV1(
                "1.0", "same-msg-id", "device-001", "tenant-001",
                Instant.now(), 2, "0.1.0", 1000, false,
                validPayload(2, "x").features(),
                new TelemetryPayloadV1.SecurityBlock("bbbbbb0100001069a3f2bc02", "disabled"));
        var result = pipeline.validateTelemetry(p2, "{}", "device-001", "tenant-001");
        assertThat(result.accepted()).isFalse();
        assertThat(result.rejectionCode()).isEqualTo("DUPLICATE_MESSAGE");
    }

    @Test
    @DisplayName("Sequence regression is rejected")
    void sequenceRegressionRejected() {
        var p1 = validPayload(10, "aaaaaa0100001069a3f2bc01");
        pipeline.validateTelemetry(p1, "{}", "device-001", "tenant-001");

        var p2 = validPayload(5, "bbbbbb0100001069a3f2bc02");
        var result = pipeline.validateTelemetry(p2, "{}", "device-001", "tenant-001");
        assertThat(result.accepted()).isFalse();
        assertThat(result.rejectionCode()).isEqualTo("SEQUENCE_REGRESSION");
    }

    @Test
    @DisplayName("Old timestamp is rejected")
    void oldTimestampRejected() {
        var payload = new TelemetryPayloadV1(
                "1.0", "msg-1", "device-001", "tenant-001",
                Instant.now().minusSeconds(400), 1, "0.1.0", 1000, false,
                validPayload(1, "x").features(),
                new TelemetryPayloadV1.SecurityBlock("6839ab0100001069a3f2bc01", "disabled"));
        var result = pipeline.validateTelemetry(payload, "{}", "device-001", "tenant-001");
        assertThat(result.accepted()).isFalse();
        assertThat(result.rejectionCode()).isEqualTo("TIMESTAMP_TOO_OLD");
    }

    @Test
    @DisplayName("Firmware version is tracked")
    void firmwareVersionTracked() {
        var payload = validPayload(1, "6839ab0100001069a3f2bc01");
        pipeline.validateTelemetry(payload, "{}", "device-001", "tenant-001");
        assertThat(deviceChecker.getLastFirmwareVersion("device-001")).isEqualTo("0.1.0");
    }

    // --- Fake device status checker ---

    static class FakeDeviceStatusChecker implements ProtocolValidationPipeline.DeviceStatusChecker {
        private final Set<String> knownDevices = new HashSet<>();
        private final Set<String> revokedDevices = new HashSet<>();
        private final Map<String, String> firmwareVersions = new HashMap<>();

        void registerDevice(String deviceId, String tenantId) {
            knownDevices.add(deviceId + ":" + tenantId);
        }

        void revokeDevice(String deviceId) {
            revokedDevices.add(deviceId);
        }

        String getLastFirmwareVersion(String deviceId) {
            return firmwareVersions.get(deviceId);
        }

        @Override
        public Status checkDevice(String deviceId, String tenantId) {
            if (revokedDevices.contains(deviceId)) return Status.REVOKED;
            if (knownDevices.contains(deviceId + ":" + tenantId)) return Status.ACTIVE;
            return Status.NOT_FOUND;
        }

        @Override
        public byte[] getHmacKey(String deviceId) {
            return null; // disabled for tests
        }

        @Override
        public void recordFirmwareVersion(String deviceId, String firmwareVersion) {
            firmwareVersions.put(deviceId, firmwareVersion);
        }
    }
}
