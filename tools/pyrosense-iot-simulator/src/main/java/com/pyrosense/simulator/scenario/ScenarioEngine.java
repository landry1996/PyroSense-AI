package com.pyrosense.simulator.scenario;

import com.pyrosense.simulator.domain.ScenarioType;
import com.pyrosense.simulator.domain.SimulatedDevice;
import com.pyrosense.simulator.domain.TelemetryReading;

import java.time.Instant;
import java.util.Random;

public class ScenarioEngine {

    private final Random random = new Random();

    public TelemetryReading generate(SimulatedDevice device, ScenarioType scenario, int tick, Instant timestamp) {
        return switch (scenario) {
            case NORMAL -> generateNormal(device, tick, timestamp);
            case INSULATION_DEGRADATION_PROGRESSIVE -> generateInsulationDegradation(device, tick, timestamp);
            case LOOSE_CONNECTION -> generateLooseConnection(device, tick, timestamp);
            case MICRO_ARC_RECURRENT -> generateMicroArc(device, tick, timestamp);
            case OVERLOAD -> generateOverload(device, tick, timestamp);
            case TEMPERATURE_RISE -> generateTemperatureRise(device, tick, timestamp);
            case DEVICE_OFFLINE -> null;
        };
    }

    private TelemetryReading generateNormal(SimulatedDevice device, int tick, Instant timestamp) {
        return baseBuilder(device, timestamp)
                .rmsCurrent(10.0 + noise(0.5))
                .rmsVoltage(230.0 + noise(2.0))
                .activePower(2300.0 + noise(50.0))
                .reactivePower(200.0 + noise(20.0))
                .powerFactor(0.95 + noise(0.02))
                .thd(3.0 + noise(0.5))
                .temperatureCelsius(25.0 + noise(1.0))
                .hfNoiseLevel(0.1 + noise(0.02))
                .microArcCount(0)
                .transientCount(random.nextInt(2))
                .build();
    }

    private TelemetryReading generateInsulationDegradation(SimulatedDevice device, int tick, Instant timestamp) {
        double progression = Math.min(tick / 200.0, 1.0);
        double leakageCurrent = 0.5 * progression;
        double thdIncrease = 5.0 * progression;

        return baseBuilder(device, timestamp)
                .rmsCurrent(10.0 + leakageCurrent + noise(0.3))
                .rmsVoltage(230.0 + noise(2.0))
                .activePower(2300.0 + noise(50.0))
                .reactivePower(200.0 + 100.0 * progression + noise(20.0))
                .powerFactor(0.95 - 0.1 * progression + noise(0.01))
                .thd(3.0 + thdIncrease + noise(0.3))
                .temperatureCelsius(25.0 + 3.0 * progression + noise(0.5))
                .hfNoiseLevel(0.1 + 0.3 * progression + noise(0.02))
                .microArcCount(progression > 0.7 ? random.nextInt(3) : 0)
                .transientCount(random.nextInt(2) + (int) (3 * progression))
                .build();
    }

    private TelemetryReading generateLooseConnection(SimulatedDevice device, int tick, Instant timestamp) {
        boolean spike = random.nextDouble() < 0.15;
        double spikeMultiplier = spike ? 2.0 + random.nextDouble() * 3.0 : 1.0;
        double tempSpike = spike ? 15.0 + random.nextDouble() * 10.0 : 0.0;

        return baseBuilder(device, timestamp)
                .rmsCurrent(10.0 * spikeMultiplier + noise(0.5))
                .rmsVoltage(spike ? 230.0 - 20.0 * random.nextDouble() : 230.0 + noise(2.0))
                .activePower(2300.0 * (spike ? 0.7 : 1.0) + noise(50.0))
                .reactivePower(200.0 + noise(40.0))
                .powerFactor(spike ? 0.75 + noise(0.05) : 0.95 + noise(0.02))
                .thd(spike ? 8.0 + noise(2.0) : 3.0 + noise(0.5))
                .temperatureCelsius(25.0 + tempSpike + noise(1.0))
                .hfNoiseLevel(spike ? 0.5 + noise(0.1) : 0.1 + noise(0.02))
                .microArcCount(spike ? random.nextInt(5) + 1 : 0)
                .transientCount(spike ? random.nextInt(8) + 3 : random.nextInt(2))
                .build();
    }

