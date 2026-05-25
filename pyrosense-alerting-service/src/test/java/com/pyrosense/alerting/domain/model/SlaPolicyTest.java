package com.pyrosense.alerting.domain.model;

import com.pyrosense.shared.valueobject.AlertSeverity;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class SlaPolicyTest {

    @Test
    void shouldReturnCorrectDeadlines() {
        SlaPolicy policy = SlaPolicy.defaults();

        assertThat(policy.deadlineFor(AlertSeverity.CRITICAL)).isEqualTo(Duration.ofHours(24));
        assertThat(policy.deadlineFor(AlertSeverity.WARNING)).isEqualTo(Duration.ofDays(7));
        assertThat(policy.deadlineFor(AlertSeverity.INFO)).isEqualTo(Duration.ofDays(30));
    }

    @Test
    void shouldHaveDefaultEscalationInterval() {
        SlaPolicy policy = SlaPolicy.defaults();

        assertThat(policy.escalationInterval()).isEqualTo(Duration.ofHours(4));
    }

    @Test
    void shouldCreateCustomPolicy() {
        SlaPolicy policy = new SlaPolicy(
                Duration.ofHours(12),
                Duration.ofDays(3),
                Duration.ofDays(14),
                Duration.ofHours(2)
        );

        assertThat(policy.criticalDeadline()).isEqualTo(Duration.ofHours(12));
        assertThat(policy.escalationInterval()).isEqualTo(Duration.ofHours(2));
    }
}
