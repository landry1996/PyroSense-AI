package com.pyrosense.ingestion.domain.model.quality;

public record MissingDataRatio(
        int expectedMessages,
        int receivedMessages,
        double ratio
) {
    public static MissingDataRatio compute(int expected, int received) {
        if (expected <= 0) {
            return new MissingDataRatio(0, received, 0.0);
        }
        double ratio = 1.0 - ((double) Math.min(received, expected) / expected);
        return new MissingDataRatio(expected, received, Math.max(0, ratio));
    }

    public boolean isAcceptable() {
        return ratio <= 0.05;
    }

    public boolean isWarning() {
        return ratio > 0.05 && ratio <= 0.20;
    }

    public boolean isCritical() {
        return ratio > 0.20;
    }

    public double completenessPercent() {
        return (1.0 - ratio) * 100.0;
    }
}
