package com.pyrosense.dashboard.domain.model;

import java.time.Instant;
import java.util.UUID;

public record PilotObservation(
        UUID id,
        UUID pilotId,
        String authorId,
        String authorName,
        ObservationType type,
        String content,
        String deviceId,
        String siteName,
        Instant createdAt
) {
    public enum ObservationType {
        FIELD_NOTE,
        INSTALLATION_REPORT,
        DAILY_MONITORING,
        FEEDBACK,
        GENERAL
    }

    public static PilotObservation create(UUID pilotId, String authorId, String authorName,
                                           ObservationType type, String content,
                                           String deviceId, String siteName) {
        return new PilotObservation(UUID.randomUUID(), pilotId, authorId, authorName,
                type, content, deviceId, siteName, Instant.now());
    }
}
