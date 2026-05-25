package com.pyrosense.alerting.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class EscalationLevelTest {

    @Test
    void shouldProgressThroughLevels() {
        assertThat(EscalationLevel.NONE.next()).isEqualTo(EscalationLevel.FIRST);
        assertThat(EscalationLevel.FIRST.next()).isEqualTo(EscalationLevel.SECOND);
        assertThat(EscalationLevel.SECOND.next()).isEqualTo(EscalationLevel.EMERGENCY);
        assertThat(EscalationLevel.EMERGENCY.next()).isEqualTo(EscalationLevel.EMERGENCY);
    }

    @Test
    void shouldDetectEscalated() {
        assertThat(EscalationLevel.NONE.isEscalated()).isFalse();
        assertThat(EscalationLevel.FIRST.isEscalated()).isTrue();
        assertThat(EscalationLevel.SECOND.isEscalated()).isTrue();
        assertThat(EscalationLevel.EMERGENCY.isEscalated()).isTrue();
    }

    @Test
    void shouldHaveCorrectLevels() {
        assertThat(EscalationLevel.NONE.level()).isEqualTo(0);
        assertThat(EscalationLevel.FIRST.level()).isEqualTo(1);
        assertThat(EscalationLevel.SECOND.level()).isEqualTo(2);
        assertThat(EscalationLevel.EMERGENCY.level()).isEqualTo(3);
    }
}
