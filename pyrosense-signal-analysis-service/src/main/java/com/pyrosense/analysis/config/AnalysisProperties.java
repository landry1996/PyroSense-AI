package com.pyrosense.analysis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pyrosense.analysis.thresholds")
public record AnalysisProperties(
        double zScoreThreshold,
        double thdMaxPercent,
        double temperatureMaxCelsius,
        double temperatureRiseRatePerHour,
        int microArcRecurrenceThreshold,
        int transientCountThreshold,
        double hfNoiseMaxDb,
        double powerFactorMin,
        double driftPercentThreshold,
        double exponentialSmoothingAlpha,
        int baselineMinimumSamples
) {
    public AnalysisProperties {
        if (zScoreThreshold <= 0) zScoreThreshold = 3.0;
        if (thdMaxPercent <= 0) thdMaxPercent = 8.0;
        if (temperatureMaxCelsius <= 0) temperatureMaxCelsius = 85.0;
        if (temperatureRiseRatePerHour <= 0) temperatureRiseRatePerHour = 5.0;
        if (microArcRecurrenceThreshold <= 0) microArcRecurrenceThreshold = 3;
        if (transientCountThreshold <= 0) transientCountThreshold = 10;
        if (hfNoiseMaxDb == 0) hfNoiseMaxDb = -40.0;
        if (powerFactorMin <= 0) powerFactorMin = 0.85;
        if (driftPercentThreshold <= 0) driftPercentThreshold = 15.0;
        if (exponentialSmoothingAlpha <= 0 || exponentialSmoothingAlpha >= 1) exponentialSmoothingAlpha = 0.3;
        if (baselineMinimumSamples <= 0) baselineMinimumSamples = 100;
    }
}
