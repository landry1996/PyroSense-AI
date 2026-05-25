package com.pyrosense.scoring.domain.model;

import java.util.Objects;

public record RiskFactor(
        String name,
        String description,
        double weight,
        double normalizedValue,
        double contribution
) {
    public RiskFactor {
        Objects.requireNonNull(name);
        Objects.requireNonNull(description);
        if (weight < 0 || weight > 1.0) throw new IllegalArgumentException("Weight must be 0-1");
        if (normalizedValue < 0 || normalizedValue > 1.0) throw new IllegalArgumentException("Value must be 0-1");
    }

    public static RiskFactor of(String name, String description, double weight, double normalizedValue) {
        double contribution = weight * normalizedValue * 100.0;
        return new RiskFactor(name, description, weight, normalizedValue, contribution);
    }
}
