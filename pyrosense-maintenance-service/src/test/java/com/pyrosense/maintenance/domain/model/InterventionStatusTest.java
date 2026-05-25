package com.pyrosense.maintenance.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class InterventionStatusTest {

    @Test
    void createdCanTransitionToPlannedOrAssignedOrCancelled() {
        assertThat(InterventionStatus.CREATED.canTransitionTo(InterventionStatus.PLANNED)).isTrue();
        assertThat(InterventionStatus.CREATED.canTransitionTo(InterventionStatus.ASSIGNED)).isTrue();
        assertThat(InterventionStatus.CREATED.canTransitionTo(InterventionStatus.CANCELLED)).isTrue();
        assertThat(InterventionStatus.CREATED.canTransitionTo(InterventionStatus.IN_PROGRESS)).isFalse();
        assertThat(InterventionStatus.CREATED.canTransitionTo(InterventionStatus.COMPLETED)).isFalse();
    }

    @Test
    void plannedCanTransitionToAssignedOrCancelled() {
        assertThat(InterventionStatus.PLANNED.canTransitionTo(InterventionStatus.ASSIGNED)).isTrue();
        assertThat(InterventionStatus.PLANNED.canTransitionTo(InterventionStatus.CANCELLED)).isTrue();
        assertThat(InterventionStatus.PLANNED.canTransitionTo(InterventionStatus.IN_PROGRESS)).isFalse();
    }

    @Test
    void assignedCanTransitionToInProgressOrCancelled() {
        assertThat(InterventionStatus.ASSIGNED.canTransitionTo(InterventionStatus.IN_PROGRESS)).isTrue();
        assertThat(InterventionStatus.ASSIGNED.canTransitionTo(InterventionStatus.CANCELLED)).isTrue();
        assertThat(InterventionStatus.ASSIGNED.canTransitionTo(InterventionStatus.COMPLETED)).isFalse();
    }

    @Test
    void inProgressCanTransitionToCompletedOrCancelled() {
        assertThat(InterventionStatus.IN_PROGRESS.canTransitionTo(InterventionStatus.COMPLETED)).isTrue();
        assertThat(InterventionStatus.IN_PROGRESS.canTransitionTo(InterventionStatus.CANCELLED)).isTrue();
        assertThat(InterventionStatus.IN_PROGRESS.canTransitionTo(InterventionStatus.ASSIGNED)).isFalse();
    }

    @Test
    void terminalStatesCannotTransition() {
        assertThat(InterventionStatus.COMPLETED.canTransitionTo(InterventionStatus.CANCELLED)).isFalse();
        assertThat(InterventionStatus.CANCELLED.canTransitionTo(InterventionStatus.CREATED)).isFalse();
    }

    @Test
    void terminalStatesAreTerminal() {
        assertThat(InterventionStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(InterventionStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(InterventionStatus.IN_PROGRESS.isTerminal()).isFalse();
    }
}
