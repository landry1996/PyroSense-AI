package com.pyrosense.ingestion.domain.validation;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class TelemetryValidator {

    private static final double MAX_VOLTAGE = 1000.0;
    private static final double MAX_CURRENT = 500.0;
    private static final double MAX_POWER = 500_000.0;
    private static final double MAX_TEMPERATURE = 200.0;
    private static final double MAX_THD = 100.0;
    private static final int MAX_SAMPLING_WINDOW_MS = 60_000;
    private static final Duration MAX_CLOCK_DRIFT = Duration.ofMinutes(5);

    private TelemetryValidator() {}

    public static List<String> validate(double rmsVoltage, double rmsCurrent, double activePower,
                                         double reactivePower, double powerFactor, double thd,
                                         double temperatureCelsius, double hfNoiseLevel,
                                         int microArcCount, int transientCount,
                                         int samplingWindowMs, Instant timestamp) {
        var violations = new ArrayList<String>();

        if (rmsVoltage < 0 || rmsVoltage > MAX_VOLTAGE)
            violations.add("rmsVoltage must be between 0 and " + MAX_VOLTAGE);
        if (rmsCurrent < 0 || rmsCurrent > MAX_CURRENT)
            violations.add("rmsCurrent must be between 0 and " + MAX_CURRENT);
        if (activePower < 0 || activePower > MAX_POWER)
            violations.add("activePower must be between 0 and " + MAX_POWER);
        if (reactivePower < 0 || reactivePower > MAX_POWER)
            violations.add("reactivePower must be between 0 and " + MAX_POWER);
        if (powerFactor < 0 || powerFactor > 1.0)
            violations.add("powerFactor must be between 0 and 1.0");
        if (thd < 0 || thd > MAX_THD)
            violations.add("thd must be between 0 and " + MAX_THD);
        if (temperatureCelsius < -40 || temperatureCelsius > MAX_TEMPERATURE)
            violations.add("temperatureCelsius must be between -40 and " + MAX_TEMPERATURE);
        if (hfNoiseLevel < 0 || hfNoiseLevel > 10.0)
            violations.add("hfNoiseLevel must be between 0 and 10.0");
        if (microArcCount < 0)
            violations.add("microArcCount must be >= 0");
        if (transientCount < 0)
            violations.add("transientCount must be >= 0");
        if (samplingWindowMs <= 0 || samplingWindowMs > MAX_SAMPLING_WINDOW_MS)
            violations.add("samplingWindowMs must be between 1 and " + MAX_SAMPLING_WINDOW_MS);
        if (timestamp == null)
            violations.add("timestamp must not be null");
        else if (timestamp.isAfter(Instant.now().plus(MAX_CLOCK_DRIFT)))
            violations.add("timestamp is too far in the future");

        return violations;
    }
}
