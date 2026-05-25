CREATE TABLE risk_assessments (
    id                      UUID PRIMARY KEY,
    device_id               UUID NOT NULL,
    panel_id                UUID,
    circuit_id              UUID,
    score                   INTEGER NOT NULL,
    level                   VARCHAR(20) NOT NULL,
    trend                   VARCHAR(20) NOT NULL,
    factors                 CLOB NOT NULL,
    predicted_incident_days BIGINT,
    recommendation          TEXT NOT NULL,
    computed_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
