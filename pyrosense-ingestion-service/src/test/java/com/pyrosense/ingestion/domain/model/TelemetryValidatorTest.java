package com.pyrosense.ingestion.domain.model;

import com.pyrosense.ingestion.domain.validation.TelemetryValidator;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class TelemetryValidatorTest {

    @Test
    void shouldPassValidTelemetry() {
        var violations = TelemetryValidator.validate(
                230.0, 12.0, 2800.0, 300.0, 0.94, 4.5, 39.5, 0.18, 2, 1, 1000, Instant.now());
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectNegativeVoltage() {
        var violations = TelemetryValidator.validate(
                -1.0, 12.0, 2800.0, 300.0, 0.94, 4.5, 39.5, 0.18, 0, 0, 1000, Instant.now());
        assertThat(violations).anyMatch(v -> v.contains("rmsVoltage"));
    }

    @Test
    void shouldRejectVoltageAboveMax() {
        var violations = TelemetryValidator.validate(
                1001.0, 12.0, 2800.0, 300.0, 0.94, 4.5, 39.5, 0.18, 0, 0, 1000, Instant.now());
        assertThat(violations).anyMatch(v -> v.contains("rmsVoltage"));
    }

    @Test
    void shouldRejectNegativeCurrent() {
        var violations = TelemetryValidator.validate(
                230.0, -5.0, 2800.0, 300.0, 0.94, 4.5, 39.5, 0.18, 0, 0, 1000, Instant.now());
        assertThat(violations).anyMatch(v -> v.contains("rmsCurrent"));
    }

    @Test
    void shouldRejectPowerFactorAboveOne() {
        var violations = TelemetryValidator.validate(
                230.0, 12.0, 2800.0, 300.0, 1.5, 4.5, 39.5, 0.18, 0, 0, 1000, Instant.now());
        assertThat(violations).anyMatch(v -> v.contains("powerFactor"));
    }

    @Test
    void shouldRejectThdAbove100() {
        var violations = TelemetryValidator.validate(
                230.0, 12.0, 2800.0, 300.0, 0.94, 101.0, 39.5, 0.18, 0, 0, 1000, Instant.now());
        assertThat(violations).anyMatch(v -> v.contains("thd"));
    }

    @Test
    void shouldRejectTemperatureBelowMinus40() {
        var violations = TelemetryValidator.validate(
                230.0, 12.0, 2800.0, 300.0, 0.94, 4.5, -50.0, 0.18, 0, 0, 1000, Instant.now());
        assertThat(violations).anyMatch(v -> v.contains("temperatureCelsius"));
    }

    @Test
    void shouldRejectNegativeMicroArcCount() {
        var violations = TelemetryValidator.validate(
                230.0, 12.0, 2800.0, 300.0, 0.94, 4.5, 39.5, 0.18, -1, 0, 1000, Instant.now());
        assertThat(violations).anyMatch(v -> v.contains("microArcCount"));
    }

    @Test
    void shouldRejectInvalidSamplingWindow() {
        var violations = TelemetryValidator.validate(
                230.0, 12.0, 2800.0, 300.0, 0.94, 4.5, 39.5, 0.18, 0, 0, 0, Instant.now());
        assertThat(violations).anyMatch(v -> v.contains("samplingWindowMs"));
    }

    @Test
    void shouldRejectNullTimestamp() {
        var violations = TelemetryValidator.validate(
                230.0, 12.0, 2800.0, 300.0, 0.94, 4.5, 39.5, 0.18, 0, 0, 1000, null);
        assertThat(violations).anyMatch(v -> v.contains("timestamp"));
    }

    @Test
    void shouldRejectTimestampTooFarInFuture() {
        var violations = TelemetryValidator.validate(
                230.0, 12.0, 2800.0, 300.0, 0.94, 4.5, 39.5, 0.18, 0, 0, 1000,
                Instant.now().plusSeconds(600));
        assertThat(violations).anyMatch(v -> v.contains("future"));
    }

    @Test
    void shouldCollectMultipleViolations() {
        var violations = TelemetryValidator.validate(
                -1.0, -1.0, -1.0, -1.0, 2.0, 200.0, 300.0, 20.0, -1, -1, 0, null);
        assertThat(violations.size()).isGreaterThan(5);
    }
}
