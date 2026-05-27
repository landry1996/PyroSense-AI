package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import java.time.Instant;

public record HeartbeatPayloadV1(
        String schemaVersion,
        String deviceId,
        String tenantId,
        Instant timestamp,
        long sequenceNumber,
        String firmwareVersion,
        String state,
        long uptimeSeconds,
        long freeHeapBytes,
        int wifiRssiDbm,
        int bufferUsagePercent,
        double signalQuality,
        Integer cpuPercent,
        Integer internalTempCelsius
) {}
