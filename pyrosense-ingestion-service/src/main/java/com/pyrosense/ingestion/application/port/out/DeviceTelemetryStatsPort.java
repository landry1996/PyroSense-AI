package com.pyrosense.ingestion.application.port.out;

import java.time.Instant;

public interface DeviceTelemetryStatsPort {

    record TelemetryStats(
            int totalReadings,
            int outOfRangeReadings,
            java.util.List<String> outOfRangeFields,
            double averageSignalQuality,
            double maxClockDriftSeconds,
            double hfNoiseAverage,
            double hfNoiseMax,
            double currentStdDev,
            double voltageStdDev,
            int sequenceGaps,
            int duplicateSequences,
            int offlineTransitions,
            String latestFirmwareVersion
    ) {}

    TelemetryStats computeStats(String deviceId, Instant from, Instant to);

    int countExpectedMessages(String deviceId, Instant from, Instant to);

    int countReceivedMessages(String deviceId, Instant from, Instant to);

    int countExpectedHeartbeats(String deviceId, Instant from, Instant to);

    int countReceivedHeartbeats(String deviceId, Instant from, Instant to);
}
