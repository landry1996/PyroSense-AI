package com.pyrosense.maintenance.domain.model;

public enum InterventionStatus {

    CREATED,
    PLANNED,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(InterventionStatus target) {
        return switch (this) {
            case CREATED -> target == PLANNED || target == ASSIGNED || target == CANCELLED;
            case PLANNED -> target == ASSIGNED || target == CANCELLED;
            case ASSIGNED -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == COMPLETED || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
