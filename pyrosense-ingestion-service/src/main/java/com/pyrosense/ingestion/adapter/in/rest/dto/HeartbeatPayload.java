package com.pyrosense.ingestion.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record HeartbeatPayload(
        @NotBlank String deviceId,
        @NotNull Instant timestamp,
        String firmwareVersion,
        double uptimeHours,
        double memoryUsagePercent,
        double cpuTemperatureCelsius
) {}
