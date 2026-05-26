package com.pyrosense.dashboard.application.port.out;

import com.pyrosense.dashboard.domain.model.*;
import com.pyrosense.shared.id.TenantId;
import java.util.List;

public interface DashboardReadModelPort {

    DashboardOverview getOverview(TenantId tenantId);

    List<RiskyBuilding> getRiskyBuildings(TenantId tenantId, int limit);

    List<RiskTrendPoint> getRiskTrend(TenantId tenantId, int days);

    List<RecentAlert> getRecentAlerts(TenantId tenantId, int limit);

    List<PriorityIntervention> getPriorityInterventions(TenantId tenantId, int limit);

    List<PriorityIntervention> getInterventionsByAssignee(String userId, int limit);

    DeviceHealthSummary getDeviceHealth(TenantId tenantId);
}
