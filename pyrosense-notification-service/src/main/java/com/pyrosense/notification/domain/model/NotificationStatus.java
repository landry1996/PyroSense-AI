package com.pyrosense.notification.domain.model;

public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED,
    RETRYING;

    public boolean isTerminal() {
        return this == SENT || this == FAILED;
    }

    public boolean canRetry() {
        return this == FAILED || this == RETRYING;
    }
}
