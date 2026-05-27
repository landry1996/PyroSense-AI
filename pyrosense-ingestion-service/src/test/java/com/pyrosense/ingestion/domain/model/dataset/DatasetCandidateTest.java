package com.pyrosense.ingestion.domain.model.dataset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class DatasetCandidateTest {

    @Test
    @DisplayName("new candidate starts in PENDING_LABEL status")
    void newCandidateIsPending() {
        var candidate = createCandidate(85, "MICRO_ARC");

        assertThat(candidate.getStatus()).isEqualTo(DatasetCandidate.CandidateStatus.PENDING_LABEL);
        assertThat(candidate.getLabels()).isEmpty();
        assertThat(candidate.isExportable()).isFalse();
    }

    @Test
    @DisplayName("adding high confidence confirmed label transitions to VALIDATED")
    void confirmedLabelValidates() {
        var candidate = createCandidate(90, "MICRO_ARC");
        var label = DataLabel.fromTechnician(DataLabel.LabelValue.MICRO_ARC_CONFIRMED, "tech-1", 0.95, "Visible arc marks");

        candidate.addLabel(label);

        assertThat(candidate.getStatus()).isEqualTo(DatasetCandidate.CandidateStatus.VALIDATED);
        assertThat(candidate.isExportable()).isTrue();
        assertThat(candidate.getPrimaryLabel()).isEqualTo(label);
    }

    @Test
    @DisplayName("adding low confidence label transitions to LABELED")
    void lowConfidenceLabelStaysLabeled() {
        var candidate = createCandidate(85, "TEMPERATURE_RISE");
        var label = DataLabel.fromTechnician(DataLabel.LabelValue.INCONCLUSIVE, "tech-1", 0.5, "Unclear");

        candidate.addLabel(label);

        assertThat(candidate.getStatus()).isEqualTo(DatasetCandidate.CandidateStatus.LABELED);
        assertThat(candidate.isExportable()).isTrue();
    }

    @Test
    @DisplayName("INSUFFICIENT quality tier is not exportable")
    void insufficientQualityNotExportable() {
        var candidate = createCandidate(30, "NOISE");
        candidate.addLabel(DataLabel.fromSystem(DataLabel.LabelValue.SENSOR_NOISE, 0.8));

        assertThat(candidate.getQualityTier()).isEqualTo(DatasetCandidate.DataQualityTier.INSUFFICIENT);
        assertThat(candidate.isExportable()).isFalse();
    }

    @Test
    @DisplayName("quality tiers are computed from score")
    void qualityTierComputation() {
        assertThat(createCandidate(95, null).getQualityTier()).isEqualTo(DatasetCandidate.DataQualityTier.HIGH);
        assertThat(createCandidate(80, null).getQualityTier()).isEqualTo(DatasetCandidate.DataQualityTier.MEDIUM);
        assertThat(createCandidate(60, null).getQualityTier()).isEqualTo(DatasetCandidate.DataQualityTier.LOW);
        assertThat(createCandidate(40, null).getQualityTier()).isEqualTo(DatasetCandidate.DataQualityTier.INSUFFICIENT);
    }

    @Test
    @DisplayName("markExported transitions to EXPORTED with timestamp")
    void markExported() {
        var candidate = createCandidate(90, "ARC");
        candidate.addLabel(DataLabel.fromLab(DataLabel.LabelValue.MICRO_ARC_CONFIRMED, "researcher", "Lab test"));

        candidate.markExported();

        assertThat(candidate.getStatus()).isEqualTo(DatasetCandidate.CandidateStatus.EXPORTED);
        assertThat(candidate.getExportedAt()).isNotNull();
    }

    @Test
    @DisplayName("reject marks candidate as REJECTED")
    void rejectCandidate() {
        var candidate = createCandidate(90, "NOISE");
        candidate.reject("Corrupted data");

        assertThat(candidate.getStatus()).isEqualTo(DatasetCandidate.CandidateStatus.REJECTED);
    }

    @Test
    @DisplayName("primary label prefers high confidence")
    void primaryLabelHighConfidence() {
        var candidate = createCandidate(90, "ARC");
        var lowConf = DataLabel.fromTechnician(DataLabel.LabelValue.MICRO_ARC_SUSPECTED, "tech-1", 0.5, "Maybe");
        var highConf = DataLabel.fromTechnician(DataLabel.LabelValue.MICRO_ARC_CONFIRMED, "tech-2", 0.95, "Confirmed");

        candidate.addLabel(lowConf);
        candidate.addLabel(highConf);

        assertThat(candidate.getPrimaryLabel().value()).isEqualTo(DataLabel.LabelValue.MICRO_ARC_CONFIRMED);
    }

    private DatasetCandidate createCandidate(int qualityScore, String anomalyType) {
        return new DatasetCandidate(
                UUID.randomUUID(),
                "ps_abc123", "ps_tenant1",
                Instant.now().minusSeconds(3600), Instant.now(),
                qualityScore, 65.0, anomalyType,
                UUID.randomUUID(), UUID.randomUUID(),
                new FeatureSummary(230, 1.5, 5.0, 0.5, 22.0, 1.0, 3.5, 5.0, 0.3, 0.5, 2, 0.95, 360, 0.88));
    }
}
