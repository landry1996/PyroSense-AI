package com.pyrosense.ingestion.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class SignalQualityScoreTest {

    @Test
    @DisplayName("excellent signal quality")
    void excellentQuality() {
        var score = SignalQualityScore.fromRawValue(0.95);
        assertThat(score.level()).isEqualTo(SignalQualityScore.QualityLevel.EXCELLENT);
        assertThat(score.isBelowMinimum()).isFalse();
        assertThat(score.requiresAlert()).isFalse();
    }

    @Test
    @DisplayName("good signal quality")
    void goodQuality() {
        var score = SignalQualityScore.fromRawValue(0.75);
        assertThat(score.level()).isEqualTo(SignalQualityScore.QualityLevel.GOOD);
        assertThat(score.isBelowMinimum()).isFalse();
        assertThat(score.requiresAlert()).isFalse();
    }

    @Test
    @DisplayName("degraded signal quality")
    void degradedQuality() {
        var score = SignalQualityScore.fromRawValue(0.55);
        assertThat(score.level()).isEqualTo(SignalQualityScore.QualityLevel.DEGRADED);
        assertThat(score.isBelowMinimum()).isFalse();
        assertThat(score.requiresAlert()).isFalse();
    }

    @Test
    @DisplayName("poor signal quality triggers alert")
    void poorQuality() {
        var score = SignalQualityScore.fromRawValue(0.35);
        assertThat(score.level()).isEqualTo(SignalQualityScore.QualityLevel.POOR);
        assertThat(score.isBelowMinimum()).isFalse();
        assertThat(score.requiresAlert()).isTrue();
    }

    @Test
    @DisplayName("critical signal quality triggers alert")
    void criticalQuality() {
        var score = SignalQualityScore.fromRawValue(0.1);
        assertThat(score.level()).isEqualTo(SignalQualityScore.QualityLevel.CRITICAL);
        assertThat(score.isBelowMinimum()).isTrue();
        assertThat(score.requiresAlert()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"0.0,CRITICAL", "0.29,CRITICAL", "0.3,POOR", "0.49,POOR", "0.5,DEGRADED", "0.69,DEGRADED", "0.7,GOOD", "0.89,GOOD", "0.9,EXCELLENT", "1.0,EXCELLENT"})
    @DisplayName("boundary values map to correct levels")
    void boundaryValues(double value, String expectedLevel) {
        var score = SignalQualityScore.fromRawValue(value);
        assertThat(score.level()).isEqualTo(SignalQualityScore.QualityLevel.valueOf(expectedLevel));
    }
}
