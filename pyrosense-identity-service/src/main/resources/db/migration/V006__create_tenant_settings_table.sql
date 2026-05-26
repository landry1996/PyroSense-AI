CREATE TABLE tenant_settings (
    tenant_id UUID PRIMARY KEY REFERENCES tenants(id),
    settings JSONB NOT NULL DEFAULT '{}',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
