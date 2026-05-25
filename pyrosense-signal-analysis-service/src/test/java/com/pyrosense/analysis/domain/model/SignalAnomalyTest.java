package com.pyrosense.analysis.domain.model;

import com.pyrosense.shared.id.DeviceId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class SignalAnomalyTest {

    @Test
    void shouldCalculateWeightedScore() {
        var anomaly = new SignalAnomaly(
                UUID.randomUUID(), DeviceId.generate(), SignalFeature.MICRO_ARC_COUNT,
                AnomalyType.MICRO_ARC_RECURRENT, 5.0, 0.0, 4.5, 0.9, Instant.now());

        double score = anomaly.weightedScore();
        assertThat(score).isGreaterThan(0);
        assertThat(score).isLessThanOrEqualTo(1.0);
    }

    @Test
    void shouldRejectInvalidConfidence() {
        assertThatThrownBy(() -> new SignalAnomaly(
                UUID.randomUUID(), DeviceId.generate(), SignalFeature.THD,
                AnomalyType.THD_ABNORMAL, 10.0, 5.0, 3.0, 1.5, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNegativeConfidence() {
        assertThatThrownBy(() -> new SignalAnomaly(
                UUID.randomUUID(), DeviceId.generate(), SignalFeature.THD,
                AnomalyType.THD_ABNORMAL, 10.0, 5.0, 3.0, -0.1, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCapWeightedScoreAtOne() {
        var anomaly = new SignalAnomaly(
                UUID.randomUUID(), DeviceId.generate(), SignalFeature.MICRO_ARC_COUNT,
                AnomalyType.MICRO_ARC_RECURRENT, 100.0, 0.0, 15.0, 1.0, Instant.now());

        assertThat(anomaly.weightedScore()).isLessThanOrEqualTo(1.0);
    }
}
