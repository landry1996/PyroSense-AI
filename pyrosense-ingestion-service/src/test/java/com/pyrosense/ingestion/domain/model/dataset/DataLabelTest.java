package com.pyrosense.ingestion.domain.model.dataset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DataLabelTest {

    @Test
    @DisplayName("fromTechnician creates label with TECHNICIAN source")
    void fromTechnician() {
        var label = DataLabel.fromTechnician(DataLabel.LabelValue.MICRO_ARC_CONFIRMED, "tech-1", 0.95, "Arc visible");

        assertThat(label.source()).isEqualTo(DataLabel.LabelSource.TECHNICIAN);
        assertThat(label.value()).isEqualTo(DataLabel.LabelValue.MICRO_ARC_CONFIRMED);
        assertThat(label.confidence()).isEqualTo(0.95);
        assertThat(label.labeledBy()).isEqualTo("tech-1");
    }

    @Test
    @DisplayName("fromSystem creates label with SYSTEM source")
    void fromSystem() {
        var label = DataLabel.fromSystem(DataLabel.LabelValue.NORMAL, 0.85);

        assertThat(label.source()).isEqualTo(DataLabel.LabelSource.SYSTEM);
        assertThat(label.confidence()).isEqualTo(0.85);
        assertThat(label.labeledBy()).isEqualTo("system");
    }

    @Test
    @DisplayName("fromLab creates label with LAB source and full confidence")
    void fromLab() {
        var label = DataLabel.fromLab(DataLabel.LabelValue.LOOSE_CONNECTION_CONFIRMED, "dr.dupont", "Lab measurement");

        assertThat(label.source()).isEqualTo(DataLabel.LabelSource.LAB);
        assertThat(label.confidence()).isEqualTo(1.0);
        assertThat(label.justification()).isEqualTo("Lab measurement");
    }

    @Test
    @DisplayName("isConfirmed returns true for CONFIRMED labels and FALSE_POSITIVE")
    void isConfirmed() {
        assertThat(DataLabel.fromTechnician(DataLabel.LabelValue.MICRO_ARC_CONFIRMED, "t", 0.9, "").isConfirmed()).isTrue();
        assertThat(DataLabel.fromTechnician(DataLabel.LabelValue.LOOSE_CONNECTION_CONFIRMED, "t", 0.9, "").isConfirmed()).isTrue();
        assertThat(DataLabel.fromTechnician(DataLabel.LabelValue.FALSE_POSITIVE, "t", 0.9, "").isConfirmed()).isTrue();
        assertThat(DataLabel.fromTechnician(DataLabel.LabelValue.NORMAL, "t", 0.9, "").isConfirmed()).isFalse();
        assertThat(DataLabel.fromTechnician(DataLabel.LabelValue.INCONCLUSIVE, "t", 0.9, "").isConfirmed()).isFalse();
    }

    @Test
    @DisplayName("isHighConfidence returns true above 0.9 threshold")
    void isHighConfidence() {
        assertThat(DataLabel.fromTechnician(DataLabel.LabelValue.NORMAL, "t", 0.95, "").isHighConfidence()).isTrue();
        assertThat(DataLabel.fromTechnician(DataLabel.LabelValue.NORMAL, "t", 0.9, "").isHighConfidence()).isTrue();
        assertThat(DataLabel.fromTechnician(DataLabel.LabelValue.NORMAL, "t", 0.85, "").isHighConfidence()).isFalse();
    }
}
