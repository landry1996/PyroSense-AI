package com.pyrosense.maintenance.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RiskImpactTest {

    @Test
    void shouldCalculateRiskReduction() {
        RiskImpact impact = new RiskImpact(80, 25, 60);
        assertThat(impact.riskReduction()).isEqualTo(55);
    }

    @Test
    void shouldDetectImprovement() {
        assertThat(new RiskImpact(80, 25, null).hasImprovement()).isTrue();
        assertThat(new RiskImpact(30, 30, null).hasImprovement()).isFalse();
        assertThat(new RiskImpact(20, 40, null).hasImprovement()).isFalse();
    }

    @Test
    void shouldClampReductionToZero() {
        RiskImpact impact = new RiskImpact(20, 50, null);
        assertThat(impact.riskReduction()).isEqualTo(0);
    }

    @Test
    void shouldRejectInvalidScores() {
        assertThatThrownBy(() -> new RiskImpact(-1, 50, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RiskImpact(50, 101, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
