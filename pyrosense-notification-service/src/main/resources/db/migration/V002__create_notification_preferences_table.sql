CREATE TABLE notification_preferences (
    user_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    webhook_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notification_preferences_tenant ON notification_preferences(tenant_id);
