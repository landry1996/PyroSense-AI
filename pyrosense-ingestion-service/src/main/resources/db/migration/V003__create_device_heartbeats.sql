-- V003: Device heartbeat tracking
CREATE TABLE device_heartbeats (
    tenant_id               UUID NOT NULL,
    device_id               UUID NOT NULL,
    received_at             TIMESTAMPTZ NOT NULL,
    firmware_version        VARCHAR(20),
    uptime_hours            DOUBLE PRECISION,
    memory_usage_percent    DOUBLE PRECISION,
    cpu_temperature_celsius DOUBLE PRECISION,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

SELECT create_hypertable(
    'device_heartbeats',
    'received_at',
    chunk_time_interval => INTERVAL '7 days',
    if_not_exists => TRUE
);

CREATE INDEX idx_hb_tenant_device_time
    ON device_heartbeats (tenant_id, device_id, received_at DESC);
