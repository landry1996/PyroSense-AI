package com.pyrosense.simulator.domain;

import com.pyrosense.simulator.config.SimulatorConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatorConfigTest {

    @Test
    void defaultsShouldHaveReasonableValues() {
        SimulatorConfig config = SimulatorConfig.defaults();

        assertThat(config.mqttHost()).isEqualTo("localhost");
        assertThat(config.mqttPort()).isEqualTo(1883);
        assertThat(config.restBaseUrl()).isEqualTo("http://localhost:8083");
        assertThat(config.tenantCount()).isEqualTo(2);
        assertThat(config.buildingsPerTenant()).isEqualTo(2);
        assertThat(config.devicesPerBuilding()).isEqualTo(3);
        assertThat(config.publishInterval()).isEqualTo(Duration.ofSeconds(5));
        assertThat(config.timeAccelerationFactor()).isEqualTo(1);
        assertThat(config.simulationDurationDays()).isEqualTo(1);
        assertThat(config.useMqtt()).isTrue();
        assertThat(config.useRest()).isFalse();
    }

    @Test
    void builderShouldOverrideDefaults() {
        SimulatorConfig config = SimulatorConfig.builder()
                .tenantCount(5)
                .devicesPerBuilding(10)
                .timeAccelerationFactor(60)
                .build();

        assertThat(config.tenantCount()).isEqualTo(5);
        assertThat(config.devicesPerBuilding()).isEqualTo(10);
        assertThat(config.timeAccelerationFactor()).isEqualTo(60);
    }

    @Test
    void builderShouldSetAllFields() {
        SimulatorConfig config = SimulatorConfig.builder()
                .mqttHost("broker.example.com")
                .mqttPort(8883)
                .restBaseUrl("http://api.example.com")
                .tenantCount(3)
                .buildingsPerTenant(5)
                .devicesPerBuilding(8)
                .publishInterval(Duration.ofSeconds(10))
                .timeAccelerationFactor(100)
                .simulationDurationDays(7)
                .useMqtt(false)
                .useRest(true)
                .build();

        assertThat(config.mqttHost()).isEqualTo("broker.example.com");
        assertThat(config.mqttPort()).isEqualTo(8883);
        assertThat(config.restBaseUrl()).isEqualTo("http://api.example.com");
        assertThat(config.tenantCount()).isEqualTo(3);
        assertThat(config.buildingsPerTenant()).isEqualTo(5);
        assertThat(config.devicesPerBuilding()).isEqualTo(8);
        assertThat(config.publishInterval()).isEqualTo(Duration.ofSeconds(10));
        assertThat(config.timeAccelerationFactor()).isEqualTo(100);
        assertThat(config.simulationDurationDays()).isEqualTo(7);
        assertThat(config.useMqtt()).isFalse();
        assertThat(config.useRest()).isTrue();
    }
}
