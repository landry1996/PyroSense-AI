package com.pyrosense.maintenance.application.port.out;

import com.pyrosense.shared.domain.DomainEvent;

public interface MaintenanceEventPublisherPort {
    void publish(DomainEvent event);
}
