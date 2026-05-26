package com.pyrosense.dashboard.application.usecase;

import com.pyrosense.dashboard.application.port.in.GetRiskyBuildingsQuery;
import com.pyrosense.dashboard.application.port.out.DashboardCachePort;
import com.pyrosense.dashboard.application.port.out.DashboardReadModelPort;
import com.pyrosense.dashboard.domain.model.RiskyBuilding;
import com.pyrosense.shared.id.TenantId;
import java.time.Duration;
import java.util.List;

public class GetRiskyBuildingsService implements GetRiskyBuildingsQuery {

    private final DashboardReadModelPort readModel;
    private final DashboardCachePort cache;
    private final Duration cacheTtl;

    public GetRiskyBuildingsService(DashboardReadModelPort readModel, DashboardCachePort cache, Duration cacheTtl) {
        this.readModel = readModel;
        this.cache = cache;
        this.cacheTtl = cacheTtl;
    }

    @Override
    public List<RiskyBuilding> getRiskyBuildings(TenantId tenantId, int limit) {
        String cacheKey = "dashboard:risky-buildings:" + tenantId.value() + ":" + limit;
        return cache.get(cacheKey, List.class).orElseGet(() -> {
            List<RiskyBuilding> buildings = readModel.getRiskyBuildings(tenantId, limit);
            cache.put(cacheKey, buildings, cacheTtl);
            return buildings;
        });
    }
}
