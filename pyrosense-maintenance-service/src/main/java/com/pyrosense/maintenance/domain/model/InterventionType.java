package com.pyrosense.maintenance.domain.model;

public enum InterventionType {

    PREVENTIVE,
    CORRECTIVE,
    PREDICTIVE,
    EMERGENCY;

    public boolean requiresImmediateAction() {
        return this == EMERGENCY || this == CORRECTIVE;
    }
}
