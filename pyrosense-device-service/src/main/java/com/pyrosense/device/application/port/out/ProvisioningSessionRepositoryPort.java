package com.pyrosense.device.application.port.out;

import com.pyrosense.device.domain.model.DeviceProvisioningSession;
import com.pyrosense.shared.id.DeviceId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProvisioningSessionRepositoryPort {

    void save(DeviceProvisioningSession session);

    Optional<DeviceProvisioningSession> findById(UUID id);

    List<DeviceProvisioningSession> findByDeviceId(DeviceId deviceId);

    int countRecentFailedByIp(String sourceIp, int windowMinutes);
}
