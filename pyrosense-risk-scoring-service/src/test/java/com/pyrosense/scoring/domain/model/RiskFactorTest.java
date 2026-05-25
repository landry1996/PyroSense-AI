package com.pyrosense.scoring.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RiskFactorTest {

    @Test
    void shouldComputeContribution() {
        var factor = RiskFactor.of("test", "description", 0.3, 0.8);
        assertThat(factor.contribution()).isCloseTo(24.0, within(0.01));
    }

    @Test
    void shouldRejectInvalidWeight() {
        assertThatThrownBy(() -> RiskFactor.of("test", "desc", 1.5, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectInvalidValue() {
        assertThatThrownBy(() -> RiskFactor.of("test", "desc", 0.5, 1.5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAcceptZeroValues() {
        var factor = RiskFactor.of("test", "desc", 0.0, 0.0);
        assertThat(factor.contribution()).isEqualTo(0.0);
    }

    @Test
    void shouldAcceptMaxValues() {
        var factor = RiskFactor.of("test", "desc", 1.0, 1.0);
        assertThat(factor.contribution()).isEqualTo(100.0);
    }
}
