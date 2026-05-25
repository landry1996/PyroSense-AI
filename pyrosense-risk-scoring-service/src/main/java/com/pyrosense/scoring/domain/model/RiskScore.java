package com.pyrosense.scoring.domain.model;

public record RiskScore(int value) implements Comparable<RiskScore> {

    public RiskScore {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("Risk score must be between 0 and 100, got: " + value);
        }
    }

    public static RiskScore zero() {
        return new RiskScore(0);
    }

    public static RiskScore of(double rawValue) {
        int clamped = (int) Math.round(Math.max(0, Math.min(100, rawValue)));
        return new RiskScore(clamped);
    }

    public RiskLevel level() {
        return RiskLevel.fromScore(value);
    }

    public boolean isCritical() {
        return value >= 80;
    }

    public boolean isHigh() {
        return value >= 60;
    }

    @Override
    public int compareTo(RiskScore other) {
        return Integer.compare(this.value, other.value);
    }
}
