package com.pyrosense.ingestion.application.port.out;

import com.pyrosense.shared.domain.DomainEvent;

import java.util.List;

public interface TelemetryEventPublisherPort {

    void publish(DomainEvent event);

    void publishToDlq(String topic, String key, String payload, String reason);
}
