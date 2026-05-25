package com.pyrosense.reporting.application.port.out;

import com.pyrosense.shared.domain.DomainEvent;

public interface ReportEventPublisherPort {
    void publish(DomainEvent event);
}
