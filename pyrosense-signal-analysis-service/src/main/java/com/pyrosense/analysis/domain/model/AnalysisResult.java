package com.pyrosense.analysis.domain.model;

import com.pyrosense.shared.id.DeviceId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AnalysisResult(
        UUID id,
        DeviceId deviceId,
        Instant analyzedAt,
        List<SignalAnomaly> anomalies,
        double aggregateRiskScore,
        boolean baselineAvailable
) {
    public AnalysisResult {
        Objects.requireNonNull(id);
        Objects.requireNonNull(deviceId);
        Objects.requireNonNull(analyzedAt);
        anomalies = List.copyOf(anomalies);
        if (aggregateRiskScore < 0 || aggregateRiskScore > 100) {
            throw new IllegalArgumentException("Aggregate risk score must be between 0 and 100");
        }
    }

    public boolean hasAnomalies() {
        return !anomalies.isEmpty();
    }

    public int anomalyCount() {
        return anomalies.size();
    }

    public static AnalysisResult noAnomaly(DeviceId deviceId, Instant analyzedAt, boolean baselineAvailable) {
        return new AnalysisResult(UUID.randomUUID(), deviceId, analyzedAt, List.of(), 0.0, baselineAvailable);
    }
}
