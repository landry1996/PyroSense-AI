package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import com.pyrosense.ingestion.application.port.out.DeviceAuthenticationPort;
import org.springframework.stereotype.Component;

@Component
public class DeviceStatusCheckerAdapter implements ProtocolValidationPipeline.DeviceStatusChecker {

    private final DeviceAuthenticationPort authPort;

    public DeviceStatusCheckerAdapter(DeviceAuthenticationPort authPort) {
        this.authPort = authPort;
    }

    @Override
    public Status checkDevice(String deviceId, String tenantId) {
        var status = authPort.checkDeviceStatus(deviceId, tenantId);
        return switch (status) {
            case ACTIVE -> Status.ACTIVE;
            case REVOKED -> Status.REVOKED;
            case NOT_FOUND -> Status.NOT_FOUND;
        };
    }

    @Override
    public byte[] getHmacKey(String deviceId) {
        return authPort.getActiveHmacKey(deviceId);
    }

    @Override
    public void recordFirmwareVersion(String deviceId, String firmwareVersion) {
        authPort.recordFirmwareVersion(deviceId, firmwareVersion);
    }
}
