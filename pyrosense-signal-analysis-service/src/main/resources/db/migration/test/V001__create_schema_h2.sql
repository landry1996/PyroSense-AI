CREATE TABLE baseline_profiles (
    id              UUID PRIMARY KEY,
    device_id       UUID NOT NULL UNIQUE,
    feature_stats   CLOB NOT NULL,
    sample_count    INTEGER NOT NULL DEFAULT 0,
    minimum_samples INTEGER NOT NULL DEFAULT 100,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE analysis_results (
    id                    UUID PRIMARY KEY,
    device_id             UUID NOT NULL,
    analyzed_at           TIMESTAMP NOT NULL,
    anomaly_count         INTEGER NOT NULL DEFAULT 0,
    aggregate_risk_score  DOUBLE PRECISION NOT NULL DEFAULT 0,
    baseline_available    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE signal_anomalies (
    id              UUID PRIMARY KEY,
    result_id       UUID NOT NULL,
    device_id       UUID NOT NULL,
    feature         VARCHAR(50) NOT NULL,
    anomaly_type    VARCHAR(50) NOT NULL,
    current_value   DOUBLE PRECISION NOT NULL,
    baseline_value  DOUBLE PRECISION NOT NULL,
    deviation_sigma DOUBLE PRECISION NOT NULL,
    confidence      DOUBLE PRECISION NOT NULL,
    detected_at     TIMESTAMP NOT NULL
);
