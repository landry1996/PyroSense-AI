package com.pyrosense.device.adapter.in.rest.dto;

import com.pyrosense.device.domain.model.ConnectivityType;
import com.pyrosense.device.domain.model.DeviceStatus;

import java.time.Instant;
import java.util.UUID;

public record DeviceResponse(
        UUID id,
        String serialNumber,
        UUID tenantId,
        UUID buildingId,
        UUID panelId,
        String firmwareVersion,
        String hardwareRevision,
        ConnectivityType connectivityType,
        DeviceStatus status,
        Instant lastSeenAt,
        Instant installationDate,
        Instant createdAt,
        String createdBy
) {}
