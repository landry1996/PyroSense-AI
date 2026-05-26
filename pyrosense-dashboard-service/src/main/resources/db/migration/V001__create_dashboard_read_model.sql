-- Dashboard read model tables (materialized views for fast queries)
-- These are populated by cross-service data via Kafka events

CREATE TABLE buildings (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    address         TEXT,
    risk_score      DOUBLE PRECISION NOT NULL DEFAULT 0,
    status          VARCHAR(50) NOT NULL DEFAULT 'OK',
    total_devices   INT NOT NULL DEFAULT 0,
    active_devices  INT NOT NULL DEFAULT 0,
    last_alert_at   TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_buildings_tenant ON buildings(tenant_id);
CREATE INDEX idx_buildings_tenant_risk ON buildings(tenant_id, risk_score DESC);

CREATE TABLE devices (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    building_id     UUID,
    serial_number   VARCHAR(100) NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'PROVISIONED',
    risk_score      DOUBLE PRECISION NOT NULL DEFAULT 0,
    last_seen_at    TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_devices_tenant ON devices(tenant_id);
CREATE INDEX idx_devices_tenant_status ON devices(tenant_id, status);
CREATE INDEX idx_devices_last_seen ON devices(tenant_id, last_seen_at) WHERE last_seen_at IS NOT NULL;

CREATE TABLE alerts (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    device_id       VARCHAR(255),
    building_id     VARCHAR(255),
    title           VARCHAR(500) NOT NULL,
    severity        VARCHAR(50) NOT NULL,
    status          VARCHAR(50) NOT NULL,
    type            VARCHAR(100) NOT NULL,
    sla_breached    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alerts_tenant ON alerts(tenant_id);
CREATE INDEX idx_alerts_tenant_status ON alerts(tenant_id, status);
CREATE INDEX idx_alerts_tenant_severity ON alerts(tenant_id, severity) WHERE status IN ('OPEN', 'ACKNOWLEDGED');
CREATE INDEX idx_alerts_building ON alerts(building_id) WHERE building_id IS NOT NULL;
CREATE INDEX idx_alerts_created ON alerts(tenant_id, created_at DESC);

CREATE TABLE interventions (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    building_id     VARCHAR(255),
    type            VARCHAR(100) NOT NULL,
    priority        VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(50) NOT NULL DEFAULT 'PLANNED',
    assigned_to     VARCHAR(255),
    scheduled_date  DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_interventions_tenant ON interventions(tenant_id);
CREATE INDEX idx_interventions_tenant_status ON interventions(tenant_id, status) WHERE status NOT IN ('COMPLETED', 'CANCELLED');
CREATE INDEX idx_interventions_assigned ON interventions(assigned_to) WHERE assigned_to IS NOT NULL;
CREATE INDEX idx_interventions_overdue ON interventions(tenant_id, scheduled_date) WHERE status NOT IN ('COMPLETED', 'CANCELLED');

CREATE TABLE dashboard_risk_trend (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    date            DATE NOT NULL,
    avg_score       DOUBLE PRECISION NOT NULL DEFAULT 0,
    max_score       DOUBLE PRECISION NOT NULL DEFAULT 0,
    alert_count     INT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, date)
);

CREATE INDEX idx_risk_trend_tenant_date ON dashboard_risk_trend(tenant_id, date DESC);
