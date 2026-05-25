package com.pyrosense.device.application.port.in;

import com.pyrosense.shared.id.DeviceId;

public interface ActivateDeviceUseCase {

    void execute(DeviceId deviceId, String actor);
}
