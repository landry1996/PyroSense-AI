package com.pyrosense.maintenance.domain.model;

public enum InterventionResult {

    CONFIRMED_DEFECT,
    NO_DEFECT_FOUND,
    REPAIRED,
    REPLACED_COMPONENT,
    NEEDS_FOLLOW_UP;

    public boolean isFalsePositive() {
        return this == NO_DEFECT_FOUND;
    }

    public boolean isDefectConfirmed() {
        return this == CONFIRMED_DEFECT || this == REPAIRED || this == REPLACED_COMPONENT;
    }
}
