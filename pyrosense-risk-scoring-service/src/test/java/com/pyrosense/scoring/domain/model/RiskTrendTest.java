package com.pyrosense.scoring.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class RiskTrendTest {

    @Test
    void shouldReturnStableForSingleScore() {
        assertThat(RiskTrend.compute(List.of(50))).isEqualTo(RiskTrend.STABLE);
    }

    @Test
    void shouldReturnStableForNullList() {
        assertThat(RiskTrend.compute(null)).isEqualTo(RiskTrend.STABLE);
    }

    @Test
    void shouldDetectDegrading() {
        assertThat(RiskTrend.compute(List.of(40, 50))).isEqualTo(RiskTrend.DEGRADING);
    }

    @Test
    void shouldDetectImproving() {
        assertThat(RiskTrend.compute(List.of(60, 50))).isEqualTo(RiskTrend.IMPROVING);
    }

    @Test
    void shouldDetectStableWhenSmallChange() {
        assertThat(RiskTrend.compute(List.of(50, 53))).isEqualTo(RiskTrend.STABLE);
    }

    @Test
    void shouldDetectCriticalWithConsecutiveLargeIncrease() {
        assertThat(RiskTrend.compute(List.of(30, 45, 60))).isEqualTo(RiskTrend.CRITICAL);
    }

    @Test
    void shouldNotBeCriticalWithSingleLargeIncrease() {
        assertThat(RiskTrend.compute(List.of(40, 42, 60))).isEqualTo(RiskTrend.DEGRADING);
    }
}
