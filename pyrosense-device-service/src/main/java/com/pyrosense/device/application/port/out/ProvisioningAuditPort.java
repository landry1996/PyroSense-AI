package com.pyrosense.device.application.port.out;

public interface ProvisioningAuditPort {

    void logClaimTokenCreated(String deviceId, String tenantId, String actor);

    void logProvisioningAttempt(String deviceId, String sourceIp, boolean success, String reason);

    void logCredentialRotated(String deviceId, int version, String actor);

    void logDeviceRevoked(String deviceId, String reason, String actor);
}
