-- V004: Rejected ingestion records for audit and debugging
CREATE TABLE ingestion_rejections (
    id              UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id       UUID,
    device_id       UUID,
    rejected_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reason          VARCHAR(500) NOT NULL,
    source          VARCHAR(20) NOT NULL DEFAULT 'mqtt',
    payload_hash    VARCHAR(64),
    violations      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

SELECT create_hypertable(
    'ingestion_rejections',
    'rejected_at',
    chunk_time_interval => INTERVAL '30 days',
    if_not_exists => TRUE
);

CREATE INDEX idx_rej_tenant_device
    ON ingestion_rejections (tenant_id, device_id, rejected_at DESC);

CREATE INDEX idx_rej_reason
    ON ingestion_rejections (reason, rejected_at DESC);
