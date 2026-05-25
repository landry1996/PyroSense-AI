package com.pyrosense.maintenance.domain.model;

public enum InterventionPriority {

    LOW(1),
    MEDIUM(2),
    HIGH(3),
    URGENT(4);

    private final int level;

    InterventionPriority(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }

    public boolean isHigherThan(InterventionPriority other) {
        return this.level > other.level;
    }
}
