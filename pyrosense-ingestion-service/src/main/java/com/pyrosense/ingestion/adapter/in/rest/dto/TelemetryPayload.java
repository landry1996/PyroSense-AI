package com.pyrosense.ingestion.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record TelemetryPayload(
        @NotBlank String deviceId,
        @NotNull Instant timestamp,
        @Positive int samplingWindowMs,
        double rmsCurrent,
        double rmsVoltage,
        double activePower,
        double reactivePower,
        double powerFactor,
        double thd,
        double temperatureCelsius,
        double hfNoiseLevel,
        int microArcCount,
        int transientCount,
        String firmwareVersion
) {}
