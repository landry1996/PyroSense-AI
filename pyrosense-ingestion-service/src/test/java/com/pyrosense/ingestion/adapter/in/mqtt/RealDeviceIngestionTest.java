package com.pyrosense.ingestion.adapter.in.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pyrosense.ingestion.adapter.in.mqtt.protocol.*;
import com.pyrosense.ingestion.application.port.in.IngestHeartbeatUseCase;
import com.pyrosense.ingestion.application.port.in.IngestTelemetryUseCase;
import com.pyrosense.ingestion.application.port.out.*;
import com.pyrosense.ingestion.domain.model.IngestionRejectionReason;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RealDeviceIngestionTest {

    private IngestTelemetryUseCase ingestTelemetry;
    private IngestHeartbeatUseCase ingestHeartbeat;
    private TelemetryEventPublisherPort eventPublisher;
    private RejectionRepositoryPort rejectionRepository;
    private DeviceCapabilityLookupPort capabilityLookup;
    private ProtocolValidationPipeline pipeline;
    private ObjectMapper objectMapper;
    private SimpleMeterRegistry meterRegistry;

    private static final String DEVICE_ID = UUID.randomUUID().toString();
    private static final String TENANT_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setup() {
        ingestTelemetry = mock(IngestTelemetryUseCase.class);
        ingestHeartbeat = mock(IngestHeartbeatUseCase.class);
        eventPublisher = mock(TelemetryEventPublisherPort.class);
        rejectionRepository = mock(RejectionRepositoryPort.class);
        capabilityLookup = mock(DeviceCapabilityLookupPort.class);

        var payloadValidator = new PayloadValidator();
        var signatureVerifier = new SignatureVerifier();
        var nonceStore = new InMemoryNonceStore();
        var sequenceStore = new InMemorySequenceStore();
        var messageIdStore = new InMemoryMessageIdStore();
        var antiReplayGuard = new AntiReplayGuard(nonceStore, sequenceStore, messageIdStore);
        var deviceStatusChecker = new AcceptAllDeviceStatusChecker();
        pipeline = new ProtocolValidationPipeline(payloadValidator, signatureVerifier, antiReplayGuard, deviceStatusChecker);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    @DisplayName("valid v1 telemetry accepted and forwarded to use case")
    void validV1TelemetryAccepted() throws Exception {
        when(ingestTelemetry.execute(any())).thenReturn(IngestTelemetryUseCase.IngestionResult.ACCEPTED);

        var payload = createValidTelemetryPayload();
        String rawJson = objectMapper.writeValueAsString(payload);

        var result = pipeline.validateTelemetry(payload, rawJson, DEVICE_ID, TENANT_ID);
        assertThat(result.accepted()).isTrue();
    }

    @Test
    @DisplayName("invalid schema version is rejected")
    void invalidSchemaVersionRejected() throws Exception {
        var payload = new TelemetryPayloadV1(
                "2.0", UUID.randomUUID().toString(), DEVICE_ID, TENANT_ID,
                Instant.now(), 1, "1.0.0", 1000, false,
                createFeatures(), createSecurity());
        String rawJson = objectMapper.writeValueAsString(payload);

        var result = pipeline.validateTelemetry(payload, rawJson, DEVICE_ID, TENANT_ID);
        assertThat(result.accepted()).isFalse();
        assertThat(result.rejectionCode()).isEqualTo("INVALID_SCHEMA_VERSION");
    }

    @Test
    @DisplayName("topic/payload device ID mismatch is rejected")
    void topicPayloadMismatchRejected() throws Exception {
        var payload = createValidTelemetryPayload();
        String rawJson = objectMapper.writeValueAsString(payload);

        var result = pipeline.validateTelemetry(payload, rawJson, "wrong-device-id", TENANT_ID);
        assertThat(result.accepted()).isFalse();
        assertThat(result.rejectionCode()).isEqualTo("TOPIC_PAYLOAD_MISMATCH");
    }

    @Test
    @DisplayName("revoked device is rejected")
    void revokedDeviceRejected() throws Exception {
        var revokedChecker = new RevokedDeviceStatusChecker();
        var revokedPipeline = new ProtocolValidationPipeline(
                new PayloadValidator(), new SignatureVerifier(),
                new AntiReplayGuard(new InMemoryNonceStore(), new InMemorySequenceStore(), new InMemoryMessageIdStore()),
                revokedChecker);

        var payload = createValidTelemetryPayload();
        String rawJson = objectMapper.writeValueAsString(payload);

        var result = revokedPipeline.validateTelemetry(payload, rawJson, DEVICE_ID, TENANT_ID);
        assertThat(result.accepted()).isFalse();
        assertThat(result.rejectionCode()).isEqualTo("DEVICE_REVOKED");
    }

    @Test
    @DisplayName("IngestionRejectionReason maps pipeline codes correctly")
    void rejectionReasonMapping() {
        assertThat(IngestionRejectionReason.fromPipelineCode("INVALID_SIGNATURE"))
                .isEqualTo(IngestionRejectionReason.INVALID_SIGNATURE);
        assertThat(IngestionRejectionReason.fromPipelineCode("DEVICE_REVOKED"))
                .isEqualTo(IngestionRejectionReason.DEVICE_REVOKED);
        assertThat(IngestionRejectionReason.fromPipelineCode("DEVICE_NOT_FOUND"))
                .isEqualTo(IngestionRejectionReason.UNKNOWN_DEVICE);
    }

    @Test
    @DisplayName("duplicate message is detected by anti-replay")
    void duplicateMessageDetected() throws Exception {
        var payload = createValidTelemetryPayload();
        String rawJson = objectMapper.writeValueAsString(payload);

        var result1 = pipeline.validateTelemetry(payload, rawJson, DEVICE_ID, TENANT_ID);
        assertThat(result1.accepted()).isTrue();

        var result2 = pipeline.validateTelemetry(payload, rawJson, DEVICE_ID, TENANT_ID);
        assertThat(result2.accepted()).isFalse();
        assertThat(result2.rejectionCode()).isEqualTo("DUPLICATE_MESSAGE");
    }

    @Test
    @DisplayName("sequence regression is detected")
    void sequenceRegressionDetected() throws Exception {
        var nonce1 = "aabbccddeeff112233445566";
        var payload1 = new TelemetryPayloadV1(
                "1.0", UUID.randomUUID().toString(), DEVICE_ID, TENANT_ID,
                Instant.now(), 10, "1.0.0", 1000, false,
                createFeatures(), new TelemetryPayloadV1.SecurityBlock(nonce1, "disabled"));
        String rawJson1 = objectMapper.writeValueAsString(payload1);
        pipeline.validateTelemetry(payload1, rawJson1, DEVICE_ID, TENANT_ID);

        var nonce2 = "112233445566aabbccddeeff";
        var payload2 = new TelemetryPayloadV1(
                "1.0", UUID.randomUUID().toString(), DEVICE_ID, TENANT_ID,
                Instant.now(), 5, "1.0.0", 1000, false,
                createFeatures(), new TelemetryPayloadV1.SecurityBlock(nonce2, "disabled"));
        String rawJson2 = objectMapper.writeValueAsString(payload2);

        var result = pipeline.validateTelemetry(payload2, rawJson2, DEVICE_ID, TENANT_ID);
        assertThat(result.accepted()).isFalse();
        assertThat(result.rejectionCode()).isEqualTo("SEQUENCE_REGRESSION");
    }

    private TelemetryPayloadV1 createValidTelemetryPayload() {
        return new TelemetryPayloadV1(
                "1.0",
                UUID.randomUUID().toString(),
                DEVICE_ID,
                TENANT_ID,
                Instant.now(),
                1,
                "1.0.0",
                1000,
                false,
                createFeatures(),
                createSecurity()
        );
    }

    private TelemetryPayloadV1.TelemetryFeatures createFeatures() {
        return new TelemetryPayloadV1.TelemetryFeatures(
                5.0, 230.0, 1150.0, 100.0, 0.95, 3.2, 42.0, 0.15, 0, 0, 0.85);
    }

    private TelemetryPayloadV1.SecurityBlock createSecurity() {
        return new TelemetryPayloadV1.SecurityBlock(
                "a1b2c3d4e5f6a1b2c3d4e5f6",
                "disabled"
        );
    }

    // --- In-memory test implementations ---

    static class AcceptAllDeviceStatusChecker implements ProtocolValidationPipeline.DeviceStatusChecker {
        @Override
        public Status checkDevice(String deviceId, String tenantId) { return Status.ACTIVE; }
        @Override
        public byte[] getHmacKey(String deviceId) { return null; }
        @Override
        public void recordFirmwareVersion(String deviceId, String firmwareVersion) {}
    }

    static class RevokedDeviceStatusChecker implements ProtocolValidationPipeline.DeviceStatusChecker {
        @Override
        public Status checkDevice(String deviceId, String tenantId) { return Status.REVOKED; }
        @Override
        public byte[] getHmacKey(String deviceId) { return null; }
        @Override
        public void recordFirmwareVersion(String deviceId, String firmwareVersion) {}
    }

    static class InMemoryNonceStore implements AntiReplayGuard.NonceStore {
        private final java.util.Set<String> nonces = new java.util.HashSet<>();
        @Override
        public boolean exists(String nonce) { return nonces.contains(nonce); }
        @Override
        public void store(String nonce) { nonces.add(nonce); }
    }

    static class InMemorySequenceStore implements AntiReplayGuard.SequenceStore {
        private final java.util.Map<String, Long> sequences = new java.util.HashMap<>();
        @Override
        public long getLastSequence(String deviceId) { return sequences.getOrDefault(deviceId, 0L); }
        @Override
        public void updateSequence(String deviceId, long sequence) { sequences.put(deviceId, sequence); }
    }

    static class InMemoryMessageIdStore implements AntiReplayGuard.MessageIdStore {
        private final java.util.Set<String> ids = new java.util.HashSet<>();
        @Override
        public boolean exists(String messageId) { return ids.contains(messageId); }
        @Override
        public void store(String messageId) { ids.add(messageId); }
    }
}
