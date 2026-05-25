CREATE TABLE risk_assessments (
    id                      UUID PRIMARY KEY,
    device_id               UUID NOT NULL,
    panel_id                UUID,
    circuit_id              UUID,
    score                   INTEGER NOT NULL CHECK (score >= 0 AND score <= 100),
    level                   VARCHAR(20) NOT NULL,
    trend                   VARCHAR(20) NOT NULL,
    factors                 JSONB NOT NULL,
    predicted_incident_days BIGINT,
    recommendation          TEXT NOT NULL,
    computed_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ra_device_time ON risk_assessments (device_id, computed_at DESC);
CREATE INDEX idx_ra_panel_time ON risk_assessments (panel_id, computed_at DESC) WHERE panel_id IS NOT NULL;
CREATE INDEX idx_ra_level ON risk_assessments (level, computed_at DESC);
CREATE INDEX idx_ra_score ON risk_assessments (score DESC, computed_at DESC);
