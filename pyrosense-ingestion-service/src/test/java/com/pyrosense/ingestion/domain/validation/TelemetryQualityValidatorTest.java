package com.pyrosense.ingestion.domain.validation;

import com.pyrosense.ingestion.domain.model.SignalQualityScore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TelemetryQualityValidatorTest {

    @Test
    @DisplayName("good quality data passes assessment")
    void goodQuality() {
        var result = TelemetryQualityValidator.assess(0.85, 230.0, 5.0, 0.2, 1000);

        assertThat(result.acceptable()).isTrue();
        assertThat(result.warnings()).isEmpty();
        assertThat(result.signalScore().level()).isEqualTo(SignalQualityScore.QualityLevel.GOOD);
    }

    @Test
    @DisplayName("low signal quality is flagged")
    void lowSignalQuality() {
        var result = TelemetryQualityValidator.assess(0.2, 230.0, 5.0, 0.2, 1000);

        assertThat(result.acceptable()).isFalse();
        assertThat(result.warnings()).anyMatch(w -> w.contains("Signal quality below minimum"));
        assertThat(result.signalScore().requiresAlert()).isTrue();
    }

    @Test
    @DisplayName("high HF noise is warned")
    void highNoiseLevel() {
        var result = TelemetryQualityValidator.assess(0.8, 230.0, 5.0, 0.85, 1000);

        assertThat(result.acceptable()).isTrue();
        assertThat(result.warnings()).anyMatch(w -> w.contains("High HF noise"));
    }

    @Test
    @DisplayName("voltage near noise floor is warned")
    void voltageNearNoiseFloor() {
        var result = TelemetryQualityValidator.assess(0.8, 0.01, 5.0, 0.2, 1000);

        assertThat(result.acceptable()).isTrue();
        assertThat(result.warnings()).anyMatch(w -> w.contains("Voltage reading near noise floor"));
    }

    @Test
    @DisplayName("current near noise floor is warned")
    void currentNearNoiseFloor() {
        var result = TelemetryQualityValidator.assess(0.8, 230.0, 0.01, 0.2, 1000);

        assertThat(result.acceptable()).isTrue();
        assertThat(result.warnings()).anyMatch(w -> w.contains("Current reading near noise floor"));
    }

    @Test
    @DisplayName("short sampling window is warned")
    void shortSamplingWindow() {
        var result = TelemetryQualityValidator.assess(0.8, 230.0, 5.0, 0.2, 50);

        assertThat(result.acceptable()).isTrue();
        assertThat(result.warnings()).anyMatch(w -> w.contains("Sampling window unusually short"));
    }

    @Test
    @DisplayName("multiple quality issues accumulate warnings")
    void multipleIssues() {
        var result = TelemetryQualityValidator.assess(0.2, 0.01, 0.01, 0.9, 50);

        assertThat(result.acceptable()).isFalse();
        assertThat(result.warnings()).hasSizeGreaterThanOrEqualTo(4);
    }
}
