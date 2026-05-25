package com.pyrosense.device.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProvisionDeviceRequest(
        @NotNull UUID tenantId,
        @NotNull UUID buildingId,
        @NotNull UUID panelId
) {}
