package com.pyrosense.device.application.port.in;

import com.pyrosense.shared.id.DeviceId;

public interface RevokeDeviceCredentialUseCase {

    void execute(DeviceId deviceId, String reason, String actor);
}
