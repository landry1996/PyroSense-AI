package com.pyrosense.dashboard.domain.model;

import java.time.Instant;
import java.util.List;

public record TelemetryQualityReport(
        String deviceId,
        List<HourlyCount> receivedPerHour,
        List<HourlyCount> rejectedPerHour,
        List<RejectionReason> rejectionReasons,
        List<QualityPoint> signalQualityTrend,
        List<OfflinePeriod> offlinePeriods
) {
    public record HourlyCount(Instant hour, int count) {}

    public record RejectionReason(String reason, int count) {}

    public record QualityPoint(Instant timestamp, double quality) {}

    public record OfflinePeriod(Instant start, Instant end, long durationMinutes) {}
}
