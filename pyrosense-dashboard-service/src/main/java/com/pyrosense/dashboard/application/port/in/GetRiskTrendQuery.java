package com.pyrosense.dashboard.application.port.in;

import com.pyrosense.dashboard.domain.model.RiskTrendPoint;
import com.pyrosense.shared.id.TenantId;
import java.util.List;

public interface GetRiskTrendQuery {

    List<RiskTrendPoint> getRiskTrend(TenantId tenantId, int days);
}
