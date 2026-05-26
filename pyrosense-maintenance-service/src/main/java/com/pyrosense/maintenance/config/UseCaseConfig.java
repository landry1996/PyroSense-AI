package com.pyrosense.maintenance.config;

import com.pyrosense.maintenance.application.port.in.CreateInterventionUseCase;
import com.pyrosense.maintenance.application.port.out.*;
import com.pyrosense.maintenance.application.usecase.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public CreateInterventionService createInterventionService(InterventionRepositoryPort repository,
                                                               MaintenanceEventPublisherPort eventPublisher) {
        return new CreateInterventionService(repository, eventPublisher);
    }

    @Bean
    public ManageInterventionService manageInterventionService(InterventionRepositoryPort repository,
                                                               MaintenanceEventPublisherPort eventPublisher,
                                                               RiskScoreReevaluationPublisherPort riskReevaluationPublisher) {
        return new ManageInterventionService(repository, eventPublisher, riskReevaluationPublisher);
    }

    @Bean
    public GetInterventionService getInterventionService(InterventionRepositoryPort repository) {
        return new GetInterventionService(repository);
    }

    @Bean
    public AddInterventionCommentService addInterventionCommentService(InterventionRepositoryPort repository) {
        return new AddInterventionCommentService(repository);
    }

    @Bean
    public ManageRecommendationService manageRecommendationService(RecommendationRepositoryPort recommendationRepository,
                                                                    CreateInterventionUseCase createInterventionUseCase,
                                                                    MaintenanceEventPublisherPort eventPublisher,
                                                                    AuditLogPort auditLog) {
        return new ManageRecommendationService(recommendationRepository, createInterventionUseCase, eventPublisher, auditLog);
    }
}
