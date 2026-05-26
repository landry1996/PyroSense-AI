package com.pyrosense.shared.event;

import java.util.UUID;

public interface IdempotentEventConsumer {

    boolean isAlreadyProcessed(UUID eventId);

    void markAsProcessed(UUID eventId);
}
