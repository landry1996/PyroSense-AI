CREATE TABLE notifications (
    id                UUID PRIMARY KEY,
    tenant_id         UUID NOT NULL,
    recipient_id      UUID NOT NULL,
    channel           VARCHAR(20) NOT NULL,
    severity          VARCHAR(20) NOT NULL,
    subject           VARCHAR(500) NOT NULL,
    body              TEXT NOT NULL,
    alert_fingerprint VARCHAR(255) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    retry_count       INT NOT NULL DEFAULT 0,
    next_retry_at     TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    sent_at           TIMESTAMP,
    failure_reason    VARCHAR(1000)
);

CREATE INDEX idx_notifications_tenant_id ON notifications(tenant_id);
CREATE INDEX idx_notifications_recipient_id ON notifications(recipient_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_retry ON notifications(status, next_retry_at) WHERE status = 'RETRYING';
CREATE INDEX idx_notifications_fingerprint ON notifications(alert_fingerprint, recipient_id, channel);
