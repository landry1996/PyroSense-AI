package com.pyrosense.device.domain.event;

import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record DeviceProvisionedEvent(
        UUID eventId,
        Instant occurredAt,
        DeviceId deviceId,
        TenantId tenantId,
        BuildingId buildingId,
        ElectricalPanelId panelId
) implements DomainEvent {

    @Override
    public String eventType() {
        return "device.provisioned";
    }
}
