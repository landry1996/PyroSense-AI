CREATE TABLE notification_delivery_attempts (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notifications(id),
    channel VARCHAR(20) NOT NULL,
    attempt_number INT NOT NULL,
    success BOOLEAN NOT NULL,
    error_message VARCHAR(1000),
    attempted_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_delivery_attempts_notification ON notification_delivery_attempts(notification_id);
CREATE INDEX idx_delivery_attempts_time ON notification_delivery_attempts(attempted_at DESC);

-- Add suppressed/cancelled status support (no schema change needed, just documenting)
-- NotificationStatus now supports: PENDING, SENT, FAILED, RETRYING, CANCELLED, SUPPRESSED
