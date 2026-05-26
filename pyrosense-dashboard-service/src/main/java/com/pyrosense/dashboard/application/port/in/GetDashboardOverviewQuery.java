package com.pyrosense.dashboard.application.port.in;

import com.pyrosense.dashboard.domain.model.DashboardOverview;
import com.pyrosense.shared.id.TenantId;

public interface GetDashboardOverviewQuery {

    DashboardOverview getOverview(TenantId tenantId);
}
