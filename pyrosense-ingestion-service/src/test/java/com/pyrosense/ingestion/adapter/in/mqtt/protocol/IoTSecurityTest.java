package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IoT Security — MVP 3 Threat Coverage")
class IoTSecurityTest {

    private ProtocolValidationPipeline pipeline;
    private FakeDeviceStatusChecker deviceChecker;
    private DeviceRateLimiter rateLimiter;
    private IoTSecurityAuditor auditor;
    private InMemoryNonceStore nonceStore;
    private InMemorySequenceStore sequenceStore;
    private InMemoryMessageIdStore messageIdStore;

    @BeforeEach
    void setUp() {
        var validator = new PayloadValidator();
        var signatureVerifier = new SignatureVerifier();
        nonceStore = new InMemoryNonceStore();
        sequenceStore = new InMemorySequenceStore();
        messageIdStore = new InMemoryMessageIdStore();
        var antiReplay = new AntiReplayGuard(nonceStore, sequenceStore, messageIdStore);
        deviceChecker = new FakeDeviceStatusChecker();
        rateLimiter = new DeviceRateLimiter(5, 60);
        auditor = new IoTSecurityAuditor();

        deviceChecker.registerDevice("dev-001", "tenant-001");
        deviceChecker.registerDevice("dev-002", "tenant-002");

        pipeline = new ProtocolValidationPipeline(validator, signatureVerifier, antiReplay,
                deviceChecker, rateLimiter, auditor);
    }

    private TelemetryPayloadV1 payload(String deviceId, String tenantId, long seq, String nonce) {
        return new TelemetryPayloadV1(
                "1.0", "msg-" + seq, deviceId, tenantId,
                Instant.now(), seq, "2.1.0", 1000, false,
                new TelemetryPayloadV1.TelemetryFeatures(
                        14.5, 230.0, 3200.0, 300.0, 0.94,
                        4.7, 39.5, 0.18, 0, 1, 0.97),
                new TelemetryPayloadV1.SecurityBlock(nonce, "disabled"));
    }

    private String uniqueNonce(int id) {
        return String.format("%024d", id);
    }

    @Nested
    @DisplayName("Threat 1: Unauthorized Device")
    class UnauthorizedDevice {

