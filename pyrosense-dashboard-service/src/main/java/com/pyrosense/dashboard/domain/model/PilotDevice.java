package com.pyrosense.dashboard.domain.model;

import java.time.Instant;
import java.util.UUID;

public record PilotDevice(
        UUID id,
        UUID pilotId,
        String deviceId,
        String serialNumber,
        String siteName,
        String circuitDescription,
        PilotDeviceStatus status,
        Instant installedAt,
        Instant removedAt,
        String installationNotes
) {
    public enum PilotDeviceStatus {
        PLANNED,
        INSTALLED,
        ACTIVE,
        OFFLINE,
        REMOVED
    }

    public static PilotDevice plan(UUID pilotId, String deviceId, String serialNumber,
                                    String siteName, String circuitDescription) {
        return new PilotDevice(UUID.randomUUID(), pilotId, deviceId, serialNumber,
                siteName, circuitDescription, PilotDeviceStatus.PLANNED,
                null, null, null);
    }

    public PilotDevice markInstalled(String notes) {
        return new PilotDevice(id, pilotId, deviceId, serialNumber, siteName,
                circuitDescription, PilotDeviceStatus.INSTALLED,
                Instant.now(), null, notes);
    }

    public PilotDevice markActive() {
        return new PilotDevice(id, pilotId, deviceId, serialNumber, siteName,
                circuitDescription, PilotDeviceStatus.ACTIVE,
                installedAt, null, installationNotes);
    }

    public PilotDevice markRemoved() {
        return new PilotDevice(id, pilotId, deviceId, serialNumber, siteName,
                circuitDescription, PilotDeviceStatus.REMOVED,
                installedAt, Instant.now(), installationNotes);
    }
}
