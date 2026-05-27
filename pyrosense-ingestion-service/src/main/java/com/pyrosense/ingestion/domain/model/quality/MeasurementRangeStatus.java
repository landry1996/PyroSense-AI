package com.pyrosense.ingestion.domain.model.quality;

import java.util.List;

public record MeasurementRangeStatus(
        int totalReadings,
        int outOfRangeCount,
        List<String> outOfRangeFields,
        double outOfRangeRatio
) {
    public static MeasurementRangeStatus compute(int total, int outOfRange, List<String> fields) {
        double ratio = total > 0 ? (double) outOfRange / total : 0.0;
        return new MeasurementRangeStatus(total, outOfRange, fields, ratio);
    }

    public boolean hasIssues() {
        return outOfRangeCount > 0;
    }

    public boolean isCritical() {
        return outOfRangeRatio > 0.10;
    }
}
