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
    timestamp       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_tenant_time ON audit_log(tenant_id, timestamp DESC);
CREATE INDEX idx_audit_log_user ON audit_log(user_id, timestamp DESC);
CREATE INDEX idx_audit_log_action ON audit_log(action, timestamp DESC);
