-- V006: Retention policies and continuous aggregate refresh schedules

-- Retention: raw data kept for 90 days, then dropped automatically
SELECT add_retention_policy('electrical_telemetry', INTERVAL '90 days', if_not_exists => TRUE);
SELECT add_retention_policy('device_heartbeats', INTERVAL '90 days', if_not_exists => TRUE);
SELECT add_retention_policy('ingestion_rejections', INTERVAL '30 days', if_not_exists => TRUE);

-- Continuous aggregate refresh policies
-- 1-minute: refresh every 1 minute, look back 5 minutes
SELECT add_continuous_aggregate_policy('telemetry_1min',
    start_offset => INTERVAL '5 minutes',
    end_offset => INTERVAL '1 minute',
    schedule_interval => INTERVAL '1 minute',
    if_not_exists => TRUE
);

-- 15-minute: refresh every 5 minutes, look back 30 minutes
SELECT add_continuous_aggregate_policy('telemetry_15min',
    start_offset => INTERVAL '30 minutes',
    end_offset => INTERVAL '5 minutes',
    schedule_interval => INTERVAL '5 minutes',
    if_not_exists => TRUE
);

-- 1-hour: refresh every 15 minutes, look back 2 hours
SELECT add_continuous_aggregate_policy('telemetry_1hour',
    start_offset => INTERVAL '2 hours',
    end_offset => INTERVAL '15 minutes',
    schedule_interval => INTERVAL '15 minutes',
    if_not_exists => TRUE
);

-- Daily: refresh every hour, look back 2 days
SELECT add_continuous_aggregate_policy('telemetry_daily',
    start_offset => INTERVAL '2 days',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE
);

-- Retention on aggregates (keep aggregates longer than raw data)
-- 1-min aggregate: 180 days
-- 15-min aggregate: 1 year
-- 1-hour aggregate: 2 years
-- daily aggregate: 5 years (no auto-drop)
SELECT add_retention_policy('telemetry_1min', INTERVAL '180 days', if_not_exists => TRUE);
SELECT add_retention_policy('telemetry_15min', INTERVAL '365 days', if_not_exists => TRUE);
SELECT add_retention_policy('telemetry_1hour', INTERVAL '730 days', if_not_exists => TRUE);
