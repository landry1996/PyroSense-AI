package com.pyrosense.identity.application.port.out;

import com.pyrosense.identity.domain.model.DeviceCredential;
import com.pyrosense.shared.id.DeviceId;

import java.util.Optional;

public interface DeviceCredentialRepository {

    DeviceCredential save(DeviceCredential credential);

    Optional<DeviceCredential> findActiveByDeviceId(DeviceId deviceId);

    void deleteByDeviceId(DeviceId deviceId);
}
