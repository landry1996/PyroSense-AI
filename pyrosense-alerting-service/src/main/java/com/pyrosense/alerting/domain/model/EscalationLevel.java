package com.pyrosense.alerting.domain.model;

public enum EscalationLevel {
    NONE(0),
    FIRST(1),
    SECOND(2),
    EMERGENCY(3);

    private final int level;

    EscalationLevel(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }

    public EscalationLevel next() {
        return switch (this) {
            case NONE -> FIRST;
            case FIRST -> SECOND;
            case SECOND, EMERGENCY -> EMERGENCY;
        };
    }

    public boolean isEscalated() {
        return this != NONE;
    }
}
