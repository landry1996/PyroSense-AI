package com.pyrosense.analysis.domain.detection;

import com.pyrosense.analysis.domain.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TemperatureTrendDetector {

    private TemperatureTrendDetector() {}

    public static List<SignalAnomaly> detect(SignalWindow window, DetectionThresholds thresholds, Instant now) {
        double[] temps = window.valuesFor(SignalFeature.TEMPERATURE);
        if (temps.length == 0) return List.of();

        double latestTemp = temps[temps.length - 1];
        if (latestTemp > thresholds.temperatureMaxCelsius()) {
            return List.of(createAnomaly(window, latestTemp, thresholds.temperatureMaxCelsius(),
                    (latestTemp - thresholds.temperatureMaxCelsius()) / 10.0, now));
        }

        Duration windowDuration = Duration.between(window.startTime(), window.endTime());
        if (windowDuration.isZero()) return List.of();

        double firstTemp = temps[0];
        double riseRate = (latestTemp - firstTemp) / (windowDuration.toMinutes() / 60.0);

        if (riseRate > thresholds.temperatureRiseRatePerHour()) {
            double confidence = Math.min(riseRate / (thresholds.temperatureRiseRatePerHour() * 2), 1.0);
            return List.of(createAnomaly(window, latestTemp, firstTemp, riseRate, confidence, now));
        }

        return List.of();
    }

    private static SignalAnomaly createAnomaly(SignalWindow window, double current, double baseline,
                                               double deviation, Instant now) {
        return new SignalAnomaly(UUID.randomUUID(), window.deviceId(), SignalFeature.TEMPERATURE,
                AnomalyType.TEMPERATURE_RISING, current, baseline, deviation, 0.8, now);
    }

    private static SignalAnomaly createAnomaly(SignalWindow window, double current, double baseline,
                                               double deviation, double confidence, Instant now) {
        return new SignalAnomaly(UUID.randomUUID(), window.deviceId(), SignalFeature.TEMPERATURE,
                AnomalyType.TEMPERATURE_RISING, current, baseline, deviation, confidence, now);
    }
}
