package com.pyrosense.maintenance.adapter.out.persistence;

import com.pyrosense.maintenance.application.port.out.RecommendationRepositoryPort;
import com.pyrosense.maintenance.domain.model.InterventionRecommendation;
import com.pyrosense.maintenance.domain.model.RecommendationStatus;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRecommendationRepository implements RecommendationRepositoryPort {

    private final Map<UUID, InterventionRecommendation> store = new ConcurrentHashMap<>();

    @Override
    public InterventionRecommendation save(InterventionRecommendation recommendation) {
        store.put(recommendation.getId(), recommendation);
        return recommendation;
    }

    @Override
    public Optional<InterventionRecommendation> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<InterventionRecommendation> findByAlertId(AlertId alertId) {
        return store.values().stream()
                .filter(r -> r.getAlertId().equals(alertId))
                .findFirst();
    }

    @Override
    public List<InterventionRecommendation> findByTenantIdAndStatus(TenantId tenantId, RecommendationStatus status) {
        return store.values().stream()
                .filter(r -> r.getTenantId().equals(tenantId) && r.getStatus() == status)
                .toList();
    }

    @Override
    public List<InterventionRecommendation> findPendingExpired() {
        Instant now = Instant.now();
        return store.values().stream()
                .filter(r -> r.getStatus() == RecommendationStatus.PENDING)
                .filter(r -> r.getSlaExpiresAt().isBefore(now))
                .toList();
    }
}
