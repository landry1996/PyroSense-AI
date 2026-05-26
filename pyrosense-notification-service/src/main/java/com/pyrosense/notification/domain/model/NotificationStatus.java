package com.pyrosense.notification.domain.model;

public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED,
    RETRYING,
    CANCELLED,
    SUPPRESSED;

    public boolean isTerminal() {
        return this == SENT || this == FAILED || this == CANCELLED || this == SUPPRESSED;
    }

    public boolean canRetry() {
        return this == FAILED || this == RETRYING;
    }
}
