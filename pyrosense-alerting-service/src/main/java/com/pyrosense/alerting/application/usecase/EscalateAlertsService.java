package com.pyrosense.alerting.application.usecase;

import com.pyrosense.alerting.application.port.in.EscalateAlertsUseCase;
import com.pyrosense.alerting.application.port.out.AlertEventPublisherPort;
import com.pyrosense.alerting.application.port.out.AlertRepositoryPort;
import com.pyrosense.alerting.domain.model.Alert;
import com.pyrosense.alerting.domain.model.SlaPolicy;
import com.pyrosense.shared.domain.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EscalateAlertsService implements EscalateAlertsUseCase {

    private static final Logger log = LoggerFactory.getLogger(EscalateAlertsService.class);

    private final AlertRepositoryPort repository;
    private final AlertEventPublisherPort eventPublisher;
    private final SlaPolicy slaPolicy;

    public EscalateAlertsService(AlertRepositoryPort repository,
                                  AlertEventPublisherPort eventPublisher,
                                  SlaPolicy slaPolicy) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.slaPolicy = slaPolicy;
    }

    @Override
    public int escalateOverdueAlerts() {
        List<Alert> candidates = repository.findEscalationCandidates();
        int escalated = 0;

        for (Alert alert : candidates) {
            if (alert.shouldEscalate(slaPolicy.escalationInterval())) {
                alert.escalate();
                Alert saved = repository.save(alert);
                for (DomainEvent event : saved.getDomainEvents()) {
                    eventPublisher.publish(event);
                }
                saved.clearDomainEvents();
                escalated++;
                log.info("Escalated alert {} to level {}", alert.getId(), alert.escalationLevel());
            }
        }
        return escalated;
    }
}
