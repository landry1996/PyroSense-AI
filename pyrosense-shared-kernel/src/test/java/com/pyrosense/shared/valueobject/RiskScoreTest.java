package com.pyrosense.shared.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RiskScoreTest {

    @Test
    void shouldCreateValidRiskScore() {
        var score = RiskScore.of(75);
        assertThat(score.value()).isEqualTo(75);
    }

    @Test
    void shouldAllowBoundaryValues() {
        assertThat(RiskScore.of(0).value()).isZero();
        assertThat(RiskScore.of(100).value()).isEqualTo(100);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -100, 101, 200, Integer.MAX_VALUE, Integer.MIN_VALUE})
    void shouldRejectInvalidValues(int invalidValue) {
        assertThatThrownBy(() -> RiskScore.of(invalidValue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RiskScore must be between");
    }

    @Test
    void shouldMapToInfoSeverityBelow50() {
        assertThat(RiskScore.of(0).toSeverity()).isEqualTo(AlertSeverity.INFO);
        assertThat(RiskScore.of(49).toSeverity()).isEqualTo(AlertSeverity.INFO);
    }

    @Test
    void shouldMapToWarningSeverityBetween50And79() {
        assertThat(RiskScore.of(50).toSeverity()).isEqualTo(AlertSeverity.WARNING);
        assertThat(RiskScore.of(79).toSeverity()).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    void shouldMapToCriticalSeverityAbove80() {
        assertThat(RiskScore.of(80).toSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(RiskScore.of(100).toSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void shouldDetectCritical() {
        assertThat(RiskScore.of(80).isCritical()).isTrue();
        assertThat(RiskScore.of(79).isCritical()).isFalse();
    }

    @Test
    void shouldDetectWarning() {
        assertThat(RiskScore.of(50).isWarning()).isTrue();
        assertThat(RiskScore.of(79).isWarning()).isTrue();
        assertThat(RiskScore.of(80).isWarning()).isFalse();
        assertThat(RiskScore.of(49).isWarning()).isFalse();
    }

    @Test
    void shouldCompare() {
        assertThat(RiskScore.of(80)).isGreaterThan(RiskScore.of(50));
        assertThat(RiskScore.of(30)).isLessThan(RiskScore.of(60));
        assertThat(RiskScore.of(50)).isEqualByComparingTo(RiskScore.of(50));
    }

    @Test
    void shouldCheckThreshold() {
        assertThat(RiskScore.of(75).isAboveThreshold(75)).isTrue();
        assertThat(RiskScore.of(74).isAboveThreshold(75)).isFalse();
    }

    @Test
    void factoryMethodsShouldWork() {
        assertThat(RiskScore.zero().value()).isZero();
        assertThat(RiskScore.maximum().value()).isEqualTo(100);
    }
}
