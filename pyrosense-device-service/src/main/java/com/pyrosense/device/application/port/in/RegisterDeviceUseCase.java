package com.pyrosense.device.application.port.in;

import com.pyrosense.device.domain.model.ConnectivityType;
import com.pyrosense.shared.id.DeviceId;

public interface RegisterDeviceUseCase {

    record RegisterDeviceCommand(
            String serialNumber,
            String firmwareVersion,
            String hardwareRevision,
            ConnectivityType connectivityType
    ) {}

    record RegisterDeviceResult(DeviceId deviceId, String enrollmentKey) {}

    RegisterDeviceResult execute(RegisterDeviceCommand command, String actor);
}