    private TelemetryReading generateMicroArc(SimulatedDevice device, int tick, Instant timestamp) {
        boolean arcEvent = random.nextDouble() < 0.25;
        int arcs = arcEvent ? random.nextInt(10) + 3 : random.nextInt(2);

        return baseBuilder(device, timestamp)
                .rmsCurrent(10.0 + (arcEvent ? 2.0 : 0.0) + noise(0.5))
                .rmsVoltage(230.0 + noise(2.0))
                .activePower(2300.0 + noise(50.0))
                .reactivePower(200.0 + noise(20.0))
                .powerFactor(0.95 + noise(0.02))
                .thd(arcEvent ? 6.0 + noise(1.5) : 3.0 + noise(0.5))
                .temperatureCelsius(25.0 + (arcEvent ? 5.0 : 0.0) + noise(1.0))
                .hfNoiseLevel(arcEvent ? 0.6 + noise(0.15) : 0.1 + noise(0.02))
                .microArcCount(arcs)
                .transientCount(arcEvent ? random.nextInt(6) + 2 : random.nextInt(2))
                .build();
    }

    private TelemetryReading generateOverload(SimulatedDevice device, int tick, Instant timestamp) {
        double loadFactor = 1.5 + 0.5 * Math.sin(tick * 0.05);

        return baseBuilder(device, timestamp)
                .rmsCurrent(10.0 * loadFactor + noise(1.0))
                .rmsVoltage(230.0 - 5.0 * (loadFactor - 1.0) + noise(2.0))
                .activePower(2300.0 * loadFactor + noise(100.0))
                .reactivePower(200.0 * loadFactor + noise(30.0))
                .powerFactor(0.95 - 0.05 * (loadFactor - 1.0) + noise(0.01))
                .thd(3.0 + 4.0 * (loadFactor - 1.0) + noise(0.5))
                .temperatureCelsius(25.0 + 15.0 * (loadFactor - 1.0) + noise(1.0))
                .hfNoiseLevel(0.1 + 0.2 * (loadFactor - 1.0) + noise(0.03))
                .microArcCount(loadFactor > 1.8 ? random.nextInt(3) : 0)
                .transientCount(random.nextInt(4) + (int) (3 * (loadFactor - 1.0)))
                .build();
    }

    private TelemetryReading generateTemperatureRise(SimulatedDevice device, int tick, Instant timestamp) {
        double tempProgression = Math.min(tick / 150.0, 1.0);
        double temperature = 25.0 + 40.0 * tempProgression;

        return baseBuilder(device, timestamp)
                .rmsCurrent(10.0 + 2.0 * tempProgression + noise(0.5))
                .rmsVoltage(230.0 + noise(2.0))
                .activePower(2300.0 + 200.0 * tempProgression + noise(50.0))
                .reactivePower(200.0 + 50.0 * tempProgression + noise(20.0))
                .powerFactor(0.95 - 0.03 * tempProgression + noise(0.01))
                .thd(3.0 + 2.0 * tempProgression + noise(0.5))
                .temperatureCelsius(temperature + noise(0.5))
                .hfNoiseLevel(0.1 + 0.15 * tempProgression + noise(0.02))
                .microArcCount(tempProgression > 0.8 ? random.nextInt(4) : 0)
                .transientCount(random.nextInt(3) + (int) (2 * tempProgression))
                .build();
    }

    private TelemetryReading.Builder baseBuilder(SimulatedDevice device, Instant timestamp) {
        return TelemetryReading.builder()
                .tenantId(device.tenantId())
                .buildingId(device.buildingId())
                .panelId(device.panelId())
                .circuitId(device.circuitId())
                .deviceId(device.deviceId())
                .timestamp(timestamp);
    }

    private double noise(double amplitude) {
        return (random.nextGaussian()) * amplitude;
    }
}
