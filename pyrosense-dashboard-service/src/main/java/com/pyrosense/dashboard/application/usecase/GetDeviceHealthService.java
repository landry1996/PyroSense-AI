package com.pyrosense.dashboard.application.usecase;

import com.pyrosense.dashboard.application.port.in.GetDeviceHealthQuery;
import com.pyrosense.dashboard.application.port.out.DashboardCachePort;
import com.pyrosense.dashboard.application.port.out.DashboardReadModelPort;
import com.pyrosense.dashboard.domain.model.DeviceHealthSummary;
import com.pyrosense.shared.id.TenantId;
import java.time.Duration;

public class GetDeviceHealthService implements GetDeviceHealthQuery {

    private final DashboardReadModelPort readModel;
    private final DashboardCachePort cache;
    private final Duration cacheTtl;

    public GetDeviceHealthService(DashboardReadModelPort readModel, DashboardCachePort cache, Duration cacheTtl) {
        this.readModel = readModel;
        this.cache = cache;
        this.cacheTtl = cacheTtl;
    }

    @Override
    public DeviceHealthSummary getDeviceHealth(TenantId tenantId) {
        String cacheKey = "dashboard:device-health:" + tenantId.value();
        return cache.get(cacheKey, DeviceHealthSummary.class).orElseGet(() -> {
            DeviceHealthSummary health = readModel.getDeviceHealth(tenantId);
            cache.put(cacheKey, health, cacheTtl);
            return health;
        });
    }
}
