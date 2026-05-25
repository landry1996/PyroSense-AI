package com.pyrosense.simulator.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedDeviceTest {

    @Test
    void shouldCreateWithCorrectAttributes() {
        SimulatedDevice device = SimulatedDevice.create("tenant-1", "building-1", "panel-1", 0);

        assertThat(device.tenantId()).isEqualTo("tenant-1");
        assertThat(device.buildingId()).isEqualTo("building-1");
        assertThat(device.panelId()).isEqualTo("panel-1");
        assertThat(device.deviceId()).isNotBlank();
        assertThat(device.circuitId()).isNotBlank();
        assertThat(device.name()).isEqualTo("device-1");
    }

    @Test
    void shouldGenerateUniquIds() {
        SimulatedDevice d1 = SimulatedDevice.create("t1", "b1", "p1", 0);
        SimulatedDevice d2 = SimulatedDevice.create("t1", "b1", "p1", 1);

        assertThat(d1.deviceId()).isNotEqualTo(d2.deviceId());
        assertThat(d1.circuitId()).isNotEqualTo(d2.circuitId());
    }

    @Test
    void shouldBuildCorrectMqttTopic() {
        SimulatedDevice device = new SimulatedDevice("dev-123", "tenant-abc", "b1", "p1", "c1", "test");

        assertThat(device.mqttTopic()).isEqualTo("pyrosense/tenant-abc/dev-123/telemetry");
    }

    @Test
    void shouldIncrementDeviceNameByIndex() {
        SimulatedDevice d0 = SimulatedDevice.create("t", "b", "p", 0);
        SimulatedDevice d4 = SimulatedDevice.create("t", "b", "p", 4);

        assertThat(d0.name()).isEqualTo("device-1");
        assertThat(d4.name()).isEqualTo("device-5");
    }
}
