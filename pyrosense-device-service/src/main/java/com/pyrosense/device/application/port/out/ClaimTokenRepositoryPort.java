package com.pyrosense.device.application.port.out;

import com.pyrosense.device.domain.model.ClaimToken;
import com.pyrosense.shared.id.DeviceId;

import java.util.Optional;
import java.util.UUID;

public interface ClaimTokenRepositoryPort {

    void save(ClaimToken token);

    Optional<ClaimToken> findById(UUID id);

    Optional<ClaimToken> findActiveByDeviceId(DeviceId deviceId);

    void invalidateAllForDevice(DeviceId deviceId);

    int countRecentByIp(String sourceIp, int windowMinutes);
}
