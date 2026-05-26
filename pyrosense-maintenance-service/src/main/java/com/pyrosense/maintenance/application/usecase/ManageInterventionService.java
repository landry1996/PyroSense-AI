package com.pyrosense.maintenance.application.usecase;

import com.pyrosense.maintenance.application.port.in.ManageInterventionUseCase;
import com.pyrosense.maintenance.application.port.out.InterventionRepositoryPort;
import com.pyrosense.maintenance.application.port.out.MaintenanceEventPublisherPort;
import com.pyrosense.maintenance.application.port.out.RiskScoreReevaluationPublisherPort;
import com.pyrosense.maintenance.domain.event.*;
import com.pyrosense.maintenance.domain.model.FieldDiagnostic;
import com.pyrosense.maintenance.domain.model.Intervention;
import com.pyrosense.maintenance.domain.model.InterventionResult;
import com.pyrosense.maintenance.domain.model.RiskImpact;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.util.ClockProvider;

import java.time.Instant;
import java.util.UUID;

public class ManageInterventionService implements ManageInterventionUseCase {

    private final InterventionRepositoryPort repository;
    private final MaintenanceEventPublisherPort eventPublisher;
    private final RiskScoreReevaluationPublisherPort riskReevaluationPublisher;

    public ManageInterventionService(InterventionRepositoryPort repository,
                                     MaintenanceEventPublisherPort eventPublisher,
                                     RiskScoreReevaluationPublisherPort riskReevaluationPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.riskReevaluationPublisher = riskReevaluationPublisher;
    }

    @Override
    public Intervention schedule(UUID interventionId, Instant scheduledAt) {
        Intervention intervention = findOrThrow(interventionId);
        intervention.schedule(scheduledAt);
        Intervention saved = repository.save(intervention);

        eventPublisher.publish(new MaintenanceInterventionPlannedEvent(
                UUID.randomUUID(), ClockProvider.now(),
                saved.getId(), saved.getTenantId(), scheduledAt));

        return saved;
    }

    @Override
    public Intervention assign(UUID interventionId, UserId electricianId, Instant scheduledAt) {
        Intervention intervention = findOrThrow(interventionId);
        intervention.assign(electricianId, scheduledAt);
        Intervention saved = repository.save(intervention);

        eventPublisher.publish(new MaintenanceInterventionAssignedEvent(
                UUID.randomUUID(), ClockProvider.now(),
                saved.getId(), saved.getTenantId(), electricianId, scheduledAt));

        return saved;
    }

    @Override
    public Intervention start(UUID interventionId) {
        Intervention intervention = findOrThrow(interventionId);
        intervention.start();
        Intervention saved = repository.save(intervention);

        eventPublisher.publish(new MaintenanceInterventionStartedEvent(
                UUID.randomUUID(), ClockProvider.now(),
                saved.getId(), saved.getTenantId(), saved.getDeviceId()));

        return saved;
    }

    @Override
    public Intervention addDiagnostic(UUID interventionId, FieldDiagnostic diagnostic) {
        Intervention intervention = findOrThrow(interventionId);
        intervention.addDiagnostic(diagnostic);
        return repository.save(intervention);
    }

    @Override
    public Intervention complete(UUID interventionId, InterventionResult result) {
        Intervention intervention = findOrThrow(interventionId);
        intervention.complete(result);
        Intervention saved = repository.save(intervention);

        publishCompletionEvent(saved);
        publishResultSpecificEvent(saved);

        riskReevaluationPublisher.requestReevaluation(
                saved.getTenantId(), saved.getDeviceId(), saved.getId());

        return saved;
    }

    @Override
    public Intervention recordRiskImpact(UUID interventionId, RiskImpact riskImpact) {
        Intervention intervention = findOrThrow(interventionId);
        intervention.recordRiskImpact(riskImpact);
        return repository.save(intervention);
    }

    @Override
    public Intervention cancel(UUID interventionId, String reason) {
        Intervention intervention = findOrThrow(interventionId);
        intervention.cancel(reason);
        Intervention saved = repository.save(intervention);

        eventPublisher.publish(new MaintenanceInterventionCancelledEvent(
                UUID.randomUUID(), ClockProvider.now(),
                saved.getId(), saved.getTenantId(), reason));

        return saved;
    }

    private Intervention findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Intervention", id.toString()));
    }

    private void publishCompletionEvent(Intervention intervention) {
        RiskImpact impact = intervention.getRiskImpact();
        eventPublisher.publish(new MaintenanceInterventionCompletedEvent(
                UUID.randomUUID(),
                ClockProvider.now(),
                intervention.getId(),
                intervention.getTenantId(),
                intervention.getDeviceId(),
                intervention.getResult(),
                impact != null ? impact.riskScoreBefore() : null,
                impact != null ? impact.riskScoreAfter() : null
        ));
    }

    private void publishResultSpecificEvent(Intervention intervention) {
        String diagnosticObs = intervention.getDiagnostic() != null
                ? intervention.getDiagnostic().observations() : "";

        if (intervention.isDefectConfirmed()) {
            eventPublisher.publish(new ElectricalDefectConfirmedEvent(
                    UUID.randomUUID(),
                    ClockProvider.now(),
                    intervention.getId(),
                    intervention.getTenantId(),
                    intervention.getSourceAlertId(),
                    intervention.getDeviceId(),
                    intervention.getResult(),
                    diagnosticObs
            ));
        } else if (intervention.isFalsePositive()) {
            eventPublisher.publish(new FalsePositiveConfirmedEvent(
                    UUID.randomUUID(),
                    ClockProvider.now(),
                    intervention.getId(),
                    intervention.getTenantId(),
                    intervention.getSourceAlertId(),
                    intervention.getDeviceId(),
                    diagnosticObs
            ));
        }
    }
}
