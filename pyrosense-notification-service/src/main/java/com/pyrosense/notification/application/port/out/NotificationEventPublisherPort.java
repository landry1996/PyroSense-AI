package com.pyrosense.notification.application.port.out;

import com.pyrosense.shared.domain.DomainEvent;

public interface NotificationEventPublisherPort {

    void publish(DomainEvent event);
}
