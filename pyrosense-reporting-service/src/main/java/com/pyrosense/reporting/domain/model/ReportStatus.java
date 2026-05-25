package com.pyrosense.reporting.domain.model;

public enum ReportStatus {

    PENDING,
    GENERATING,
    GENERATED,
    FAILED;

    public boolean isAvailableForDownload() {
        return this == GENERATED;
    }
}
