CREATE TABLE scoring_feedbacks (
    id                  UUID PRIMARY KEY,
    device_id           UUID NOT NULL,
    tenant_id           VARCHAR(100) NOT NULL,
    alert_id            UUID,
    intervention_id     UUID,
    outcome             VARCHAR(30) NOT NULL,
    anomaly_type        VARCHAR(50) NOT NULL,
    risk_score_at_alert DOUBLE NOT NULL DEFAULT 0,
    feedback_source     VARCHAR(50) NOT NULL,
    comment             TEXT,
    received_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE scoring_adjustments (
    id              UUID PRIMARY KEY,
    device_id       UUID NOT NULL,
    tenant_id       VARCHAR(100) NOT NULL,
    anomaly_type    VARCHAR(50) NOT NULL,
    adjustment_type VARCHAR(30) NOT NULL,
    previous_value  DOUBLE NOT NULL,
    new_value       DOUBLE NOT NULL,
    delta           DOUBLE NOT NULL,
    mode            VARCHAR(20) NOT NULL DEFAULT 'SUGGESTION_ONLY',
    reason          TEXT NOT NULL,
    applied_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
