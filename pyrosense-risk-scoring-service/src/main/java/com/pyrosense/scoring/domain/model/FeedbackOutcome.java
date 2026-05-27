package com.pyrosense.scoring.domain.model;

public enum FeedbackOutcome {
    CONFIRMED_DEFECT,
    FALSE_POSITIVE,
    INCONCLUSIVE,
    NO_DEFECT_FOUND;

    public boolean improvesConfidence() {
        return this == CONFIRMED_DEFECT;
    }

    public boolean reducesConfidence() {
        return this == FALSE_POSITIVE;
    }

    public boolean isNeutral() {
        return this == INCONCLUSIVE || this == NO_DEFECT_FOUND;
    }
}
