package com.pyrosense.dashboard.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PilotProgramTest {

    @Test
    void newPilot_shouldHavePreparingStatus() {
        var pilot = new PilotProgram(UUID.randomUUID(), "tenant-1", "Pilot A", "Description");

        assertThat(pilot.getStatus()).isEqualTo(PilotStatus.PREPARING);
        assertThat(pilot.getStartedAt()).isNull();
        assertThat(pilot.getCompletedAt()).isNull();
        assertThat(pilot.getDeviceCount()).isZero();
    }

    @Test
    void activate_fromPreparing_shouldSetActive() {
        var pilot = new PilotProgram(UUID.randomUUID(), "tenant-1", "Pilot A", "Desc");

        pilot.activate();

        assertThat(pilot.getStatus()).isEqualTo(PilotStatus.ACTIVE);
        assertThat(pilot.getStartedAt()).isNotNull();
    }

    @Test
    void activate_fromPaused_shouldSetActive() {
        var pilot = new PilotProgram(UUID.randomUUID(), "tenant-1", "Pilot A", "Desc");
        pilot.activate();
        pilot.pause();

        pilot.activate();

        assertThat(pilot.getStatus()).isEqualTo(PilotStatus.ACTIVE);
    }

    @Test
    void activate_fromCompleted_shouldThrow() {
        var pilot = new PilotProgram(UUID.randomUUID(), "tenant-1", "Pilot A", "Desc");
        pilot.activate();
        pilot.complete();

        assertThatThrownBy(pilot::activate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void pause_fromActive_shouldSetPaused() {
        var pilot = new PilotProgram(UUID.randomUUID(), "tenant-1", "Pilot A", "Desc");
        pilot.activate();

        pilot.pause();

        assertThat(pilot.getStatus()).isEqualTo(PilotStatus.PAUSED);
    }

    @Test
    void pause_fromPreparing_shouldThrow() {
        var pilot = new PilotProgram(UUID.randomUUID(), "tenant-1", "Pilot A", "Desc");

        assertThatThrownBy(pilot::pause)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PREPARING");
    }

    @Test
    void complete_fromActive_shouldSetCompleted() {
        var pilot = new PilotProgram(UUID.randomUUID(), "tenant-1", "Pilot A", "Desc");
        pilot.activate();

        pilot.complete();

        assertThat(pilot.getStatus()).isEqualTo(PilotStatus.COMPLETED);
        assertThat(pilot.getCompletedAt()).isNotNull();
        assertThat(pilot.getStatus().isTerminal()).isTrue();
    }

    @Test
    void cancel_fromPreparing_shouldSetCancelled() {
        var pilot = new PilotProgram(UUID.randomUUID(), "tenant-1", "Pilot A", "Desc");

        pilot.cancel();

        assertThat(pilot.getStatus()).isEqualTo(PilotStatus.CANCELLED);
        assertThat(pilot.getCompletedAt()).isNotNull();
    }

    @Test
    void cancel_fromCompleted_shouldThrow() {
        var pilot = new PilotProgram(UUID.randomUUID(), "tenant-1", "Pilot A", "Desc");
        pilot.activate();
        pilot.complete();

        assertThatThrownBy(pilot::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void cancel_fromCancelled_shouldThrow() {
        var pilot = new PilotProgram(UUID.randomUUID(), "tenant-1", "Pilot A", "Desc");
        pilot.cancel();

        assertThatThrownBy(pilot::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void addDevice_shouldIncrementCount() {
        var pilot = new PilotProgram(UUID.randomUUID(), "tenant-1", "Pilot A", "Desc");
        var device = PilotDevice.plan(pilot.getId(), "dev-1", "SN-001", "Site A", "Circuit 1");

        pilot.addDevice(device);

        assertThat(pilot.getDeviceCount()).isEqualTo(1);
        assertThat(pilot.getDevices()).hasSize(1);
    }

    @Test
    void pilotStatus_isTerminal() {
        assertThat(PilotStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(PilotStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(PilotStatus.ACTIVE.isTerminal()).isFalse();
        assertThat(PilotStatus.PREPARING.isTerminal()).isFalse();
    }

    @Test
    void pilotStatus_isRunning() {
        assertThat(PilotStatus.ACTIVE.isRunning()).isTrue();
        assertThat(PilotStatus.PREPARING.isRunning()).isFalse();
        assertThat(PilotStatus.COMPLETED.isRunning()).isFalse();
    }
}
