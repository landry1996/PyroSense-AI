package com.pyrosense.simulator.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SimulatedBuilding(
        String buildingId,
        String tenantId,
        String name,
        String panelId,
        List<SimulatedDevice> devices
) {
    public static SimulatedBuilding create(String tenantId, String name, int deviceCount) {
        String buildingId = UUID.randomUUID().toString();
        String panelId = UUID.randomUUID().toString();
        List<SimulatedDevice> devices = new ArrayList<>();
        for (int i = 0; i < deviceCount; i++) {
            devices.add(SimulatedDevice.create(tenantId, buildingId, panelId, i));
        }
        return new SimulatedBuilding(buildingId, tenantId, name, panelId, devices);
    }
}
