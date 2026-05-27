package com.pyrosense.device.application.port.out;

import com.pyrosense.device.domain.model.DeviceCredential;
import com.pyrosense.shared.id.DeviceId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceCredentialRepositoryPort {

    void save(DeviceCredential credential);

    Optional<DeviceCredential> findById(UUID id);

    Optional<DeviceCredential> findActiveByDeviceId(DeviceId deviceId);

    List<DeviceCredential> findAllByDeviceId(DeviceId deviceId);

    int countActiveByDeviceId(DeviceId deviceId);

    int nextVersionForDevice(DeviceId deviceId);
}
