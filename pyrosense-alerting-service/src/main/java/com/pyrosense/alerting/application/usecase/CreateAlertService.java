package com.pyrosense.alerting.application.usecase;

import com.pyrosense.alerting.application.port.in.CreateAlertUseCase;
import com.pyrosense.alerting.application.port.out.AlertEventPublisherPort;
import com.pyrosense.alerting.application.port.out.AlertRepositoryPort;
import com.pyrosense.alerting.domain.model.Alert;
import com.pyrosense.alerting.domain.model.DeduplicationKey;
import com.pyrosense.alerting.domain.model.SlaPolicy;
import com.pyrosense.shared.domain.DomainEvent;

import java.util.Optional;

public class CreateAlertService implements CreateAlertUseCase {

    private final AlertRepositoryPort repository;
    private final AlertEventPublisherPort eventPublisher;
    private final SlaPolicy slaPolicy;

    public CreateAlertService(AlertRepositoryPort repository,
                               AlertEventPublisherPort eventPublisher,
                               SlaPolicy slaPolicy) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.slaPolicy = slaPolicy;
    }

    @Override
    public Alert create(CreateAlertCommand command) {
        DeduplicationKey key = new DeduplicationKey(command.deviceId(), command.type());
        Optional<Alert> existing = repository.findActiveByDeduplicationKey(key);

        if (existing.isPresent()) {
            Alert alert = existing.get();
            alert.recordOccurrence();
            Alert saved = repository.save(alert);
            return saved;
        }

        Alert alert = Alert.create(
                command.tenantId(),
                command.deviceId(),
                command.type(),
                command.severity(),
                command.title(),
                command.description(),
                slaPolicy
        );

        Alert saved = repository.save(alert);
        for (DomainEvent event : saved.getDomainEvents()) {
            eventPublisher.publish(event);
        }
        saved.clearDomainEvents();
        return saved;
    }
}
