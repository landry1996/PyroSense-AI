package com.pyrosense.maintenance.application.usecase;

import com.pyrosense.maintenance.application.port.in.CreateInterventionUseCase;
import com.pyrosense.maintenance.application.port.in.CreateInterventionUseCase.CreateInterventionCommand;
import com.pyrosense.maintenance.application.port.in.ManageRecommendationUseCase;
import com.pyrosense.maintenance.application.port.out.AuditLogPort;
import com.pyrosense.maintenance.application.port.out.MaintenanceEventPublisherPort;
import com.pyrosense.maintenance.application.port.out.RecommendationRepositoryPort;
import com.pyrosense.maintenance.domain.event.RecommendationAcceptedEvent;
import com.pyrosense.maintenance.domain.event.RecommendationRejectedEvent;
import com.pyrosense.maintenance.domain.model.Intervention;
import com.pyrosense.maintenance.domain.model.InterventionRecommendation;
import com.pyrosense.maintenance.domain.model.RecommendationStatus;
import com.pyrosense.shared.exception.BusinessException;
import com.pyrosense.shared.exception.ErrorCode;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;

import java.util.List;
import java.util.UUID;

public class ManageRecommendationService implements ManageRecommendationUseCase {

    private final RecommendationRepositoryPort recommendationRepository;
    private final CreateInterventionUseCase createInterventionUseCase;
    private final MaintenanceEventPublisherPort eventPublisher;
    private final AuditLogPort auditLog;

    public ManageRecommendationService(RecommendationRepositoryPort recommendationRepository,
                                       CreateInterventionUseCase createInterventionUseCase,
                                       MaintenanceEventPublisherPort eventPublisher,
                                       AuditLogPort auditLog) {
        this.recommendationRepository = recommendationRepository;
        this.createInterventionUseCase = createInterventionUseCase;
        this.eventPublisher = eventPublisher;
        this.auditLog = auditLog;
    }

    @Override
    public Intervention acceptRecommendation(UUID recommendationId, TenantId tenantId) {
        InterventionRecommendation recommendation = findOrThrow(recommendationId);

        if (!recommendation.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Access denied");
        }

        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Recommendation is already " + recommendation.getStatus());
        }

        var command = new CreateInterventionCommand(
                recommendation.getTenantId(),
                recommendation.getAlertId(),
                recommendation.getDeviceId(),
                recommendation.getSuggestedPriority() == com.pyrosense.maintenance.domain.model.InterventionPriority.URGENT ? "CRITICAL" : "WARNING",
                recommendation.getSuggestedType().name(),
                recommendation.getReason()
        );

        Intervention intervention = createInterventionUseCase.createFromAlert(command);
        recommendation.accept(intervention.getId());
        recommendationRepository.save(recommendation);

        eventPublisher.publish(new RecommendationAcceptedEvent(
                UUID.randomUUID(), ClockProvider.now(),
                recommendation.getId(), recommendation.getTenantId(),
                recommendation.getAlertId(), intervention.getId()));

        auditLog.log("RECOMMENDATION_ACCEPTED", recommendation.getTenantId(),
                "Recommendation %s accepted, intervention %s created".formatted(
                        recommendationId, intervention.getId()));

        return intervention;
    }

    @Override
    public InterventionRecommendation rejectRecommendation(UUID recommendationId, String reason, TenantId tenantId) {
        InterventionRecommendation recommendation = findOrThrow(recommendationId);

        if (!recommendation.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Access denied");
        }

        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Recommendation is already " + recommendation.getStatus());
        }

        recommendation.reject(reason);
        InterventionRecommendation saved = recommendationRepository.save(recommendation);

        eventPublisher.publish(new RecommendationRejectedEvent(
                UUID.randomUUID(), ClockProvider.now(),
                recommendation.getId(), recommendation.getTenantId(),
                recommendation.getAlertId(), reason));

        auditLog.log("RECOMMENDATION_REJECTED", recommendation.getTenantId(),
                "Recommendation %s rejected: %s".formatted(recommendationId, reason));

        return saved;
    }

    @Override
    public List<InterventionRecommendation> findPendingByTenant(TenantId tenantId) {
        return recommendationRepository.findByTenantIdAndStatus(tenantId, RecommendationStatus.PENDING);
    }

    private InterventionRecommendation findOrThrow(UUID id) {
        return recommendationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Recommendation", id.toString()));
    }
}
