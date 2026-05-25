package com.pyrosense.alerting.application.port.out;

import com.pyrosense.shared.domain.DomainEvent;

public interface AlertEventPublisherPort {

    void publish(DomainEvent event);
}
