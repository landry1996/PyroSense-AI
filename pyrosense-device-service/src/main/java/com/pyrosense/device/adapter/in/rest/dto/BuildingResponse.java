package com.pyrosense.device.adapter.in.rest.dto;

import java.time.Instant;

public record BuildingResponse(
        String id,
        String name,
        String address,
        int totalDevices,
        int activeDevices,
        double riskScore,
        String status,
        Instant lastAlertAt
) {}
