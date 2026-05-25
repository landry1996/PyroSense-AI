package com.pyrosense.analysis.application.port.out;

import com.pyrosense.analysis.domain.model.BaselineProfile;
import com.pyrosense.shared.id.DeviceId;

import java.util.Optional;

public interface BaselineProfileRepositoryPort {
    void save(BaselineProfile profile);
    Optional<BaselineProfile> findByDeviceId(DeviceId deviceId);
    void deleteByDeviceId(DeviceId deviceId);
}
