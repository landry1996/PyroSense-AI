package com.pyrosense.maintenance.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class InterventionPriorityPolicyTest {

    private final InterventionPriorityPolicy policy = new InterventionPriorityPolicy();

    @Test
    void criticalAlertMapsToUrgent() {
        assertThat(policy.determineFromAlert("CRITICAL", null)).isEqualTo(InterventionPriority.URGENT);
    }

    @Test
    void warningWithHighRiskScoreMapsToHigh() {
        assertThat(policy.determineFromAlert("WARNING", 75)).isEqualTo(InterventionPriority.HIGH);
    }

    @Test
    void warningWithLowRiskScoreMapsToMedium() {
        assertThat(policy.determineFromAlert("WARNING", 50)).isEqualTo(InterventionPriority.MEDIUM);
    }

    @Test
    void warningWithNullRiskScoreDefaultsToHigh() {
        assertThat(policy.determineFromAlert("WARNING", null)).isEqualTo(InterventionPriority.HIGH);
    }

    @Test
    void criticalTypeMapsToEmergency() {
        assertThat(policy.determineTypeFromAlert("CRITICAL")).isEqualTo(InterventionType.EMERGENCY);
    }

    @Test
    void warningTypeMapsToPreventive() {
        assertThat(policy.determineTypeFromAlert("WARNING")).isEqualTo(InterventionType.PREVENTIVE);
    }

    @Test
    void slaDeadlineForUrgent() {
        assertThat(policy.determineSlaDeadline(InterventionPriority.URGENT)).isEqualTo(Duration.ofHours(4));
    }

    @Test
    void slaDeadlineForHigh() {
        assertThat(policy.determineSlaDeadline(InterventionPriority.HIGH)).isEqualTo(Duration.ofHours(24));
    }
}
