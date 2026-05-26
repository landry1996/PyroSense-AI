package com.pyrosense.maintenance.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class SlaPolicyTest {

    @Test
    void urgentSla() {
        SlaPolicy sla = SlaPolicy.forPriority(InterventionPriority.URGENT);
        assertThat(sla.responseDeadline()).isEqualTo(Duration.ofHours(4));
        assertThat(sla.resolutionDeadline()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void highSla() {
        SlaPolicy sla = SlaPolicy.forPriority(InterventionPriority.HIGH);
        assertThat(sla.responseDeadline()).isEqualTo(Duration.ofHours(24));
        assertThat(sla.resolutionDeadline()).isEqualTo(Duration.ofDays(3));
    }

    @Test
    void mediumSla() {
        SlaPolicy sla = SlaPolicy.forPriority(InterventionPriority.MEDIUM);
        assertThat(sla.responseDeadline()).isEqualTo(Duration.ofDays(3));
        assertThat(sla.resolutionDeadline()).isEqualTo(Duration.ofDays(7));
    }

    @Test
    void responseBreached() {
        SlaPolicy sla = SlaPolicy.forPriority(InterventionPriority.URGENT);
        assertThat(sla.isResponseBreached(Duration.ofHours(3))).isFalse();
        assertThat(sla.isResponseBreached(Duration.ofHours(5))).isTrue();
    }

    @Test
    void resolutionBreached() {
        SlaPolicy sla = SlaPolicy.forPriority(InterventionPriority.HIGH);
        assertThat(sla.isResolutionBreached(Duration.ofDays(2))).isFalse();
        assertThat(sla.isResolutionBreached(Duration.ofDays(4))).isTrue();
    }
}
