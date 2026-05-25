package com.pyrosense.analysis.domain.model;

import java.util.Objects;

public record StatisticalRange(
        double mean,
        double stdDev,
        double min,
        double max,
        double p5,
        double p25,
        double p50,
        double p75,
        double p95,
        long count
) {
    public StatisticalRange {
        if (stdDev < 0) throw new IllegalArgumentException("Standard deviation cannot be negative");
        if (count < 0) throw new IllegalArgumentException("Count cannot be negative");
    }

    public double zScore(double value) {
        if (stdDev == 0) return 0;
        return (value - mean) / stdDev;
    }

    public boolean isOutlier(double value, double threshold) {
        return Math.abs(zScore(value)) > threshold;
    }

    public StatisticalRange merge(StatisticalRange other) {
        Objects.requireNonNull(other);
        long totalCount = this.count + other.count;
        if (totalCount == 0) return this;

        double combinedMean = (this.mean * this.count + other.mean * other.count) / totalCount;
        double combinedVariance = ((this.count * (Math.pow(this.stdDev, 2) + Math.pow(this.mean - combinedMean, 2)))
                + (other.count * (Math.pow(other.stdDev, 2) + Math.pow(other.mean - combinedMean, 2)))) / totalCount;

        return new StatisticalRange(
                combinedMean,
                Math.sqrt(combinedVariance),
                Math.min(this.min, other.min),
                Math.max(this.max, other.max),
                (this.p5 + other.p5) / 2.0,
                (this.p25 + other.p25) / 2.0,
                (this.p50 + other.p50) / 2.0,
                (this.p75 + other.p75) / 2.0,
                (this.p95 + other.p95) / 2.0,
                totalCount
        );
    }
}
