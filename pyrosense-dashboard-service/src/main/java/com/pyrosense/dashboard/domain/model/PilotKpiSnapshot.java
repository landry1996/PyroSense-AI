package com.pyrosense.dashboard.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PilotKpiSnapshot(
        UUID id,
        UUID pilotId,
        LocalDate date,
        int totalDevices,
        int activeDevices,
        int offlineDevices,
        double uptimePercent,
        double telemetryValidPercent,
        double avgSignalQuality,
        int alertsGenerated,
        int alertsConfirmed,
        int falsePositives,
        double falsePositiveRate,
        int incidentsOpen,
        int incidentsResolved,
        double avgLatencyMs,
        Instant computedAt
) {
    public static PilotKpiSnapshot compute(UUID pilotId, LocalDate date,
                                            int totalDevices, int activeDevices, int offlineDevices,
                                            double uptimePercent, double telemetryValidPercent,
                                            double avgSignalQuality,
                                            int alertsGenerated, int alertsConfirmed, int falsePositives,
                                            int incidentsOpen, int incidentsResolved,
                                            double avgLatencyMs) {
        double fpRate = alertsGenerated > 0
                ? (double) falsePositives / (alertsConfirmed + falsePositives) * 100.0
                : 0.0;
        return new PilotKpiSnapshot(UUID.randomUUID(), pilotId, date,
                totalDevices, activeDevices, offlineDevices,
                uptimePercent, telemetryValidPercent, avgSignalQuality,
                alertsGenerated, alertsConfirmed, falsePositives, fpRate,
                incidentsOpen, incidentsResolved, avgLatencyMs, Instant.now());
    }
}
