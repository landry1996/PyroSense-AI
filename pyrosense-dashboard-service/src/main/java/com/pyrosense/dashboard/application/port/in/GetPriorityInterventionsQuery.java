package com.pyrosense.dashboard.application.port.in;

import com.pyrosense.dashboard.domain.model.PriorityIntervention;
import com.pyrosense.shared.id.TenantId;
import java.util.List;

public interface GetPriorityInterventionsQuery {

    List<PriorityIntervention> getPriorityInterventions(TenantId tenantId, int limit);

    List<PriorityIntervention> getAssignedInterventions(String userId, int limit);
}
