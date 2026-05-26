package com.pyrosense.maintenance.application.port.out;

import com.pyrosense.maintenance.domain.model.InterventionRecommendation;
import com.pyrosense.maintenance.domain.model.RecommendationStatus;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.TenantId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecommendationRepositoryPort {

    InterventionRecommendation save(InterventionRecommendation recommendation);

    Optional<InterventionRecommendation> findById(UUID id);

    Optional<InterventionRecommendation> findByAlertId(AlertId alertId);

    List<InterventionRecommendation> findByTenantIdAndStatus(TenantId tenantId, RecommendationStatus status);

    List<InterventionRecommendation> findPendingExpired();
}
