package com.pyrosense.simulator.domain;

import com.pyrosense.simulator.config.SimulatorConfig;
import com.pyrosense.simulator.publisher.TelemetryPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationEngineTest {

    private SimulationEngine engine;

    @AfterEach
    void tearDown() {
        if (engine != null && engine.isRunning()) {
            engine.stop();
        }
    }

    @Test
    void shouldCreateCorrectNumberOfDevices() {
        SimulatorConfig config = SimulatorConfig.builder()
                .tenantCount(2)
                .buildingsPerTenant(3)
                .devicesPerBuilding(4)
                .useMqtt(false)
                .build();
        engine = new SimulationEngine(config, List.of(new RecordingPublisher()));

        assertThat(engine.getTotalDeviceCount()).isEqualTo(24);
    }

    @Test
    void shouldStartAndPublish() throws InterruptedException {
        RecordingPublisher publisher = new RecordingPublisher();
        SimulatorConfig config = SimulatorConfig.builder()
                .tenantCount(1)
                .buildingsPerTenant(1)
                .devicesPerBuilding(2)
                .publishInterval(Duration.ofMillis(50))
                .simulationDurationDays(1)
                .useMqtt(false)
                .build();
        engine = new SimulationEngine(config, List.of(publisher));

        engine.start();
        Thread.sleep(200);
        engine.stop();

        assertThat(publisher.readings).isNotEmpty();
        assertThat(engine.getTotalPublished()).isGreaterThan(0);
    }

    @Test
    void shouldNotPublishWhenDeviceOffline() throws InterruptedException {
        RecordingPublisher publisher = new RecordingPublisher();
        SimulatorConfig config = SimulatorConfig.builder()
                .tenantCount(1)
                .buildingsPerTenant(1)
                .devicesPerBuilding(1)
                .publishInterval(Duration.ofMillis(50))
                .useMqtt(false)
                .build();
        engine = new SimulationEngine(config, List.of(publisher));
        engine.setScenario(ScenarioType.DEVICE_OFFLINE);

        engine.start();
        Thread.sleep(200);
        engine.stop();

        assertThat(publisher.readings).isEmpty();
    }

    @Test
    void shouldChangeScenario() {
        SimulatorConfig config = SimulatorConfig.builder().useMqtt(false).build();
        engine = new SimulationEngine(config, List.of(new RecordingPublisher()));

        engine.setScenario(ScenarioType.OVERLOAD);
        assertThat(engine.getActiveScenario()).isEqualTo(ScenarioType.OVERLOAD);
    }

    @Test
    void shouldNotStartTwice() {
        RecordingPublisher publisher = new RecordingPublisher();
        SimulatorConfig config = SimulatorConfig.builder()
                .tenantCount(1)
                .buildingsPerTenant(1)
                .devicesPerBuilding(1)
                .publishInterval(Duration.ofMillis(100))
                .useMqtt(false)
                .build();
        engine = new SimulationEngine(config, List.of(publisher));

        engine.start();
        engine.start();
        assertThat(engine.isRunning()).isTrue();
        engine.stop();
    }

    @Test
    void shouldReportNotRunningInitially() {
        SimulatorConfig config = SimulatorConfig.builder().useMqtt(false).build();
        engine = new SimulationEngine(config, List.of(new RecordingPublisher()));

        assertThat(engine.isRunning()).isFalse();
    }

    static class RecordingPublisher implements TelemetryPublisher {
        final List<TelemetryReading> readings = Collections.synchronizedList(new ArrayList<>());
        @Override public void publish(TelemetryReading reading) { readings.add(reading); }
        @Override public void connect() {}
        @Override public void disconnect() {}
        @Override public String name() { return "Recording"; }
    }
}
