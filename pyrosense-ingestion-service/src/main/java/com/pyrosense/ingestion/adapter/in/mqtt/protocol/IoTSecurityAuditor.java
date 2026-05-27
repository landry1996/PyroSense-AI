package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class IoTSecurityAuditor {

    private static final Logger audit = LoggerFactory.getLogger("com.pyrosense.security.audit");

    public enum SecurityEvent {
        SIGNATURE_INVALID,
        REPLAY_DETECTED,
        DEVICE_REVOKED_ATTEMPT,
        DEVICE_NOT_FOUND,
        RATE_LIMITED,
        TIMESTAMP_VIOLATION,
        TOPIC_MISMATCH,
        PAYLOAD_TOO_LARGE,
        SCHEMA_INVALID,
        FIRMWARE_OBSOLETE,
        NONCE_REUSED,
        SEQUENCE_REGRESSION
    }

    public void logSecurityEvent(SecurityEvent event, String deviceId, String tenantId, String detail) {
        try {
            MDC.put("securityEvent", event.name());
            MDC.put("deviceId", deviceId != null ? deviceId : "unknown");
            MDC.put("tenantId", tenantId != null ? tenantId : "unknown");
            audit.warn("SECURITY_EVENT: event={} device={} tenant={} detail={}",
                    event.name(), maskDeviceId(deviceId), maskTenantId(tenantId), sanitizeDetail(detail));
        } finally {
            MDC.remove("securityEvent");
            MDC.remove("deviceId");
            MDC.remove("tenantId");
        }
    }

    public void logProvisioningAttempt(String deviceSerial, String sourceIp, boolean success, String reason) {
        if (success) {
            audit.info("PROVISIONING_SUCCESS: serial={} ip={}", maskSerial(deviceSerial), sourceIp);
        } else {
            audit.warn("PROVISIONING_FAILED: serial={} ip={} reason={}",
                    maskSerial(deviceSerial), sourceIp, reason);
        }
    }

    public void logCredentialRotation(String deviceId, int newVersion) {
        audit.info("CREDENTIAL_ROTATED: device={} version={}", maskDeviceId(deviceId), newVersion);
    }

    public void logDeviceRevocation(String deviceId, String reason) {
        audit.warn("DEVICE_REVOKED: device={} reason={}", maskDeviceId(deviceId), reason);
    }

    private String maskDeviceId(String deviceId) {
        if (deviceId == null || deviceId.length() < 8) return "***";
        return deviceId.substring(0, 4) + "..." + deviceId.substring(deviceId.length() - 4);
    }

    private String maskTenantId(String tenantId) {
        if (tenantId == null || tenantId.length() < 8) return "***";
        return tenantId.substring(0, 4) + "...";
    }

    private String maskSerial(String serial) {
        if (serial == null || serial.length() < 6) return "***";
        return serial.substring(0, 3) + "***" + serial.substring(serial.length() - 3);
    }

    private String sanitizeDetail(String detail) {
        if (detail == null) return "";
        return detail.replaceAll("(?i)(key|secret|token|password|hmac)=[^\\s,;]+", "$1=***")
                .substring(0, Math.min(detail.length(), 200));
    }
}
