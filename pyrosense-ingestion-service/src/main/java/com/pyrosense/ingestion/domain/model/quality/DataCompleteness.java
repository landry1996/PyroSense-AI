package com.pyrosense.ingestion.domain.model.quality;

import java.time.Duration;

public record DataCompleteness(
        Duration assessmentWindow,
        int expectedHeartbeats,
        int receivedHeartbeats,
        int expectedTelemetry,
        int receivedTelemetry,
        int sequenceGaps,
        int duplicateSequences
) {
    public double heartbeatCompleteness() {
        return expectedHeartbeats > 0 ? (double) receivedHeartbeats / expectedHeartbeats : 0.0;
    }

    public double telemetryCompleteness() {
        return expectedTelemetry > 0 ? (double) receivedTelemetry / expectedTelemetry : 0.0;
    }

    public double overallCompleteness() {
        double hb = heartbeatCompleteness();
        double tm = telemetryCompleteness();
        return (hb * 0.3 + tm * 0.7);
    }

    public boolean hasSequenceIssues() {
        return sequenceGaps > 0 || duplicateSequences > 0;
    }

    public boolean isAcceptable() {
        return overallCompleteness() >= 0.95 && !hasSequenceIssues();
    }
}
