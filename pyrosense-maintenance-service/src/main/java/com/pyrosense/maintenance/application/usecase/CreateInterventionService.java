package com.pyrosense.maintenance.application.usecase;

import com.pyrosense.maintenance.application.port.in.CreateInterventionUseCase;
import com.pyrosense.maintenance.application.port.out.InterventionRepositoryPort;
import com.pyrosense.maintenance.application.port.out.MaintenanceEventPublisherPort;
import com.pyrosense.maintenance.domain.event.MaintenanceInterventionCreatedEvent;
import com.pyrosense.maintenance.domain.model.Intervention;
import com.pyrosense.maintenance.domain.model.InterventionPriority;
import com.pyrosense.maintenance.domain.model.InterventionType;
import com.pyrosense.shared.exception.BusinessException;
import com.pyrosense.shared.exception.ErrorCode;
import com.pyrosense.shared.util.ClockProvider;

import java.util.UUID;

public class CreateInterventionService implements CreateInterventionUseCase {

    private final InterventionRepositoryPort repository;
    private final MaintenanceEventPublisherPort eventPublisher;

    public CreateInterventionService(InterventionRepositoryPort repository,
                                     MaintenanceEventPublisherPort eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Intervention createFromAlert(CreateInterventionCommand command) {
        repository.findBySourceAlertId(command.alertId()).ifPresent(existing -> {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "An intervention already exists for alert " + command.alertId());
        });

        InterventionType type = mapAlertSeverityToType(command.severity());
        InterventionPriority priority = mapAlertSeverityToPriority(command.severity());

        var intervention = new Intervention(
                UUID.randomUUID(),
                command.tenantId(),
                command.alertId(),
                command.deviceId(),
                type,
                priority,
                command.description()
        );

        Intervention saved = repository.save(intervention);

        eventPublisher.publish(new MaintenanceInterventionCreatedEvent(
                UUID.randomUUID(),
                ClockProvider.now(),
                saved.getId(),
                saved.getTenantId(),
                saved.getSourceAlertId(),
                saved.getDeviceId(),
                saved.getType(),
                saved.getPriority()
        ));

        return saved;
    }

    private InterventionType mapAlertSeverityToType(String severity) {
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> InterventionType.EMERGENCY;
            case "WARNING" -> InterventionType.PREVENTIVE;
            default -> InterventionType.PREDICTIVE;
        };
    }

    private InterventionPriority mapAlertSeverityToPriority(String severity) {
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> InterventionPriority.URGENT;
            case "WARNING" -> InterventionPriority.HIGH;
            default -> InterventionPriority.MEDIUM;
        };
    }
}
