-- Claim tokens: single-use, time-limited tokens for device enrollment
CREATE TABLE claim_tokens (
    id              UUID PRIMARY KEY,
    device_id       UUID NOT NULL REFERENCES devices(id),
    tenant_id       UUID NOT NULL,
    token_hash      VARCHAR(128) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NOT NULL,
    created_by      VARCHAR(100) NOT NULL,
    consumed        BOOLEAN NOT NULL DEFAULT FALSE,
    consumed_at     TIMESTAMPTZ
);

CREATE INDEX idx_claim_tokens_device_id ON claim_tokens(device_id);
CREATE INDEX idx_claim_tokens_active ON claim_tokens(device_id) WHERE consumed = FALSE;

-- Device credentials: HMAC keys issued during provisioning
CREATE TABLE device_credentials (
    id              UUID PRIMARY KEY,
    device_id       UUID NOT NULL REFERENCES devices(id),
    hmac_key_hash   VARCHAR(128) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    issued_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at      TIMESTAMPTZ,
    version         INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_device_credentials_device_id ON device_credentials(device_id);
CREATE INDEX idx_device_credentials_active ON device_credentials(device_id, status) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX idx_device_credentials_version ON device_credentials(device_id, version);

-- Provisioning sessions: audit trail of enrollment attempts
CREATE TABLE provisioning_sessions (
    id              UUID PRIMARY KEY,
    device_id       UUID NOT NULL,
    tenant_id       UUID NOT NULL,
    device_serial   VARCHAR(50) NOT NULL,
    device_model    VARCHAR(50),
    firmware_version VARCHAR(20) NOT NULL,
    source_ip       VARCHAR(45),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason  VARCHAR(200),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ
);

CREATE INDEX idx_provisioning_sessions_device ON provisioning_sessions(device_id);
CREATE INDEX idx_provisioning_sessions_ip ON provisioning_sessions(source_ip, created_at);
CREATE INDEX idx_provisioning_sessions_status ON provisioning_sessions(status, created_at);
