package com.pyrosense.analysis.application.port.out;

import com.pyrosense.shared.domain.DomainEvent;

public interface AnalysisEventPublisherPort {
    void publish(DomainEvent event);
}
