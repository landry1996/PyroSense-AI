package com.pyrosense.shared.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Contract for all domain events in the PyroSense platform.
 * Every event carries a unique ID, timestamp, and type identifier.
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();

    String eventType();
}
