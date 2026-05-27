package com.pyrosense.dashboard.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PilotDashboard(
        UUID pilotId,
        String pilotName,
        String status,
        List<PilotDeviceSummary> devices,
        IncidentsSummary incidentsSummary,
        PilotKpiSnapshot kpiSnapshot
) {
    public record PilotDeviceSummary(
            String deviceId,
            String serialNumber,
            String status,
            double signalQuality,
            String dataQualityGrade,
            Instant lastHeartbeat,
            String installationStatus
    ) {}

    public record IncidentsSummary(int open, int investigating, int resolved, int total) {}
}
