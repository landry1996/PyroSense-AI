CREATE TABLE device_credentials (
    id                      UUID PRIMARY KEY,
    device_id               UUID NOT NULL,
    tenant_id               UUID NOT NULL REFERENCES tenants(id),
    token_hash              VARCHAR(128) NOT NULL,
    expires_at              TIMESTAMPTZ NOT NULL,
    active                  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_authenticated_at   TIMESTAMPTZ,
    authentication_count    INT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_device_credentials_device_active ON device_credentials(device_id) WHERE active = TRUE;
CREATE INDEX idx_device_credentials_tenant ON device_credentials(tenant_id);
CREATE INDEX idx_device_credentials_expires ON device_credentials(expires_at) WHERE active = TRUE;
