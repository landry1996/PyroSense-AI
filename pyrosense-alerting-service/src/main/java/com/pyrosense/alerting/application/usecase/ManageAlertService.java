package com.pyrosense.alerting.application.usecase;

import com.pyrosense.alerting.application.port.in.ManageAlertUseCase;
import com.pyrosense.alerting.application.port.out.AlertEventPublisherPort;
import com.pyrosense.alerting.application.port.out.AlertRepositoryPort;
import com.pyrosense.alerting.domain.model.Alert;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.UserId;

public class ManageAlertService implements ManageAlertUseCase {

    private final AlertRepositoryPort repository;
    private final AlertEventPublisherPort eventPublisher;

    public ManageAlertService(AlertRepositoryPort repository, AlertEventPublisherPort eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Alert acknowledge(AlertId alertId, UserId userId) {
        Alert alert = findOrThrow(alertId);
        alert.acknowledge(userId);
        return saveAndPublish(alert);
    }

    @Override
    public Alert assign(AlertId alertId, UserId assignee, UserId assignedBy) {
        Alert alert = findOrThrow(alertId);
        alert.assign(assignee, assignedBy);
        return saveAndPublish(alert);
    }

    @Override
    public Alert resolve(AlertId alertId, UserId userId, String resolutionNote) {
        Alert alert = findOrThrow(alertId);
        alert.resolve(userId, resolutionNote);
        return saveAndPublish(alert);
    }

    @Override
    public Alert markFalsePositive(AlertId alertId, UserId userId, String reason) {
        Alert alert = findOrThrow(alertId);
        alert.markFalsePositive(userId, reason);
        return saveAndPublish(alert);
    }

    @Override
    public Alert addComment(AlertId alertId, UserId author, String content) {
        Alert alert = findOrThrow(alertId);
        alert.addComment(author, content);
        return repository.save(alert);
    }

    private Alert findOrThrow(AlertId alertId) {
        return repository.findById(alertId)
                .orElseThrow(() -> new NotFoundException("Alert", alertId.value()));
    }

    private Alert saveAndPublish(Alert alert) {
        Alert saved = repository.save(alert);
        for (DomainEvent event : saved.getDomainEvents()) {
            eventPublisher.publish(event);
        }
        saved.clearDomainEvents();
        return saved;
    }
}
