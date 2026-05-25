CREATE TABLE reports (
    id                  UUID PRIMARY KEY,
    report_number       VARCHAR(30) NOT NULL UNIQUE,
    tenant_id           UUID NOT NULL,
    building_id         UUID NOT NULL,
    type                VARCHAR(50) NOT NULL,
    period_start        TIMESTAMP NOT NULL,
    period_end          TIMESTAMP NOT NULL,
    status              VARCHAR(20) NOT NULL,
    metadata            TEXT,
    signature_hash      VARCHAR(128),
    signature_algorithm VARCHAR(20),
    content             BYTEA,
    file_name           VARCHAR(255),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    generated_at        TIMESTAMP
);

CREATE INDEX idx_reports_tenant_id ON reports(tenant_id);
CREATE INDEX idx_reports_building_id ON reports(building_id);
CREATE INDEX idx_reports_type ON reports(type);
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_tenant_type ON reports(tenant_id, type);
