CREATE TABLE tenants (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id                      UUID PRIMARY KEY,
    email                   VARCHAR(255) NOT NULL UNIQUE,
    full_name               VARCHAR(255) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts   INT NOT NULL DEFAULT 0,
    last_login_at           TIMESTAMP,
    locked_until            TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE memberships (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    roles       VARCHAR(500) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_memberships_user ON memberships(user_id);
CREATE INDEX idx_memberships_tenant ON memberships(tenant_id);

CREATE TABLE device_credentials (
    id                      UUID PRIMARY KEY,
    device_id               UUID NOT NULL,
    tenant_id               UUID NOT NULL REFERENCES tenants(id),
    token_hash              VARCHAR(128) NOT NULL,
    expires_at              TIMESTAMP NOT NULL,
    active                  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_authenticated_at   TIMESTAMP,
    authentication_count    INT NOT NULL DEFAULT 0
);

CREATE TABLE audit_log (
    id              UUID PRIMARY KEY,
    action          VARCHAR(100) NOT NULL,
    resource_type   VARCHAR(100) NOT NULL,
    resource_id     VARCHAR(255),
    user_id         UUID,
    tenant_id       UUID,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    details         TEXT,
    timestamp       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_tenant_time ON audit_log(tenant_id, timestamp);

CREATE TABLE tenant_settings (
    tenant_id UUID PRIMARY KEY REFERENCES tenants(id),
    settings TEXT NOT NULL DEFAULT '{}',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE emergency_contacts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    email VARCHAR(255),
    role VARCHAR(100) NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_emergency_contacts_tenant ON emergency_contacts(tenant_id, priority);
