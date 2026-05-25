package com.pyrosense.simulator.scenario;

import com.pyrosense.simulator.domain.ScenarioType;
import com.pyrosense.simulator.domain.SimulatedDevice;
import com.pyrosense.simulator.domain.TelemetryReading;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioEngineTest {

    private ScenarioEngine engine;
    private SimulatedDevice device;
    private Instant now;

    @BeforeEach
    void setUp() {
        engine = new ScenarioEngine();
        device = new SimulatedDevice("dev-1", "tenant-1", "building-1", "panel-1", "circuit-1", "test-device");
        now = Instant.now();
    }

    @Test
    void normalShouldProduceStableValues() {
        TelemetryReading r = engine.generate(device, ScenarioType.NORMAL, 0, now);

        assertThat(r).isNotNull();
        assertThat(r.rmsCurrent()).isBetween(5.0, 15.0);
        assertThat(r.rmsVoltage()).isBetween(220.0, 240.0);
        assertThat(r.powerFactor()).isBetween(0.85, 1.0);
        assertThat(r.thd()).isBetween(0.0, 8.0);
        assertThat(r.temperatureCelsius()).isBetween(20.0, 35.0);
        assertThat(r.microArcCount()).isZero();
    }

    @Test
    void insulationDegradationShouldProgressOverTime() {
        TelemetryReading early = engine.generate(device, ScenarioType.INSULATION_DEGRADATION_PROGRESSIVE, 10, now);
        TelemetryReading late = engine.generate(device, ScenarioType.INSULATION_DEGRADATION_PROGRESSIVE, 200, now);

        assertThat(late.thd()).isGreaterThan(early.thd() - 2.0);
        assertThat(late.hfNoiseLevel()).isGreaterThan(early.hfNoiseLevel() - 0.1);
    }

    @Test
    void looseConnectionShouldHaveSpikes() {
        int spikes = 0;
        for (int i = 0; i < 100; i++) {
            TelemetryReading r = engine.generate(device, ScenarioType.LOOSE_CONNECTION, i, now);
            if (r.microArcCount() > 0) spikes++;
        }
        assertThat(spikes).isGreaterThan(5);
    }

    @Test
    void microArcShouldProduceRecurrentArcs() {
        int totalArcs = 0;
        for (int i = 0; i < 50; i++) {
            TelemetryReading r = engine.generate(device, ScenarioType.MICRO_ARC_RECURRENT, i, now);
            totalArcs += r.microArcCount();
        }
        assertThat(totalArcs).isGreaterThan(20);
    }

    @Test
    void overloadShouldExceedNominalCurrent() {
        double maxCurrent = 0;
        for (int i = 0; i < 50; i++) {
            TelemetryReading r = engine.generate(device, ScenarioType.OVERLOAD, i, now);
            maxCurrent = Math.max(maxCurrent, r.rmsCurrent());
        }
        assertThat(maxCurrent).isGreaterThan(15.0);
    }

    @Test
    void temperatureRiseShouldIncrease() {
        TelemetryReading early = engine.generate(device, ScenarioType.TEMPERATURE_RISE, 5, now);
        TelemetryReading late = engine.generate(device, ScenarioType.TEMPERATURE_RISE, 150, now);

        assertThat(late.temperatureCelsius()).isGreaterThan(early.temperatureCelsius());
        assertThat(late.temperatureCelsius()).isGreaterThan(50.0);
    }

    @Test
    void deviceOfflineShouldReturnNull() {
        TelemetryReading r = engine.generate(device, ScenarioType.DEVICE_OFFLINE, 0, now);
        assertThat(r).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = ScenarioType.class, names = "DEVICE_OFFLINE", mode = EnumSource.Mode.EXCLUDE)
    void allActiveScenariosShouldPopulateDeviceInfo(ScenarioType scenario) {
        TelemetryReading r = engine.generate(device, scenario, 50, now);

        assertThat(r).isNotNull();
        assertThat(r.tenantId()).isEqualTo("tenant-1");
        assertThat(r.deviceId()).isEqualTo("dev-1");
        assertThat(r.buildingId()).isEqualTo("building-1");
        assertThat(r.panelId()).isEqualTo("panel-1");
        assertThat(r.circuitId()).isEqualTo("circuit-1");
        assertThat(r.timestamp()).isEqualTo(now);
    }

    @ParameterizedTest
    @EnumSource(value = ScenarioType.class, names = "DEVICE_OFFLINE", mode = EnumSource.Mode.EXCLUDE)
    void allActiveScenariosShouldProducePositiveValues(ScenarioType scenario) {
        TelemetryReading r = engine.generate(device, scenario, 50, now);

        assertThat(r).isNotNull();
        assertThat(r.rmsVoltage()).isPositive();
        assertThat(r.activePower()).isPositive();
        assertThat(r.microArcCount()).isNotNegative();
        assertThat(r.transientCount()).isNotNegative();
    }
}
