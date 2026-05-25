package com.pyrosense.analysis.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class StatisticalRangeTest {

    @Test
    void shouldCalculateZScore() {
        var range = new StatisticalRange(100.0, 10.0, 80.0, 120.0, 90.0, 95.0, 100.0, 105.0, 110.0, 1000);
        assertThat(range.zScore(120.0)).isEqualTo(2.0);
        assertThat(range.zScore(80.0)).isEqualTo(-2.0);
        assertThat(range.zScore(100.0)).isEqualTo(0.0);
    }

    @Test
    void shouldReturnZeroWhenStdDevIsZero() {
        var range = new StatisticalRange(50.0, 0.0, 50.0, 50.0, 50.0, 50.0, 50.0, 50.0, 50.0, 100);
        assertThat(range.zScore(60.0)).isEqualTo(0.0);
    }

    @Test
    void shouldDetectOutlier() {
        var range = new StatisticalRange(100.0, 10.0, 80.0, 120.0, 90.0, 95.0, 100.0, 105.0, 110.0, 1000);
        assertThat(range.isOutlier(140.0, 3.0)).isTrue();
        assertThat(range.isOutlier(110.0, 3.0)).isFalse();
    }

    @Test
    void shouldMergeTwoRanges() {
        var r1 = new StatisticalRange(100.0, 10.0, 80.0, 120.0, 85.0, 95.0, 100.0, 105.0, 115.0, 500);
        var r2 = new StatisticalRange(110.0, 12.0, 85.0, 130.0, 90.0, 100.0, 110.0, 115.0, 125.0, 500);

        var merged = r1.merge(r2);
        assertThat(merged.count()).isEqualTo(1000);
        assertThat(merged.mean()).isEqualTo(105.0);
        assertThat(merged.min()).isEqualTo(80.0);
        assertThat(merged.max()).isEqualTo(130.0);
    }

    @Test
    void shouldRejectNegativeStdDev() {
        assertThatThrownBy(() -> new StatisticalRange(0, -1, 0, 0, 0, 0, 0, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNegativeCount() {
        assertThatThrownBy(() -> new StatisticalRange(0, 0, 0, 0, 0, 0, 0, 0, 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
