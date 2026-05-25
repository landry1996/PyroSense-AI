package com.pyrosense.scoring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pyrosense.scoring.weights")
public record ScoringProperties(
        double microArcWeight,
        double thdDriftWeight,
        double temperatureTrendWeight,
        double transientWeight,
        double hfNoiseWeight,
        double recencyDecayFactor,
        double repetitionBoostFactor,
        double deviceReliabilityWeight,
        int historyWindowDays,
        int recencyWindowHours
) {
    public ScoringProperties {
        if (microArcWeight <= 0) microArcWeight = 0.30;
        if (thdDriftWeight <= 0) thdDriftWeight = 0.20;
        if (temperatureTrendWeight <= 0) temperatureTrendWeight = 0.20;
        if (transientWeight <= 0) transientWeight = 0.10;
        if (hfNoiseWeight <= 0) hfNoiseWeight = 0.10;
        if (recencyDecayFactor <= 0 || recencyDecayFactor > 1) recencyDecayFactor = 0.95;
        if (repetitionBoostFactor < 1) repetitionBoostFactor = 1.3;
        if (deviceReliabilityWeight <= 0) deviceReliabilityWeight = 0.10;
        if (historyWindowDays <= 0) historyWindowDays = 7;
        if (recencyWindowHours <= 0) recencyWindowHours = 24;
    }
}
