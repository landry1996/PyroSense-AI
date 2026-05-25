package com.pyrosense.shared.valueobject;

import com.pyrosense.shared.domain.ValueObject;

/**
 * Risk score between 0 and 100.
 * Maps to alert severity: INFO [0,50), WARNING [50,80), CRITICAL [80,100].
 */
public record RiskScore(int value) implements ValueObject, Comparable<RiskScore> {

    public static final int MIN = 0;
    public static final int MAX = 100;
    public static final int WARNING_THRESHOLD = 50;
    public static final int CRITICAL_THRESHOLD = 80;

    public RiskScore {
        if (value < MIN || value > MAX) {
            throw new IllegalArgumentException(
                    "RiskScore must be between " + MIN + " and " + MAX + ", got: " + value);
        }
    }

    public static RiskScore of(int value) {
        return new RiskScore(value);
    }

    public static RiskScore zero() {
        return new RiskScore(MIN);
    }

    public static RiskScore maximum() {
        return new RiskScore(MAX);
    }

    public AlertSeverity toSeverity() {
        if (value >= CRITICAL_THRESHOLD) return AlertSeverity.CRITICAL;
        if (value >= WARNING_THRESHOLD) return AlertSeverity.WARNING;
        return AlertSeverity.INFO;
    }

    public boolean isCritical() {
        return value >= CRITICAL_THRESHOLD;
    }

    public boolean isWarning() {
        return value >= WARNING_THRESHOLD && value < CRITICAL_THRESHOLD;
    }

    public boolean isAboveThreshold(int threshold) {
        return value >= threshold;
    }

    @Override
    public int compareTo(RiskScore other) {
        return Integer.compare(this.value, other.value);
    }
}
