package com.pyrosense.analysis.domain.model;

import com.pyrosense.shared.id.DeviceId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SignalAnomaly(
        UUID id,
        DeviceId deviceId,
        SignalFeature feature,
        AnomalyType type,
        double currentValue,
        double baselineValue,
        double deviationSigma,
        double confidence,
        Instant detectedAt
) {
    public SignalAnomaly {
        Objects.requireNonNull(id);
        Objects.requireNonNull(deviceId);
        Objects.requireNonNull(feature);
        Objects.requireNonNull(type);
        Objects.requireNonNull(detectedAt);
        if (confidence < 0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0 and 1");
        }
    }

    public double weightedScore() {
        return type.baseWeight() * confidence * Math.min(Math.abs(deviationSigma) / 3.0, 1.0);
    }
}
