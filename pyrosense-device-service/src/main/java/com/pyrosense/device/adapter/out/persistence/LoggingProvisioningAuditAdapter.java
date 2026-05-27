package com.pyrosense.device.adapter.out.persistence;

import com.pyrosense.device.application.port.out.ProvisioningAuditPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingProvisioningAuditAdapter implements ProvisioningAuditPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingProvisioningAuditAdapter.class);

    @Override
    public void logClaimTokenCreated(String deviceId, String tenantId, String actor) {
        log.info("AUDIT: claim_token_created device={} tenant={} actor={}", deviceId, tenantId, actor);
    }

    @Override
    public void logProvisioningAttempt(String deviceId, String sourceIp, boolean success, String reason) {
        if (success) {
            log.info("AUDIT: provisioning_success device={} ip={}", deviceId, sourceIp);
        } else {
            log.warn("AUDIT: provisioning_failed device={} ip={} reason={}", deviceId, sourceIp, reason);
        }
    }

    @Override
    public void logCredentialRotated(String deviceId, int version, String actor) {
        log.info("AUDIT: credential_rotated device={} version={} actor={}", deviceId, version, actor);
    }

    @Override
    public void logDeviceRevoked(String deviceId, String reason, String actor) {
        log.info("AUDIT: device_revoked device={} reason={} actor={}", deviceId, reason, actor);
    }
}
