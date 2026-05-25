package com.pyrosense.maintenance.config;

import com.pyrosense.maintenance.application.port.out.InterventionRepositoryPort;
import com.pyrosense.maintenance.application.port.out.MaintenanceEventPublisherPort;
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
                                                               MaintenanceEventPublisherPort eventPublisher) {
        return new ManageInterventionService(repository, eventPublisher);
    }

    @Bean
    public GetInterventionService getInterventionService(InterventionRepositoryPort repository) {
        return new GetInterventionService(repository);
    }
}
