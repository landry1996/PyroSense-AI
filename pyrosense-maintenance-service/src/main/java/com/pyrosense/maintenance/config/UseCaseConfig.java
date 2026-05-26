package com.pyrosense.maintenance.config;

import com.pyrosense.maintenance.application.port.out.InterventionRepositoryPort;
import com.pyrosense.maintenance.application.port.out.MaintenanceEventPublisherPort;
import com.pyrosense.maintenance.application.port.out.RiskScoreReevaluationPublisherPort;
import com.pyrosense.maintenance.application.usecase.AddInterventionCommentService;
import com.pyrosense.maintenance.application.usecase.CreateInterventionService;
import com.pyrosense.maintenance.application.usecase.GetInterventionService;
import com.pyrosense.maintenance.application.usecase.ManageInterventionService;
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
}
