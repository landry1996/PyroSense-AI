package com.pyrosense.ingestion.domain.model;

public record SignalQualityScore(
        double score,
        QualityLevel level,
        String details
) {

    public enum QualityLevel {
        EXCELLENT,
        GOOD,
        DEGRADED,
        POOR,
        CRITICAL
    }

    private static final double EXCELLENT_THRESHOLD = 0.9;
    private static final double GOOD_THRESHOLD = 0.7;
    private static final double DEGRADED_THRESHOLD = 0.5;
    private static final double POOR_THRESHOLD = 0.3;

    public static SignalQualityScore fromRawValue(double signalQuality) {
        QualityLevel level;
        String details;
        if (signalQuality >= EXCELLENT_THRESHOLD) {
            level = QualityLevel.EXCELLENT;
            details = "Signal optimal";
        } else if (signalQuality >= GOOD_THRESHOLD) {
            level = QualityLevel.GOOD;
            details = "Signal acceptable";
        } else if (signalQuality >= DEGRADED_THRESHOLD) {
            level = QualityLevel.DEGRADED;
            details = "Signal degraded, measurements may be less accurate";
        } else if (signalQuality >= POOR_THRESHOLD) {
            level = QualityLevel.POOR;
            details = "Signal poor, measurements unreliable";
        } else {
            level = QualityLevel.CRITICAL;
            details = "Signal critical, data may be corrupted";
        }
        return new SignalQualityScore(signalQuality, level, details);
    }

    public boolean isBelowMinimum() {
        return score < POOR_THRESHOLD;
    }

    public boolean requiresAlert() {
        return level == QualityLevel.POOR || level == QualityLevel.CRITICAL;
    }
}
