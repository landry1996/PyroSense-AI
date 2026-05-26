CREATE TABLE audit_log (
    id              UUID PRIMARY KEY,
    action          VARCHAR(100) NOT NULL,
    resource_type   VARCHAR(100) NOT NULL,
    resource_id     VARCHAR(255),
    user_id         UUID,
    actor_role      VARCHAR(50),
    tenant_id       UUID,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    correlation_id  VARCHAR(100),
    details         TEXT,
    metadata        TEXT,
    timestamp       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_tenant_time ON audit_log(tenant_id, timestamp DESC);
CREATE INDEX idx_audit_log_action ON audit_log(action, timestamp DESC);
CREATE INDEX idx_audit_log_resource ON audit_log(resource_type, timestamp DESC);
CREATE INDEX idx_audit_log_user ON audit_log(user_id, timestamp DESC);
CREATE INDEX idx_audit_log_correlation ON audit_log(correlation_id);
