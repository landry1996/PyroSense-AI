package com.pyrosense.reporting.domain.model;

public enum ReportStatus {

    REQUESTED,
    GENERATING,
    GENERATED,
    FAILED,
    EXPIRED;

    public boolean isAvailableForDownload() {
        return this == GENERATED;
    }

    public boolean isTerminal() {
        return this == GENERATED || this == FAILED || this == EXPIRED;
    }
}
