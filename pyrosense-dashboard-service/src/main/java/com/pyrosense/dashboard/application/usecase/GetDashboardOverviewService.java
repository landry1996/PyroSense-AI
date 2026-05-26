package com.pyrosense.dashboard.application.usecase;

import com.pyrosense.dashboard.application.port.in.GetDashboardOverviewQuery;
import com.pyrosense.dashboard.application.port.out.DashboardCachePort;
import com.pyrosense.dashboard.application.port.out.DashboardReadModelPort;
import com.pyrosense.dashboard.domain.model.DashboardOverview;
import com.pyrosense.shared.id.TenantId;
import java.time.Duration;

public class GetDashboardOverviewService implements GetDashboardOverviewQuery {

    private final DashboardReadModelPort readModel;
    private final DashboardCachePort cache;
    private final Duration cacheTtl;

    public GetDashboardOverviewService(DashboardReadModelPort readModel, DashboardCachePort cache, Duration cacheTtl) {
        this.readModel = readModel;
        this.cache = cache;
        this.cacheTtl = cacheTtl;
    }

    @Override
    public DashboardOverview getOverview(TenantId tenantId) {
        String cacheKey = "dashboard:overview:" + tenantId.value();
        return cache.get(cacheKey, DashboardOverview.class).orElseGet(() -> {
            DashboardOverview overview = readModel.getOverview(tenantId);
            cache.put(cacheKey, overview, cacheTtl);
            return overview;
        });
    }
}
