package com.pyrosense.scoring.application.port.out;

import com.pyrosense.shared.domain.DomainEvent;

public interface ScoringEventPublisherPort {
    void publish(DomainEvent event);
}
