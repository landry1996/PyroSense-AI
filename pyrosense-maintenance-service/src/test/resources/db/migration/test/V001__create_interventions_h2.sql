CREATE TABLE interventions (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    source_alert_id UUID NOT NULL,
    device_id       UUID NOT NULL,
    type            VARCHAR(20) NOT NULL,
    priority        VARCHAR(10) NOT NULL,
    description     TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    assigned_electrician_id UUID,
    scheduled_at    TIMESTAMP WITH TIME ZONE,
    started_at      TIMESTAMP WITH TIME ZONE,
    completed_at    TIMESTAMP WITH TIME ZONE,
    result          VARCHAR(30),
    diagnostic      CLOB,
    risk_impact     CLOB,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_interventions_tenant ON interventions(tenant_id);
CREATE INDEX idx_interventions_tenant_status ON interventions(tenant_id, status);
CREATE INDEX idx_interventions_device ON interventions(device_id);
CREATE UNIQUE INDEX idx_interventions_alert ON interventions(source_alert_id);
