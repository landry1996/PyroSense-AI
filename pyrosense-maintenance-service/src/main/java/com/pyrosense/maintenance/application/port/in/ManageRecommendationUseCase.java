package com.pyrosense.maintenance.application.port.in;

import com.pyrosense.maintenance.domain.model.Intervention;
import com.pyrosense.maintenance.domain.model.InterventionRecommendation;
import com.pyrosense.maintenance.domain.model.RecommendationStatus;
import com.pyrosense.shared.id.TenantId;

import java.util.List;
import java.util.UUID;

public interface ManageRecommendationUseCase {

    Intervention acceptRecommendation(UUID recommendationId, TenantId tenantId);

    InterventionRecommendation rejectRecommendation(UUID recommendationId, String reason, TenantId tenantId);

    List<InterventionRecommendation> findPendingByTenant(TenantId tenantId);
}
