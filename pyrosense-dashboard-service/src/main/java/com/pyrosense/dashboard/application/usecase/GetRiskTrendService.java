package com.pyrosense.dashboard.application.usecase;

import com.pyrosense.dashboard.application.port.in.GetRiskTrendQuery;
import com.pyrosense.dashboard.application.port.out.DashboardCachePort;
import com.pyrosense.dashboard.application.port.out.DashboardReadModelPort;
import com.pyrosense.dashboard.domain.model.RiskTrendPoint;
import com.pyrosense.shared.id.TenantId;
import java.time.Duration;
import java.util.List;

public class GetRiskTrendService implements GetRiskTrendQuery {

    private final DashboardReadModelPort readModel;
    private final DashboardCachePort cache;
    private final Duration cacheTtl;

    public GetRiskTrendService(DashboardReadModelPort readModel, DashboardCachePort cache, Duration cacheTtl) {
        this.readModel = readModel;
        this.cache = cache;
        this.cacheTtl = cacheTtl;
    }

    @Override
    public List<RiskTrendPoint> getRiskTrend(TenantId tenantId, int days) {
        String cacheKey = "dashboard:risk-trend:" + tenantId.value() + ":" + days;
        return cache.get(cacheKey, List.class).orElseGet(() -> {
            List<RiskTrendPoint> trend = readModel.getRiskTrend(tenantId, days);
            cache.put(cacheKey, trend, cacheTtl);
            return trend;
        });
    }
}
