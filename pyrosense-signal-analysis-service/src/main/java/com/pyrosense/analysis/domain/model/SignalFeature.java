package com.pyrosense.analysis.domain.model;

public enum SignalFeature {

    RMS_CURRENT("rms_current"),
    RMS_VOLTAGE("rms_voltage"),
    ACTIVE_POWER("active_power"),
    REACTIVE_POWER("reactive_power"),
    POWER_FACTOR("power_factor"),
    THD("thd"),
    TEMPERATURE("temperature_celsius"),
    HF_NOISE("hf_noise_level"),
    MICRO_ARC_COUNT("micro_arc_count"),
    TRANSIENT_COUNT("transient_count");

    private final String metricName;

    SignalFeature(String metricName) {
        this.metricName = metricName;
    }

    public String metricName() {
        return metricName;
    }

    public static SignalFeature fromMetricName(String name) {
        for (SignalFeature f : values()) {
            if (f.metricName.equals(name)) return f;
        }
        throw new IllegalArgumentException("Unknown metric: " + name);
    }
}
