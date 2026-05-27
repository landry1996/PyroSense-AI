package com.pyrosense.ingestion.application.port.out;

import com.pyrosense.shared.id.DeviceId;

public interface DeviceAuthenticationPort {

    enum DeviceStatus {
        ACTIVE,
        REVOKED,
        NOT_FOUND
    }

    DeviceStatus checkDeviceStatus(String deviceId, String tenantId);

    byte[] getActiveHmacKey(String deviceId);

    void recordFirmwareVersion(String deviceId, String firmwareVersion);
}
