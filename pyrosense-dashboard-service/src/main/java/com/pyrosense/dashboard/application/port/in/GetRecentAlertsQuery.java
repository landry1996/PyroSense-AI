package com.pyrosense.dashboard.application.port.in;

import com.pyrosense.dashboard.domain.model.RecentAlert;
import com.pyrosense.shared.id.TenantId;
import java.util.List;

public interface GetRecentAlertsQuery {

    List<RecentAlert> getRecentAlerts(TenantId tenantId, int limit);
}
