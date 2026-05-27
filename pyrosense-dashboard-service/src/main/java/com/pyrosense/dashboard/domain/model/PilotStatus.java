package com.pyrosense.dashboard.domain.model;

public enum PilotStatus {
    PREPARING,
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean isRunning() {
        return this == ACTIVE;
    }
}
