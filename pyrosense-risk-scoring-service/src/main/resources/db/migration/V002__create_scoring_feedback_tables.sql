CREATE TABLE scoring_feedbacks (
    id                  UUID PRIMARY KEY,
    device_id           UUID NOT NULL,
    tenant_id           VARCHAR(100) NOT NULL,
    alert_id            UUID,
    intervention_id     UUID,
    outcome             VARCHAR(30) NOT NULL,
    anomaly_type        VARCHAR(50) NOT NULL,
    risk_score_at_alert DOUBLE PRECISION NOT NULL DEFAULT 0,
    feedback_source     VARCHAR(50) NOT NULL,
    comment             TEXT,
    received_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sf_tenant_device ON scoring_feedbacks (tenant_id, device_id, received_at DESC);
CREATE INDEX idx_sf_device_anomaly ON scoring_feedbacks (device_id, anomaly_type, outcome);
CREATE INDEX idx_sf_outcome ON scoring_feedbacks (outcome, received_at DESC);

CREATE TABLE scoring_adjustments (
    id              UUID PRIMARY KEY,
    device_id       UUID NOT NULL,
    tenant_id       VARCHAR(100) NOT NULL,
    anomaly_type    VARCHAR(50) NOT NULL,
    adjustment_type VARCHAR(30) NOT NULL,
    previous_value  DOUBLE PRECISION NOT NULL,
    new_value       DOUBLE PRECISION NOT NULL,
    delta           DOUBLE PRECISION NOT NULL,
    mode            VARCHAR(20) NOT NULL DEFAULT 'SUGGESTION_ONLY',
    reason          TEXT NOT NULL,
    applied_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sa_device_anomaly ON scoring_adjustments (device_id, anomaly_type, applied_at ASC);
CREATE INDEX idx_sa_tenant ON scoring_adjustments (tenant_id, applied_at DESC);
