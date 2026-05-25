package com.pyrosense.simulator.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SimulatedTenant(
        String tenantId,
        String name,
        List<SimulatedBuilding> buildings
) {
    public static SimulatedTenant create(String name, int buildingCount, int devicesPerBuilding) {
        String tenantId = UUID.randomUUID().toString();
        List<SimulatedBuilding> buildings = new ArrayList<>();
        for (int i = 0; i < buildingCount; i++) {
            buildings.add(SimulatedBuilding.create(tenantId, "building-" + (i + 1), devicesPerBuilding));
        }
        return new SimulatedTenant(tenantId, name, buildings);
    }

    public List<SimulatedDevice> allDevices() {
        return buildings.stream()
                .flatMap(b -> b.devices().stream())
                .toList();
    }
}
