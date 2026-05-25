package com.pyrosense.alerting.config;

import com.pyrosense.alerting.application.port.out.AlertEventPublisherPort;
import com.pyrosense.alerting.application.port.out.AlertRepositoryPort;
import com.pyrosense.alerting.application.usecase.CreateAlertService;
import com.pyrosense.alerting.application.usecase.EscalateAlertsService;
import com.pyrosense.alerting.application.usecase.GetAlertService;
import com.pyrosense.alerting.application.usecase.ManageAlertService;
import com.pyrosense.alerting.domain.model.SlaPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AlertingProperties.class)
public class UseCaseConfig {

    @Bean
    public SlaPolicy slaPolicy(AlertingProperties props) {
        return new SlaPolicy(
                props.sla().criticalDeadline(),
                props.sla().warningDeadline(),
                props.sla().infoDeadline(),
                props.sla().escalationInterval()
        );
    }

    @Bean
    public CreateAlertService createAlertService(AlertRepositoryPort repository,
                                                  AlertEventPublisherPort eventPublisher,
                                                  SlaPolicy slaPolicy) {
        return new CreateAlertService(repository, eventPublisher, slaPolicy);
    }

    @Bean
    public ManageAlertService manageAlertService(AlertRepositoryPort repository,
                                                  AlertEventPublisherPort eventPublisher) {
        return new ManageAlertService(repository, eventPublisher);
    }

    @Bean
    public GetAlertService getAlertService(AlertRepositoryPort repository) {
        return new GetAlertService(repository);
    }

    @Bean
    public EscalateAlertsService escalateAlertsService(AlertRepositoryPort repository,
                                                        AlertEventPublisherPort eventPublisher,
                                                        SlaPolicy slaPolicy) {
        return new EscalateAlertsService(repository, eventPublisher, slaPolicy);
    }
}
