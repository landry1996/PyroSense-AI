CREATE TABLE analysis_results (
    id                    UUID PRIMARY KEY,
    device_id             UUID NOT NULL,
    analyzed_at           TIMESTAMPTZ NOT NULL,
    anomaly_count         INTEGER NOT NULL DEFAULT 0,
    aggregate_risk_score  DOUBLE PRECISION NOT NULL DEFAULT 0,
    baseline_available    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ar_device_time ON analysis_results (device_id, analyzed_at DESC);

CREATE TABLE signal_anomalies (
    id              UUID PRIMARY KEY,
    result_id       UUID NOT NULL REFERENCES analysis_results(id),
    device_id       UUID NOT NULL,
    feature         VARCHAR(50) NOT NULL,
    anomaly_type    VARCHAR(50) NOT NULL,
    current_value   DOUBLE PRECISION NOT NULL,
    baseline_value  DOUBLE PRECISION NOT NULL,
    deviation_sigma DOUBLE PRECISION NOT NULL,
    confidence      DOUBLE PRECISION NOT NULL,
    detected_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_sa_device_time ON signal_anomalies (device_id, detected_at DESC);
CREATE INDEX idx_sa_result ON signal_anomalies (result_id);
CREATE INDEX idx_sa_type ON signal_anomalies (anomaly_type, detected_at DESC);
