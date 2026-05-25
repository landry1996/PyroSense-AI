package com.pyrosense.ingestion.application.port.out;

import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

public interface DeviceAuthorizationPort {

    boolean isDeviceActive(DeviceId deviceId);

    boolean isDeviceOwnedByTenant(DeviceId deviceId, TenantId tenantId);
}
