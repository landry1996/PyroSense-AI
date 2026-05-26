CREATE TABLE notification_delivery_attempts (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL,
    attempt_number INT NOT NULL,
    success BOOLEAN NOT NULL,
    error_message VARCHAR(1000),
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (notification_id) REFERENCES notifications(id)
);

CREATE INDEX idx_delivery_attempts_notification ON notification_delivery_attempts(notification_id);
CREATE INDEX idx_delivery_attempts_time ON notification_delivery_attempts(attempted_at DESC);
