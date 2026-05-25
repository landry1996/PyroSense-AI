CREATE TABLE baseline_profiles (
    id              UUID PRIMARY KEY,
    device_id       UUID NOT NULL UNIQUE,
    feature_stats   JSONB NOT NULL,
    sample_count    INTEGER NOT NULL DEFAULT 0,
    minimum_samples INTEGER NOT NULL DEFAULT 100,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bp_device_id ON baseline_profiles (device_id);
