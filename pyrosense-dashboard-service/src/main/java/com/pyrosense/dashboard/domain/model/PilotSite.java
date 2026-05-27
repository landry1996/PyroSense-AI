package com.pyrosense.dashboard.domain.model;

import java.time.Instant;
import java.util.UUID;

public record PilotSite(
        UUID id,
        UUID pilotId,
        String name,
        String address,
        String contactName,
        String contactPhone,
        String notes,
        Instant createdAt
) {
    public static PilotSite create(UUID pilotId, String name, String address,
                                    String contactName, String contactPhone, String notes) {
        return new PilotSite(UUID.randomUUID(), pilotId, name, address,
                contactName, contactPhone, notes, Instant.now());
    }
}
