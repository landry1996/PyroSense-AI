package com.pyrosense.device.application.port.out;

import com.pyrosense.shared.domain.DomainEvent;

import java.util.List;

public interface DeviceEventPublisherPort {

    void publish(List<DomainEvent> events);
}
