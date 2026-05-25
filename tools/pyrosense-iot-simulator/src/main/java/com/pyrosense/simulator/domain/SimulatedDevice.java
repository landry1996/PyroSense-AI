package com.pyrosense.simulator.domain;

import java.util.UUID;

public record SimulatedDevice(
        String deviceId,
        String tenantId,
        String buildingId,
        String panelId,
        String circuitId,
        String name
) {
    public static SimulatedDevice create(String tenantId, String buildingId, String panelId, int index) {
        String deviceId = UUID.randomUUID().toString();
        String circuitId = UUID.randomUUID().toString();
        String name = "device-" + (index + 1);
        return new SimulatedDevice(deviceId, tenantId, buildingId, panelId, circuitId, name);
    }

    public String mqttTopic() {
        return "pyrosense/" + tenantId + "/" + deviceId + "/telemetry";
    }
}
