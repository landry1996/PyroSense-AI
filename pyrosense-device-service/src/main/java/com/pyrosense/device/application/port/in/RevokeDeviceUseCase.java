package com.pyrosense.device.application.port.in;

import com.pyrosense.shared.id.DeviceId;

public interface RevokeDeviceUseCase {

    record RevokeDeviceCommand(DeviceId deviceId, String reason) {}

    void execute(RevokeDeviceCommand command, String actor);
}
