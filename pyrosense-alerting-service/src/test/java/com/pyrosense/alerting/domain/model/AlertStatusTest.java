package com.pyrosense.alerting.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

class AlertStatusTest {

    @Test
    void openCanTransitionToAllNonTerminal() {
        assertThat(AlertStatus.OPEN.canTransitionTo(AlertStatus.ACKNOWLEDGED)).isTrue();
        assertThat(AlertStatus.OPEN.canTransitionTo(AlertStatus.IN_PROGRESS)).isTrue();
        assertThat(AlertStatus.OPEN.canTransitionTo(AlertStatus.RESOLVED)).isTrue();
        assertThat(AlertStatus.OPEN.canTransitionTo(AlertStatus.FALSE_POSITIVE)).isTrue();
    }

    @Test
    void acknowledgedCanTransitionForward() {
        assertThat(AlertStatus.ACKNOWLEDGED.canTransitionTo(AlertStatus.IN_PROGRESS)).isTrue();
        assertThat(AlertStatus.ACKNOWLEDGED.canTransitionTo(AlertStatus.RESOLVED)).isTrue();
        assertThat(AlertStatus.ACKNOWLEDGED.canTransitionTo(AlertStatus.FALSE_POSITIVE)).isTrue();
        assertThat(AlertStatus.ACKNOWLEDGED.canTransitionTo(AlertStatus.OPEN)).isFalse();
    }

    @Test
    void inProgressCanOnlyResolve() {
        assertThat(AlertStatus.IN_PROGRESS.canTransitionTo(AlertStatus.RESOLVED)).isTrue();
        assertThat(AlertStatus.IN_PROGRESS.canTransitionTo(AlertStatus.FALSE_POSITIVE)).isTrue();
        assertThat(AlertStatus.IN_PROGRESS.canTransitionTo(AlertStatus.OPEN)).isFalse();
        assertThat(AlertStatus.IN_PROGRESS.canTransitionTo(AlertStatus.ACKNOWLEDGED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(AlertStatus.class)
    void resolvedCannotTransition(AlertStatus target) {
        assertThat(AlertStatus.RESOLVED.canTransitionTo(target)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(AlertStatus.class)
    void falsePositiveCannotTransition(AlertStatus target) {
        assertThat(AlertStatus.FALSE_POSITIVE.canTransitionTo(target)).isFalse();
    }

    @Test
    void terminalStatuses() {
        assertThat(AlertStatus.RESOLVED.isTerminal()).isTrue();
        assertThat(AlertStatus.FALSE_POSITIVE.isTerminal()).isTrue();
        assertThat(AlertStatus.OPEN.isTerminal()).isFalse();
        assertThat(AlertStatus.ACKNOWLEDGED.isTerminal()).isFalse();
        assertThat(AlertStatus.IN_PROGRESS.isTerminal()).isFalse();
    }
}
