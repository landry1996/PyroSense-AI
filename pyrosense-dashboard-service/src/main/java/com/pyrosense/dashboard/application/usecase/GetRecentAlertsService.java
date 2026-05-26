package com.pyrosense.dashboard.application.usecase;

import com.pyrosense.dashboard.application.port.in.GetRecentAlertsQuery;
import com.pyrosense.dashboard.application.port.out.DashboardReadModelPort;
import com.pyrosense.dashboard.domain.model.RecentAlert;
import com.pyrosense.shared.id.TenantId;
import java.util.List;

public class GetRecentAlertsService implements GetRecentAlertsQuery {

    private final DashboardReadModelPort readModel;

    public GetRecentAlertsService(DashboardReadModelPort readModel) {
        this.readModel = readModel;
    }

    @Override
    public List<RecentAlert> getRecentAlerts(TenantId tenantId, int limit) {
        return readModel.getRecentAlerts(tenantId, limit);
    }
}
