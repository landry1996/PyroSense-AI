package com.pyrosense.scoring.domain.scoring;

import com.pyrosense.scoring.domain.model.*;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class RiskScoringEngineTest {

    private final RiskScoringEngine engine = new RiskScoringEngine(ScoringWeights.defaults());
    private final DeviceId deviceId = DeviceId.generate();
    private final ElectricalPanelId panelId = new ElectricalPanelId(UUID.randomUUID());
    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");

    @Test
    void shouldReturnZeroScoreWithNoAnomalies() {
        RiskAssessment result = engine.computeScore(
                deviceId, panelId, null, List.of(), List.of(), true, true, now);

        assertThat(result.score().value()).isEqualTo(0);
        assertThat(result.level()).isEqualTo(RiskLevel.LOW);
        assertThat(result.factors()).hasSize(6);
    }

    @Test
    void shouldScoreHighWithMicroArcs() {
        List<AnomalyInput> anomalies = List.of(
                new AnomalyInput(deviceId, "MICRO_ARC_RECURRENT", 0.9, 5.0, now),
                new AnomalyInput(deviceId, "MICRO_ARC_RECURRENT", 0.85, 4.0, now.minusSeconds(3600))
        );

        RiskAssessment result = engine.computeScore(
                deviceId, panelId, null, anomalies, List.of(), true, true, now);

        assertThat(result.score().value()).isGreaterThanOrEqualTo(20);
        assertThat(result.factors().stream()
                .filter(f -> "micro_arc".equals(f.name()))
                .findFirst().orElseThrow()
                .contribution()).isGreaterThan(0);
    }

    @Test
    void shouldScoreCriticalWithMultipleAnomalyTypes() {
        List<AnomalyInput> anomalies = List.of(
                new AnomalyInput(deviceId, "MICRO_ARC_RECURRENT", 0.95, 6.0, now),
                new AnomalyInput(deviceId, "THD_ABNORMAL", 0.9, 4.5, now),
                new AnomalyInput(deviceId, "TEMPERATURE_RISING", 0.85, 3.5, now),
                new AnomalyInput(deviceId, "TRANSIENT_ABNORMAL", 0.8, 3.0, now),
                new AnomalyInput(deviceId, "HF_NOISE_ELEVATED", 0.8, 4.0, now)
        );

        RiskAssessment result = engine.computeScore(
                deviceId, panelId, null, anomalies, List.of(), true, true, now);

        assertThat(result.score().value()).isGreaterThanOrEqualTo(60);
    }

    @Test
    void shouldNeverExceed100() {
        List<AnomalyInput> anomalies = List.of(
                new AnomalyInput(deviceId, "MICRO_ARC_RECURRENT", 1.0, 10.0, now),
                new AnomalyInput(deviceId, "MICRO_ARC_RECURRENT", 1.0, 10.0, now),
                new AnomalyInput(deviceId, "MICRO_ARC_RECURRENT", 1.0, 10.0, now),
                new AnomalyInput(deviceId, "THD_ABNORMAL", 1.0, 10.0, now),
                new AnomalyInput(deviceId, "TEMPERATURE_RISING", 1.0, 10.0, now),
                new AnomalyInput(deviceId, "TRANSIENT_ABNORMAL", 1.0, 10.0, now),
                new AnomalyInput(deviceId, "HF_NOISE_ELEVATED", 1.0, 10.0, now)
        );

        RiskAssessment result = engine.computeScore(
                deviceId, panelId, null, anomalies, List.of(), true, true, now);

        assertThat(result.score().value()).isLessThanOrEqualTo(100);
    }

    @Test
    void shouldNeverBeNegative() {
        RiskAssessment result = engine.computeScore(
                deviceId, panelId, null, List.of(), List.of(), true, true, now);

        assertThat(result.score().value()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldPenalizeOfflineDevice() {
        RiskAssessment online = engine.computeScore(
                deviceId, panelId, null, List.of(), List.of(), true, true, now);
        RiskAssessment offline = engine.computeScore(
                deviceId, panelId, null, List.of(), List.of(), false, true, now);

        assertThat(offline.score().value()).isGreaterThanOrEqualTo(online.score().value());
    }

    @Test
    void shouldPenalizeNoBaseline() {
        RiskAssessment withBaseline = engine.computeScore(
                deviceId, panelId, null, List.of(), List.of(), true, true, now);
        RiskAssessment noBaseline = engine.computeScore(
                deviceId, panelId, null, List.of(), List.of(), true, false, now);

        assertThat(noBaseline.score().value()).isGreaterThanOrEqualTo(withBaseline.score().value());
    }

    @Test
    void shouldDecayOlderAnomalies() {
        AnomalyInput recent = new AnomalyInput(deviceId, "MICRO_ARC_RECURRENT", 0.9, 5.0, now);
        AnomalyInput old = new AnomalyInput(deviceId, "MICRO_ARC_RECURRENT", 0.9, 5.0,
                now.minusSeconds(48 * 3600));

        RiskAssessment recentResult = engine.computeScore(
                deviceId, panelId, null, List.of(recent), List.of(), true, true, now);
        RiskAssessment oldResult = engine.computeScore(
                deviceId, panelId, null, List.of(old), List.of(), true, true, now);

        assertThat(recentResult.score().value()).isGreaterThan(oldResult.score().value());
    }

    @Test
    void shouldIncreaseScoreWithRepetition() {
        AnomalyInput single = new AnomalyInput(deviceId, "THD_ABNORMAL", 0.8, 3.0, now);
        List<AnomalyInput> repeated = List.of(
                new AnomalyInput(deviceId, "THD_ABNORMAL", 0.8, 3.0, now),
                new AnomalyInput(deviceId, "THD_ABNORMAL", 0.8, 3.0, now.minusSeconds(3600)),
                new AnomalyInput(deviceId, "THD_ABNORMAL", 0.8, 3.0, now.minusSeconds(7200))
        );

        RiskAssessment singleResult = engine.computeScore(
                deviceId, panelId, null, List.of(single), List.of(), true, true, now);
        RiskAssessment repeatedResult = engine.computeScore(
                deviceId, panelId, null, repeated, List.of(), true, true, now);

        assertThat(repeatedResult.score().value()).isGreaterThanOrEqualTo(singleResult.score().value());
    }

    @Test
    void shouldProvideExplanation() {
        List<AnomalyInput> anomalies = List.of(
                new AnomalyInput(deviceId, "MICRO_ARC_RECURRENT", 0.9, 5.0, now)
        );

        RiskAssessment result = engine.computeScore(
                deviceId, panelId, null, anomalies, List.of(), true, true, now);

        assertThat(result.recommendation()).isNotBlank();
        assertThat(result.factors()).allSatisfy(f -> {
            assertThat(f.name()).isNotBlank();
            assertThat(f.description()).isNotBlank();
        });
    }

    @Test
    void shouldEstimateShorterIncidentWindowForHighRisk() {
        List<AnomalyInput> highRisk = List.of(
                new AnomalyInput(deviceId, "MICRO_ARC_RECURRENT", 0.95, 6.0, now),
                new AnomalyInput(deviceId, "THD_ABNORMAL", 0.9, 5.0, now),
                new AnomalyInput(deviceId, "TEMPERATURE_RISING", 0.9, 4.0, now)
        );

        RiskAssessment lowResult = engine.computeScore(
                deviceId, panelId, null, List.of(), List.of(), true, true, now);
        RiskAssessment highResult = engine.computeScore(
                deviceId, panelId, null, highRisk, List.of(), true, true, now);

        assertThat(highResult.predictedIncidentWindow().toDays())
                .isLessThan(lowResult.predictedIncidentWindow().toDays());
    }

    @Test
    void shouldSupportCircuitLevelScoring() {
        UUID circuitId = UUID.randomUUID();
        List<AnomalyInput> anomalies = List.of(
                new AnomalyInput(deviceId, "THD_ABNORMAL", 0.7, 3.0, now)
        );

        RiskAssessment result = engine.computeScore(
                deviceId, panelId, circuitId, anomalies, List.of(), true, true, now);

        assertThat(result.circuitId()).isEqualTo(circuitId);
        assertThat(result.panelId()).isEqualTo(panelId);
    }

    @Test
    void shouldHandleInsufficientData() {
        RiskAssessment result = engine.computeScore(
                deviceId, panelId, null, List.of(), List.of(), true, false, now);

        assertThat(result.level()).isIn(RiskLevel.LOW, RiskLevel.MODERATE);
        assertThat(result.recommendation()).isNotBlank();
    }
}
