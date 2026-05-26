package com.pyrosense.dashboard.application.usecase;

import com.pyrosense.dashboard.application.port.in.GetPriorityInterventionsQuery;
import com.pyrosense.dashboard.application.port.out.DashboardReadModelPort;
import com.pyrosense.dashboard.domain.model.PriorityIntervention;
import com.pyrosense.shared.id.TenantId;
import java.util.List;

public class GetPriorityInterventionsService implements GetPriorityInterventionsQuery {

    private final DashboardReadModelPort readModel;

    public GetPriorityInterventionsService(DashboardReadModelPort readModel) {
        this.readModel = readModel;
    }

    @Override
    public List<PriorityIntervention> getPriorityInterventions(TenantId tenantId, int limit) {
        return readModel.getPriorityInterventions(tenantId, limit);
    }

    @Override
    public List<PriorityIntervention> getAssignedInterventions(String userId, int limit) {
        return readModel.getInterventionsByAssignee(userId, limit);
    }
}
