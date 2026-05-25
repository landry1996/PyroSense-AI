package com.pyrosense.scoring.domain.model;

import com.pyrosense.shared.id.DeviceId;

import java.time.Instant;
import java.util.Objects;

public record AnomalyInput(
        DeviceId deviceId,
        String anomalyType,
        double confidence,
        double deviationSigma,
        Instant detectedAt
) {
    public AnomalyInput {
        Objects.requireNonNull(deviceId);
        Objects.requireNonNull(anomalyType);
        Objects.requireNonNull(detectedAt);
        if (confidence < 0 || confidence > 1.0) throw new IllegalArgumentException("Confidence must be 0-1");
    }
}
