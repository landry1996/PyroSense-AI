package com.pyrosense.device.application.port.in;

import com.pyrosense.shared.id.DeviceId;

public interface RotateDeviceCredentialUseCase {

    record RotateCredentialResult(String newHmacKey, int version) {}

    RotateCredentialResult execute(DeviceId deviceId, String actor);
}
