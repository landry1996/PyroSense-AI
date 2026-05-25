package com.pyrosense.simulator.domain;

import java.time.Instant;
import java.util.UUID;

public record TelemetryReading(
        String tenantId,
        String buildingId,
        String panelId,
        String circuitId,
        String deviceId,
        Instant timestamp,
        double rmsCurrent,
        double rmsVoltage,
        double activePower,
        double reactivePower,
        double powerFactor,
        double thd,
        double temperatureCelsius,
        double hfNoiseLevel,
        int microArcCount,
        int transientCount
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String tenantId;
        private String buildingId;
        private String panelId;
        private String circuitId;
        private String deviceId;
        private Instant timestamp = Instant.now();
        private double rmsCurrent = 10.0;
        private double rmsVoltage = 230.0;
        private double activePower = 2300.0;
        private double reactivePower = 200.0;
        private double powerFactor = 0.95;
        private double thd = 3.0;
        private double temperatureCelsius = 25.0;
        private double hfNoiseLevel = 0.1;
        private int microArcCount = 0;
        private int transientCount = 0;

        public Builder tenantId(String v) { this.tenantId = v; return this; }
        public Builder buildingId(String v) { this.buildingId = v; return this; }
        public Builder panelId(String v) { this.panelId = v; return this; }
        public Builder circuitId(String v) { this.circuitId = v; return this; }
        public Builder deviceId(String v) { this.deviceId = v; return this; }
        public Builder timestamp(Instant v) { this.timestamp = v; return this; }
        public Builder rmsCurrent(double v) { this.rmsCurrent = v; return this; }
        public Builder rmsVoltage(double v) { this.rmsVoltage = v; return this; }
        public Builder activePower(double v) { this.activePower = v; return this; }
        public Builder reactivePower(double v) { this.reactivePower = v; return this; }
        public Builder powerFactor(double v) { this.powerFactor = v; return this; }
        public Builder thd(double v) { this.thd = v; return this; }
        public Builder temperatureCelsius(double v) { this.temperatureCelsius = v; return this; }
        public Builder hfNoiseLevel(double v) { this.hfNoiseLevel = v; return this; }
        public Builder microArcCount(int v) { this.microArcCount = v; return this; }
        public Builder transientCount(int v) { this.transientCount = v; return this; }

        public TelemetryReading build() {
            return new TelemetryReading(tenantId, buildingId, panelId, circuitId, deviceId,
                    timestamp, rmsCurrent, rmsVoltage, activePower, reactivePower,
                    powerFactor, thd, temperatureCelsius, hfNoiseLevel, microArcCount, transientCount);
        }
    }
}
