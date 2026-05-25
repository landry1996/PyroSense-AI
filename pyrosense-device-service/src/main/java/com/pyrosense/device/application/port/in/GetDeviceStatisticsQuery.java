package com.pyrosense.device.application.port.in;

import com.pyrosense.shared.id.TenantId;

public interface GetDeviceStatisticsQuery {

    DeviceStatistics getByTenant(TenantId tenantId);

    record DeviceStatistics(int total, int active, int offline, int provisioned, int revoked) {}
}
