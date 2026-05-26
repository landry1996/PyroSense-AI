package com.pyrosense.dashboard.application.port.in;

import com.pyrosense.dashboard.domain.model.DeviceHealthSummary;
import com.pyrosense.shared.id.TenantId;

public interface GetDeviceHealthQuery {

    DeviceHealthSummary getDeviceHealth(TenantId tenantId);
}
