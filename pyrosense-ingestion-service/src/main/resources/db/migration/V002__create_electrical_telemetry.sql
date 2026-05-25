-- V002: Main time-series table for electrical telemetry
-- Design decisions:
--   - No surrogate UUID PK (time-series anti-pattern). PK is (tenant_id, device_id, measured_at).
--   - Composite PK enables native chunk exclusion by tenant + device.
--   - DOUBLE PRECISION for sensor values (IEEE 754 float64, sufficient for electrical measurements).
--   - No JPA identity column: rows are append-only, never updated.

CREATE TABLE electrical_telemetry (
    tenant_id           UUID NOT NULL,
    device_id           UUID NOT NULL,
    building_id         UUID NOT NULL,
    electrical_panel_id UUID NOT NULL,
    circuit_id          UUID,
    measured_at         TIMESTAMPTZ NOT NULL,
    rms_current         DOUBLE PRECISION NOT NULL,
    rms_voltage         DOUBLE PRECISION NOT NULL,
    active_power        DOUBLE PRECISION NOT NULL,
    reactive_power      DOUBLE PRECISION NOT NULL,
    power_factor        DOUBLE PRECISION NOT NULL,
    thd                 DOUBLE PRECISION NOT NULL,
    temperature_celsius DOUBLE PRECISION NOT NULL,
    hf_noise_level      DOUBLE PRECISION NOT NULL DEFAULT 0,
    micro_arc_count     INTEGER NOT NULL DEFAULT 0,
    transient_count     INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Convert to TimescaleDB hypertable partitioned by measured_at (7-day chunks)
SELECT create_hypertable(
    'electrical_telemetry',
    'measured_at',
    chunk_time_interval => INTERVAL '7 days',
    if_not_exists => TRUE
);

-- Primary access patterns:
-- 1. Device time-range queries (analysis pipeline)
CREATE INDEX idx_et_tenant_device_time
    ON electrical_telemetry (tenant_id, device_id, measured_at DESC);

-- 2. Building-level aggregation (dashboard)
CREATE INDEX idx_et_tenant_building_time
    ON electrical_telemetry (tenant_id, building_id, measured_at DESC);

-- 3. Risk-related fields for anomaly detection queries
CREATE INDEX idx_et_risk_indicators
    ON electrical_telemetry (tenant_id, device_id, measured_at DESC)
    INCLUDE (micro_arc_count, hf_noise_level, temperature_celsius, thd);

-- 4. Micro-arc detection (sparse index - only rows with arcs)
CREATE INDEX idx_et_micro_arcs
    ON electrical_telemetry (tenant_id, device_id, measured_at DESC)
    WHERE micro_arc_count > 0;

-- 5. Circuit-level queries (when circuit_id is populated)
CREATE INDEX idx_et_circuit_time
    ON electrical_telemetry (tenant_id, circuit_id, measured_at DESC)
    WHERE circuit_id IS NOT NULL;
