package com.pyrosense.device.application.port.in;

import com.pyrosense.shared.id.DeviceId;

public interface RecordHeartbeatUseCase {

    void execute(DeviceId deviceId);
}
