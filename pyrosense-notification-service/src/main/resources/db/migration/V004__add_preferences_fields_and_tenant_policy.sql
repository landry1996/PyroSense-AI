ALTER TABLE notification_preferences ADD COLUMN IF NOT EXISTS language VARCHAR(5) NOT NULL DEFAULT 'fr';
ALTER TABLE notification_preferences ADD COLUMN IF NOT EXISTS critical_override_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE notification_preferences ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE notification_preferences ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE notification_preferences ADD COLUMN IF NOT EXISTS push_token_registered BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE tenant_notification_policy (
    tenant_id UUID PRIMARY KEY,
    email_enabled_by_default BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled_by_default BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled_by_default BOOLEAN NOT NULL DEFAULT TRUE,
    webhook_enabled_by_default BOOLEAN NOT NULL DEFAULT FALSE,
    critical_override_mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    require_phone_verification BOOLEAN NOT NULL DEFAULT TRUE,
    require_push_token BOOLEAN NOT NULL DEFAULT TRUE,
    default_quiet_hours_start TIME,
    default_quiet_hours_end TIME,
    default_language VARCHAR(5) NOT NULL DEFAULT 'fr',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
