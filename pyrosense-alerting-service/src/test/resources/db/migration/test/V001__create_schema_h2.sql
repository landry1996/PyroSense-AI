CREATE TABLE alerts (
    id              VARCHAR(36) PRIMARY KEY,
    tenant_id       VARCHAR(36) NOT NULL,
    device_id       VARCHAR(36) NOT NULL,
    type            VARCHAR(50) NOT NULL,
    severity        VARCHAR(20) NOT NULL,
    title           VARCHAR(500) NOT NULL,
    description     CLOB,
    deduplication_key VARCHAR(100) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    assigned_to     VARCHAR(36),
    escalation_level VARCHAR(20) NOT NULL DEFAULT 'NONE',
    created_at      TIMESTAMP NOT NULL,
    sla_deadline    TIMESTAMP NOT NULL,
    acknowledged_at TIMESTAMP,
    acknowledged_by VARCHAR(36),
    resolved_at     TIMESTAMP,
    resolved_by     VARCHAR(36),
    resolution_note CLOB,
    last_escalated_at TIMESTAMP,
    occurrence_count INTEGER NOT NULL DEFAULT 1,
    last_occurrence_at TIMESTAMP,
    comments        CLOB DEFAULT '[]',
    building_id     VARCHAR(36)
);

CREATE INDEX idx_alerts_tenant_id ON alerts(tenant_id);
CREATE INDEX idx_alerts_device_id ON alerts(device_id);
CREATE INDEX idx_alerts_status ON alerts(status);
CREATE INDEX idx_alerts_severity ON alerts(severity);
CREATE INDEX idx_alerts_tenant_status ON alerts(tenant_id, status);
CREATE INDEX idx_alerts_building_id ON alerts(building_id);
CREATE INDEX idx_alerts_tenant_building ON alerts(tenant_id, building_id);
