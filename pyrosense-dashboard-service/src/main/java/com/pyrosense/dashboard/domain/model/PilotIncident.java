package com.pyrosense.dashboard.domain.model;

import java.time.Instant;
import java.util.UUID;

public record PilotIncident(
        UUID id,
        UUID pilotId,
        String reportedBy,
        IncidentSeverity severity,
        IncidentCategory category,
        String title,
        String description,
        String deviceId,
        String siteName,
        String resolution,
        IncidentStatus status,
        Instant reportedAt,
        Instant resolvedAt
) {
    public enum IncidentSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum IncidentCategory {
        HARDWARE_FAILURE,
        FIRMWARE_BUG,
        CONNECTIVITY,
        FALSE_POSITIVE,
        REAL_DEFECT_DETECTED,
        SECURITY,
        SITE_ACCESS,
        OTHER
    }

    public enum IncidentStatus {
        OPEN, INVESTIGATING, RESOLVED, CLOSED
    }

    public static PilotIncident report(UUID pilotId, String reportedBy,
                                        IncidentSeverity severity, IncidentCategory category,
                                        String title, String description,
                                        String deviceId, String siteName) {
        return new PilotIncident(UUID.randomUUID(), pilotId, reportedBy,
                severity, category, title, description, deviceId, siteName,
                null, IncidentStatus.OPEN, Instant.now(), null);
    }

    public PilotIncident resolve(String resolution) {
        return new PilotIncident(id, pilotId, reportedBy, severity, category,
                title, description, deviceId, siteName,
                resolution, IncidentStatus.RESOLVED, reportedAt, Instant.now());
    }
}
