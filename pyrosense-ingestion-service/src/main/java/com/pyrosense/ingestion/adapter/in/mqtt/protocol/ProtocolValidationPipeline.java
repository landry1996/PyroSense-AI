package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtocolValidationPipeline {

    private static final Logger log = LoggerFactory.getLogger(ProtocolValidationPipeline.class);

    private final PayloadValidator validator;
    private final SignatureVerifier signatureVerifier;
    private final AntiReplayGuard antiReplayGuard;
    private final DeviceStatusChecker deviceStatusChecker;
    private final DeviceRateLimiter rateLimiter;
    private final IoTSecurityAuditor auditor;

    public ProtocolValidationPipeline(PayloadValidator validator,
                                       SignatureVerifier signatureVerifier,
                                       AntiReplayGuard antiReplayGuard,
                                       DeviceStatusChecker deviceStatusChecker) {
        this(validator, signatureVerifier, antiReplayGuard, deviceStatusChecker, null, null);
    }

    public ProtocolValidationPipeline(PayloadValidator validator,
                                       SignatureVerifier signatureVerifier,
                                       AntiReplayGuard antiReplayGuard,
                                       DeviceStatusChecker deviceStatusChecker,
                                       DeviceRateLimiter rateLimiter,
                                       IoTSecurityAuditor auditor) {
        this.validator = validator;
        this.signatureVerifier = signatureVerifier;
        this.antiReplayGuard = antiReplayGuard;
        this.deviceStatusChecker = deviceStatusChecker;
        this.rateLimiter = rateLimiter;
        this.auditor = auditor;
    }

    public record PipelineResult(boolean valid, String rejectionCode, String rejectionReason) {
        public static PipelineResult ok() {
            return new PipelineResult(true, null, null);
        }

        public static PipelineResult rejected(String code, String reason) {
            return new PipelineResult(false, code, reason);
        }

        public boolean accepted() {
            return valid;
        }
    }

    public PipelineResult validateTelemetry(TelemetryPayloadV1 payload,
                                             String rawJson,
                                             String topicDeviceId,
                                             String topicTenantId) {
        // Step 0: Per-device rate limiting
        if (rateLimiter != null) {
            var rateResult = rateLimiter.check(topicDeviceId);
            if (rateResult == DeviceRateLimiter.RateLimitResult.RATE_LIMITED) {
                auditEvent(IoTSecurityAuditor.SecurityEvent.RATE_LIMITED, topicDeviceId, topicTenantId, "Device exceeded message rate limit");
                return PipelineResult.rejected("RATE_LIMITED", "Device exceeded maximum message rate");
            }
        }

        // Step 1: Schema & field validation
        var validationResult = validator.validateTelemetry(payload, topicDeviceId, topicTenantId);
        if (!validationResult.valid()) {
            log.warn("Validation failed for device={}: {}", topicDeviceId, validationResult.errors());
            if ("TOPIC_PAYLOAD_MISMATCH".equals(validationResult.rejectionCode())) {
                auditEvent(IoTSecurityAuditor.SecurityEvent.TOPIC_MISMATCH, topicDeviceId, topicTenantId, "Payload IDs don't match topic");
            }
            return PipelineResult.rejected(validationResult.rejectionCode(),
                    String.join("; ", validationResult.errors()));
        }

        // Step 2: Device authorization (not revoked)
        var deviceStatus = deviceStatusChecker.checkDevice(topicDeviceId, topicTenantId);
        if (deviceStatus == DeviceStatusChecker.Status.REVOKED) {
            log.warn("Rejected message from revoked device={}", topicDeviceId);
            auditEvent(IoTSecurityAuditor.SecurityEvent.DEVICE_REVOKED_ATTEMPT, topicDeviceId, topicTenantId, "Revoked device still sending");
            return PipelineResult.rejected("DEVICE_REVOKED", "Device has been revoked");
        }
        if (deviceStatus == DeviceStatusChecker.Status.NOT_FOUND) {
            log.warn("Rejected message from unknown device={}", topicDeviceId);
            auditEvent(IoTSecurityAuditor.SecurityEvent.DEVICE_NOT_FOUND, topicDeviceId, topicTenantId, "Unregistered device");
            return PipelineResult.rejected("DEVICE_NOT_FOUND", "Device not registered");
        }

        // Step 3: Signature verification
        byte[] hmacKey = deviceStatusChecker.getHmacKey(topicDeviceId);
        var sigResult = signatureVerifier.verify(rawJson, payload.security().signature(), hmacKey);
        if (sigResult == SignatureVerifier.VerificationResult.INVALID_SIGNATURE) {
            log.error("SECURITY: Invalid signature from device={}", topicDeviceId);
            auditEvent(IoTSecurityAuditor.SecurityEvent.SIGNATURE_INVALID, topicDeviceId, topicTenantId, "HMAC-SHA256 mismatch");
            return PipelineResult.rejected("INVALID_SIGNATURE", "HMAC-SHA256 verification failed");
        }
        if (sigResult == SignatureVerifier.VerificationResult.SIGNATURE_DISABLED) {
            log.warn("SECURITY: No HMAC key provisioned for device={}", topicDeviceId);
            auditEvent(IoTSecurityAuditor.SecurityEvent.SIGNATURE_INVALID, topicDeviceId, topicTenantId, "No HMAC key provisioned");
            return PipelineResult.rejected("MISSING_CREDENTIALS", "Device HMAC key not provisioned");
        }

        // Step 4: Anti-replay checks
        var replayResult = antiReplayGuard.check(
                topicDeviceId,
                payload.messageId(),
                payload.security().nonce(),
                payload.sequenceNumber());

        switch (replayResult) {
            case NONCE_REUSED:
                log.error("SECURITY: Replay detected (nonce reuse) device={}", topicDeviceId);
                auditEvent(IoTSecurityAuditor.SecurityEvent.NONCE_REUSED, topicDeviceId, topicTenantId, "Nonce reuse detected");
                return PipelineResult.rejected("REPLAY_DETECTED", "Nonce already used");
            case SEQUENCE_REGRESSION:
                log.warn("Sequence regression device={} seq={}", topicDeviceId, payload.sequenceNumber());
                auditEvent(IoTSecurityAuditor.SecurityEvent.SEQUENCE_REGRESSION, topicDeviceId, topicTenantId, "seq=" + payload.sequenceNumber());
                return PipelineResult.rejected("SEQUENCE_REGRESSION", "Sequence number is not monotonically increasing");
            case DUPLICATE_MESSAGE:
                log.debug("Duplicate messageId={} device={}", payload.messageId(), topicDeviceId);
                return PipelineResult.rejected("DUPLICATE_MESSAGE", "Message already processed");
            case OK:
                break;
        }

        // Step 5: Track firmware version
        deviceStatusChecker.recordFirmwareVersion(topicDeviceId, payload.firmwareVersion());

        return PipelineResult.ok();
    }

    private void auditEvent(IoTSecurityAuditor.SecurityEvent event, String deviceId, String tenantId, String detail) {
        if (auditor != null) {
            auditor.logSecurityEvent(event, deviceId, tenantId, detail);
        }
    }

    public PipelineResult validateEvent(DeviceEventPayloadV1 payload,
                                         String rawJson,
                                         String topicDeviceId,
                                         String topicTenantId) {
        if (rateLimiter != null) {
            var rateResult = rateLimiter.check(topicDeviceId);
            if (rateResult == DeviceRateLimiter.RateLimitResult.RATE_LIMITED) {
                auditEvent(IoTSecurityAuditor.SecurityEvent.RATE_LIMITED, topicDeviceId, topicTenantId, "Event rate limited");
                return PipelineResult.rejected("RATE_LIMITED", "Device exceeded maximum message rate");
            }
        }

        var validationResult = validator.validateDeviceEvent(payload, topicDeviceId, topicTenantId);
        if (!validationResult.valid()) {
            return PipelineResult.rejected(validationResult.rejectionCode(),
                    String.join("; ", validationResult.errors()));
        }

        var deviceStatus = deviceStatusChecker.checkDevice(topicDeviceId, topicTenantId);
        if (deviceStatus == DeviceStatusChecker.Status.REVOKED) {
            auditEvent(IoTSecurityAuditor.SecurityEvent.DEVICE_REVOKED_ATTEMPT, topicDeviceId, topicTenantId, "Revoked device event");
            return PipelineResult.rejected("DEVICE_REVOKED", "Device has been revoked");
        }

        byte[] hmacKey = deviceStatusChecker.getHmacKey(topicDeviceId);
        var sigResult = signatureVerifier.verify(rawJson, payload.security().signature(), hmacKey);
        if (sigResult == SignatureVerifier.VerificationResult.INVALID_SIGNATURE) {
            auditEvent(IoTSecurityAuditor.SecurityEvent.SIGNATURE_INVALID, topicDeviceId, topicTenantId, "Event HMAC mismatch");
            return PipelineResult.rejected("INVALID_SIGNATURE", "HMAC verification failed");
        }
        if (sigResult == SignatureVerifier.VerificationResult.SIGNATURE_DISABLED) {
            auditEvent(IoTSecurityAuditor.SecurityEvent.SIGNATURE_INVALID, topicDeviceId, topicTenantId, "No HMAC key provisioned for event");
            return PipelineResult.rejected("MISSING_CREDENTIALS", "Device HMAC key not provisioned");
        }

        var replayResult = antiReplayGuard.check(
                topicDeviceId, payload.messageId(),
                payload.security().nonce(), payload.sequenceNumber());
        if (replayResult != AntiReplayGuard.ReplayCheckResult.OK) {
            auditEvent(IoTSecurityAuditor.SecurityEvent.REPLAY_DETECTED, topicDeviceId, topicTenantId, replayResult.name());
            return PipelineResult.rejected("REPLAY_DETECTED", replayResult.name());
        }

        return PipelineResult.ok();
    }

    public interface DeviceStatusChecker {
        enum Status { ACTIVE, REVOKED, NOT_FOUND }

        Status checkDevice(String deviceId, String tenantId);
        byte[] getHmacKey(String deviceId);
        void recordFirmwareVersion(String deviceId, String firmwareVersion);
    }
}
