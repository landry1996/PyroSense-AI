package com.pyrosense.device.application.usecase;

import com.pyrosense.device.application.port.in.GetDeviceStatisticsQuery;
import com.pyrosense.device.application.port.out.DeviceRepositoryPort;
import com.pyrosense.device.domain.model.DeviceStatus;
import com.pyrosense.shared.id.TenantId;

import java.util.Map;

public class GetDeviceStatisticsService implements GetDeviceStatisticsQuery {

    private final DeviceRepositoryPort repository;

    public GetDeviceStatisticsService(DeviceRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public DeviceStatistics getByTenant(TenantId tenantId) {
        Map<DeviceStatus, Integer> counts = repository.countByTenantAndStatus(tenantId);

        int active = counts.getOrDefault(DeviceStatus.ACTIVE, 0);
        int offline = counts.getOrDefault(DeviceStatus.OFFLINE, 0);
        int provisioned = counts.getOrDefault(DeviceStatus.PROVISIONED, 0);
        int revoked = counts.getOrDefault(DeviceStatus.REVOKED, 0);
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();

        return new DeviceStatistics(total, active, offline, provisioned, revoked);
    }
}