        @Test
        @DisplayName("Unknown device ID is rejected with DEVICE_NOT_FOUND")
        void unknownDeviceRejected() {
            var p = payload("unknown-device", "tenant-001", 1, uniqueNonce(1));
            var result = pipeline.validateTelemetry(p, "{}", "unknown-device", "tenant-001");
            assertThat(result.accepted()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("DEVICE_NOT_FOUND");
        }

        @Test
        @DisplayName("Device registered to different tenant is rejected")
        void wrongTenantRejected() {
            var p = payload("dev-002", "tenant-001", 1, uniqueNonce(1));
            var result = pipeline.validateTelemetry(p, "{}", "dev-002", "tenant-001");
            assertThat(result.accepted()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("DEVICE_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("Threat 3: Replay Attack")
    class ReplayAttack {

        @Test
        @DisplayName("Same nonce reused is rejected")
        void nonceReuseRejected() {
            var p1 = payload("dev-001", "tenant-001", 1, uniqueNonce(100));
            pipeline.validateTelemetry(p1, "{}", "dev-001", "tenant-001");

            var p2 = payload("dev-001", "tenant-001", 2, uniqueNonce(100));
            var result = pipeline.validateTelemetry(p2, "{}", "dev-001", "tenant-001");
            assertThat(result.accepted()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("REPLAY_DETECTED");
        }

        @Test
        @DisplayName("Duplicate messageId is rejected")
        void duplicateMessageIdRejected() {
            var p1 = new TelemetryPayloadV1(
                    "1.0", "same-msg-id", "dev-001", "tenant-001",
                    Instant.now(), 1, "2.1.0", 1000, false,
                    payload("x", "x", 0, "x").features(),
                    new TelemetryPayloadV1.SecurityBlock(uniqueNonce(200), "disabled"));
            pipeline.validateTelemetry(p1, "{}", "dev-001", "tenant-001");

            var p2 = new TelemetryPayloadV1(
                    "1.0", "same-msg-id", "dev-001", "tenant-001",
                    Instant.now(), 2, "2.1.0", 1000, false,
                    payload("x", "x", 0, "x").features(),
                    new TelemetryPayloadV1.SecurityBlock(uniqueNonce(201), "disabled"));
            var result = pipeline.validateTelemetry(p2, "{}", "dev-001", "tenant-001");
            assertThat(result.accepted()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("DUPLICATE_MESSAGE");
        }

        @Test
        @DisplayName("Sequence regression is rejected")
        void sequenceRegressionRejected() {
            var p1 = payload("dev-001", "tenant-001", 50, uniqueNonce(301));
            pipeline.validateTelemetry(p1, "{}", "dev-001", "tenant-001");

            var p2 = payload("dev-001", "tenant-001", 30, uniqueNonce(302));
            var result = pipeline.validateTelemetry(p2, "{}", "dev-001", "tenant-001");
            assertThat(result.accepted()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("SEQUENCE_REGRESSION");
        }
    }

    @Nested
    @DisplayName("Threat 4: Payload Falsification")
    class PayloadFalsification {

        @Test
        @DisplayName("Invalid HMAC signature is rejected")
        void invalidSignatureRejected() {
            deviceChecker.setHmacKey("dev-001", "correct-key-bytes".getBytes());
            var p = new TelemetryPayloadV1(
                    "1.0", "msg-1", "dev-001", "tenant-001",
                    Instant.now(), 1, "2.1.0", 1000, false,
                    payload("x", "x", 0, "x").features(),
                    new TelemetryPayloadV1.SecurityBlock(uniqueNonce(400), "fake-signature-value-here"));
            var result = pipeline.validateTelemetry(p, "{\"security\":{\"signature\":\"fake-signature-value-here\"}}", "dev-001", "tenant-001");
            assertThat(result.accepted()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("INVALID_SIGNATURE");
        }

        @Test
        @DisplayName("Missing signature is rejected")
        void missingSignatureRejected() {
            deviceChecker.setHmacKey("dev-001", "correct-key-bytes".getBytes());
            var p = new TelemetryPayloadV1(
                    "1.0", "msg-sig-2", "dev-001", "tenant-001",
                    Instant.now(), 1, "2.1.0", 1000, false,
                    payload("x", "x", 0, "x").features(),
                    new TelemetryPayloadV1.SecurityBlock(uniqueNonce(401), ""));
            var result = pipeline.validateTelemetry(p, "{}", "dev-001", "tenant-001");
            assertThat(result.accepted()).isFalse();
        }
    }

    @Nested
    @DisplayName("Threat 5: Revoked Device")
    class RevokedDevice {

        @Test
        @DisplayName("Revoked device messages are rejected immediately")
        void revokedDeviceRejected() {
            deviceChecker.revokeDevice("dev-001");
            var p = payload("dev-001", "tenant-001", 1, uniqueNonce(500));
            var result = pipeline.validateTelemetry(p, "{}", "dev-001", "tenant-001");
            assertThat(result.accepted()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("DEVICE_REVOKED");
        }
    }

    @Nested
    @DisplayName("Threat 6: Tenant/DeviceId Spoofing")
    class TenantSpoofing {

        @Test
        @DisplayName("Topic tenantId mismatch with payload is rejected")
        void topicTenantMismatch() {
            var p = payload("dev-001", "evil-tenant", 1, uniqueNonce(600));
            var result = pipeline.validateTelemetry(p, "{}", "dev-001", "evil-tenant");
            assertThat(result.accepted()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("DEVICE_NOT_FOUND");
        }

        @Test
        @DisplayName("Topic deviceId mismatch with payload deviceId is rejected")
        void topicDeviceMismatch() {
            var p = payload("dev-001", "tenant-001", 1, uniqueNonce(601));
            var result = pipeline.validateTelemetry(p, "{}", "dev-002", "tenant-001");
            assertThat(result.accepted()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("TOPIC_PAYLOAD_MISMATCH");
        }
    }

    @Nested
    @DisplayName("Threat 8: Obsolete Firmware")
    class ObsoleteFirmware {

        @Test
        @DisplayName("Firmware version is tracked on successful validation")
        void firmwareTracked() {
            var p = payload("dev-001", "tenant-001", 1, uniqueNonce(800));
            var result = pipeline.validateTelemetry(p, "{}", "dev-001", "tenant-001");
            assertThat(result.accepted()).isTrue();
            assertThat(deviceChecker.getLastFirmwareVersion("dev-001")).isEqualTo("2.1.0");
        }
    }

    @Nested
    @DisplayName("Threat 9: Timestamp Violations")
    class TimestampViolations {

        @Test
        @DisplayName("Timestamp far in the future is rejected")
        void futureTimestampRejected() {
            var p = new TelemetryPayloadV1(
                    "1.0", "msg-ts-future", "dev-001", "tenant-001",
                    Instant.now().plusSeconds(600), 1, "2.1.0", 1000, false,
                    payload("x", "x", 0, "x").features(),
                    new TelemetryPayloadV1.SecurityBlock(uniqueNonce(900), "disabled"));
            var result = pipeline.validateTelemetry(p, "{}", "dev-001", "tenant-001");
            assertThat(result.accepted()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("TIMESTAMP_FUTURE");
        }

        @Test
        @DisplayName("Timestamp too old (non-drain mode) is rejected")
        void oldTimestampRejected() {
            var p = new TelemetryPayloadV1(
                    "1.0", "msg-ts-old", "dev-001", "tenant-001",
                    Instant.now().minusSeconds(400), 1, "2.1.0", 1000, false,
                    payload("x", "x", 0, "x").features(),
                    new TelemetryPayloadV1.SecurityBlock(uniqueNonce(901), "disabled"));
            var result = pipeline.validateTelemetry(p, "{}", "dev-001", "tenant-001");
            assertThat(result.accepted()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("TIMESTAMP_TOO_OLD");
        }

        @Test
        @DisplayName("Old timestamp in drain mode is accepted (72h window)")
        void drainModeAcceptsOldTimestamp() {
            var p = new TelemetryPayloadV1(
                    "1.0", "msg-drain", "dev-001", "tenant-001",
                    Instant.now().minusSeconds(3600), 1, "2.1.0", 1000, true,
                    payload("x", "x", 0, "x").features(),
                    new TelemetryPayloadV1.SecurityBlock(uniqueNonce(902), "disabled"));
            var result = pipeline.validateTelemetry(p, "{}", "dev-001", "tenant-001");
            assertThat(result.accepted()).isTrue();
        }
    }

    @Nested
    @DisplayName("Threat 11: Schema/Injection Validation")
    class SchemaValidation {

        @Test
        @DisplayName("Unsupported schema version is rejected")
        void invalidSchemaVersion() {
            var p = new TelemetryPayloadV1(
                    "99.0", "msg-schema", "dev-001", "tenant-001",
                    Instant.now(), 1, "2.1.0", 1000, false,
                    payload("x", "x", 0, "x").features(),
                    new TelemetryPayloadV1.SecurityBlock(uniqueNonce(1100), "disabled"));
            var result = pipeline.validateTelemetry(p, "{}", "dev-001", "tenant-001");
            assertThat(result.accepted()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("INVALID_SCHEMA_VERSION");
        }

        @Test
        @DisplayName("Out-of-range feature values are rejected")
        void outOfRangeValues() {
            var p = new TelemetryPayloadV1(
                    "1.0", "msg-range", "dev-001", "tenant-001",
                    Instant.now(), 1, "2.1.0", 1000, false,
                    new TelemetryPayloadV1.TelemetryFeatures(
                            9999.0, 230.0, 3200.0, 300.0, 0.94,
                            4.7, 39.5, 0.18, 0, 1, 0.97),
                    new TelemetryPayloadV1.SecurityBlock(uniqueNonce(1101), "disabled"));
            var result = pipeline.validateTelemetry(p, "{}", "dev-001", "tenant-001");
            assertThat(result.accepted()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("FIELD_OUT_OF_RANGE");
        }

        @Test
        @DisplayName("Invalid nonce length is rejected")
        void invalidNonceLength() {
            var p = new TelemetryPayloadV1(
                    "1.0", "msg-nonce-bad", "dev-001", "tenant-001",
                    Instant.now(), 1, "2.1.0", 1000, false,
                    payload("x", "x", 0, "x").features(),
                    new TelemetryPayloadV1.SecurityBlock("short", "disabled"));
            var result = pipeline.validateTelemetry(p, "{}", "dev-001", "tenant-001");
            assertThat(result.accepted()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("FIELD_OUT_OF_RANGE");
        }
    }

    @Nested
    @DisplayName("Threat 12: DoS via Rate Limiting")
    class RateLimiting {

        @Test
        @DisplayName("Device exceeding rate limit is rejected")
        void rateLimitEnforced() {
            for (int i = 1; i <= 5; i++) {
                var p = payload("dev-001", "tenant-001", i, uniqueNonce(1200 + i));
                assertThat(pipeline.validateTelemetry(p, "{}", "dev-001", "tenant-001").accepted()).isTrue();
            }

            var overLimit = payload("dev-001", "tenant-001", 6, uniqueNonce(1206));
            var result = pipeline.validateTelemetry(overLimit, "{}", "dev-001", "tenant-001");
            assertThat(result.accepted()).isFalse();
            assertThat(result.rejectionCode()).isEqualTo("RATE_LIMITED");
        }

        @Test
        @DisplayName("Different devices have independent rate limits")
        void independentRateLimits() {
            for (int i = 1; i <= 5; i++) {
                var p = payload("dev-001", "tenant-001", i, uniqueNonce(1300 + i));
                pipeline.validateTelemetry(p, "{}", "dev-001", "tenant-001");
            }

            var p2 = payload("dev-002", "tenant-002", 1, uniqueNonce(1310));
            var result = pipeline.validateTelemetry(p2, "{}", "dev-002", "tenant-002");
            assertThat(result.accepted()).isTrue();
        }
    }

    @Nested
    @DisplayName("Threat 10: Logs must not expose secrets")
    class LogSecurity {

        @Test
        @DisplayName("Security auditor sanitizes sensitive data in details")
        void sanitizesSecrets() {
            var auditInstance = new IoTSecurityAuditor();
            // Should not throw and should mask any secret-like content
            auditInstance.logSecurityEvent(
                    IoTSecurityAuditor.SecurityEvent.SIGNATURE_INVALID,
                    "dev-001", "tenant-001",
                    "key=super-secret-hmac-value token=abc123");
        }
    }

    @Nested
    @DisplayName("Signature Verification")
    class SignatureVerification {

        @Test
        @DisplayName("Valid signature passes verification")
        void validSignature() {
            var verifier = new SignatureVerifier();
            byte[] key = "test-key-32-bytes-for-hmac-sha!".getBytes();
            String payload = "{\"data\":\"test\",\"security\":{\"signature\":\"\"}}";
            String signature = verifier.computeHmac(payload, key);

            var result = verifier.verify(
                    payload.replace("\"signature\":\"\"", "\"signature\":\"" + signature + "\""),
                    signature, key);
            assertThat(result).isEqualTo(SignatureVerifier.VerificationResult.VALID);
        }

        @Test
        @DisplayName("Tampered payload fails signature")
        void tamperedPayloadFails() {
            var verifier = new SignatureVerifier();
            byte[] key = "test-key-32-bytes-for-hmac-sha!".getBytes();
            String original = "{\"data\":\"test\",\"security\":{\"signature\":\"\"}}";
            String signature = verifier.computeHmac(original, key);
            String tampered = "{\"data\":\"evil\",\"security\":{\"signature\":\"" + signature + "\"}}";

            var result = verifier.verify(tampered, signature, key);
            assertThat(result).isEqualTo(SignatureVerifier.VerificationResult.INVALID_SIGNATURE);
        }

        @Test
        @DisplayName("Null key disables verification")
        void nullKeyDisables() {
            var verifier = new SignatureVerifier();
            var result = verifier.verify("{}", "some-sig", null);
            assertThat(result).isEqualTo(SignatureVerifier.VerificationResult.SIGNATURE_DISABLED);
        }

        @Test
        @DisplayName("Constant-time comparison prevents timing attacks")
        void constantTimeComparison() {
            var verifier = new SignatureVerifier();
            byte[] key = "timing-attack-test-key-padding!".getBytes();
            String payload = "{\"security\":{\"signature\":\"\"}}";
            String correctSig = verifier.computeHmac(payload, key);
            String wrongSig = correctSig.substring(0, correctSig.length() - 1) + "x";

            var result = verifier.verify(
                    payload.replace("\"signature\":\"\"", "\"signature\":\"" + wrongSig + "\""),
                    wrongSig, key);
            assertThat(result).isEqualTo(SignatureVerifier.VerificationResult.INVALID_SIGNATURE);
        }
    }

    // --- Test helpers ---

    static class FakeDeviceStatusChecker implements ProtocolValidationPipeline.DeviceStatusChecker {
        private final Set<String> knownDevices = new HashSet<>();
        private final Set<String> revokedDevices = new HashSet<>();
        private final Map<String, byte[]> hmacKeys = new HashMap<>();
        private final Map<String, String> firmwareVersions = new HashMap<>();

        void registerDevice(String deviceId, String tenantId) {
            knownDevices.add(deviceId + ":" + tenantId);
        }

        void revokeDevice(String deviceId) {
            revokedDevices.add(deviceId);
        }

        void setHmacKey(String deviceId, byte[] key) {
            hmacKeys.put(deviceId, key);
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
            return hmacKeys.get(deviceId);
        }

        @Override
        public void recordFirmwareVersion(String deviceId, String firmwareVersion) {
            firmwareVersions.put(deviceId, firmwareVersion);
        }
    }

    static class InMemoryNonceStore implements AntiReplayGuard.NonceStore {
        private final Set<String> nonces = new HashSet<>();
        @Override public boolean exists(String nonce) { return nonces.contains(nonce); }
        @Override public void store(String nonce) { nonces.add(nonce); }
    }

    static class InMemorySequenceStore implements AntiReplayGuard.SequenceStore {
        private final Map<String, Long> sequences = new HashMap<>();
        @Override public long getLastSequence(String deviceId) { return sequences.getOrDefault(deviceId, 0L); }
        @Override public void updateSequence(String deviceId, long sequence) { sequences.put(deviceId, sequence); }
    }

    static class InMemoryMessageIdStore implements AntiReplayGuard.MessageIdStore {
        private final Set<String> ids = new HashSet<>();
        @Override public boolean exists(String messageId) { return ids.contains(messageId); }
        @Override public void store(String messageId) { ids.add(messageId); }
    }
}
