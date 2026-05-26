package com.pyrosense.dashboard.application.port.in;

import com.pyrosense.dashboard.domain.model.RiskyBuilding;
import com.pyrosense.shared.id.TenantId;
import java.util.List;

public interface GetRiskyBuildingsQuery {

    List<RiskyBuilding> getRiskyBuildings(TenantId tenantId, int limit);
}
