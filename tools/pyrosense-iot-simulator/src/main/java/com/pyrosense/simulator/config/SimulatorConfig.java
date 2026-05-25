package com.pyrosense.simulator.config;

import java.time.Duration;

public record SimulatorConfig(
        String mqttHost,
        int mqttPort,
        String restBaseUrl,
        int tenantCount,
        int buildingsPerTenant,
        int devicesPerBuilding,
        Duration publishInterval,
        int timeAccelerationFactor,
        int simulationDurationDays,
        boolean useMqtt,
        boolean useRest
) {
    public static SimulatorConfig defaults() {
        return new SimulatorConfig(
                "localhost",
                1883,
                "http://localhost:8083",
                2,
                2,
                3,
                Duration.ofSeconds(5),
                1,
                1,
                true,
                false
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String mqttHost = "localhost";
        private int mqttPort = 1883;
        private String restBaseUrl = "http://localhost:8083";
        private int tenantCount = 2;
        private int buildingsPerTenant = 2;
        private int devicesPerBuilding = 3;
        private Duration publishInterval = Duration.ofSeconds(5);
        private int timeAccelerationFactor = 1;
        private int simulationDurationDays = 1;
        private boolean useMqtt = true;
        private boolean useRest = false;

        public Builder mqttHost(String v) { this.mqttHost = v; return this; }
        public Builder mqttPort(int v) { this.mqttPort = v; return this; }
        public Builder restBaseUrl(String v) { this.restBaseUrl = v; return this; }
        public Builder tenantCount(int v) { this.tenantCount = v; return this; }
        public Builder buildingsPerTenant(int v) { this.buildingsPerTenant = v; return this; }
        public Builder devicesPerBuilding(int v) { this.devicesPerBuilding = v; return this; }
        public Builder publishInterval(Duration v) { this.publishInterval = v; return this; }
        public Builder timeAccelerationFactor(int v) { this.timeAccelerationFactor = v; return this; }
        public Builder simulationDurationDays(int v) { this.simulationDurationDays = v; return this; }
        public Builder useMqtt(boolean v) { this.useMqtt = v; return this; }
        public Builder useRest(boolean v) { this.useRest = v; return this; }

        public SimulatorConfig build() {
            return new SimulatorConfig(mqttHost, mqttPort, restBaseUrl, tenantCount,
                    buildingsPerTenant, devicesPerBuilding, publishInterval,
                    timeAccelerationFactor, simulationDurationDays, useMqtt, useRest);
        }
    }
}
