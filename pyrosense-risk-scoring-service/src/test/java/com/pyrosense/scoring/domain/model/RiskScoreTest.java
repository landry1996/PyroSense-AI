package com.pyrosense.scoring.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class RiskScoreTest {

    @Test
    void shouldCreateValidScore() {
        var score = new RiskScore(50);
        assertThat(score.value()).isEqualTo(50);
    }

    @Test
    void shouldRejectNegativeScore() {
        assertThatThrownBy(() -> new RiskScore(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectScoreAbove100() {
        assertThatThrownBy(() -> new RiskScore(101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAcceptBoundaryValues() {
        assertThatNoException().isThrownBy(() -> new RiskScore(0));
        assertThatNoException().isThrownBy(() -> new RiskScore(100));
    }

    @Test
    void shouldClampWithOf() {
        assertThat(RiskScore.of(-10).value()).isEqualTo(0);
        assertThat(RiskScore.of(150).value()).isEqualTo(100);
        assertThat(RiskScore.of(55.7).value()).isEqualTo(56);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10, 29})
    void shouldMapToLowLevel(int score) {
        assertThat(new RiskScore(score).level()).isEqualTo(RiskLevel.LOW);
    }

    @ParameterizedTest
    @ValueSource(ints = {30, 45, 59})
    void shouldMapToModerateLevel(int score) {
        assertThat(new RiskScore(score).level()).isEqualTo(RiskLevel.MODERATE);
    }

    @ParameterizedTest
    @ValueSource(ints = {60, 70, 79})
    void shouldMapToHighLevel(int score) {
        assertThat(new RiskScore(score).level()).isEqualTo(RiskLevel.HIGH);
    }

    @ParameterizedTest
    @ValueSource(ints = {80, 90, 100})
    void shouldMapToCriticalLevel(int score) {
        assertThat(new RiskScore(score).level()).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void shouldBeComparable() {
        var low = new RiskScore(20);
        var high = new RiskScore(80);
        assertThat(low).isLessThan(high);
    }

    @Test
    void shouldDetectCritical() {
        assertThat(new RiskScore(80).isCritical()).isTrue();
        assertThat(new RiskScore(79).isCritical()).isFalse();
    }
}
