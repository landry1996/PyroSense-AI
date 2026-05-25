-- V005: Continuous aggregates for pre-computed rollups
-- TimescaleDB materializes these incrementally as new data arrives.

-- 1-minute average (used by real-time dashboard and analysis pipeline)
CREATE MATERIALIZED VIEW telemetry_1min
WITH (timescaledb.continuous) AS
SELECT
    tenant_id,
    device_id,
    building_id,
    electrical_panel_id,
    time_bucket('1 minute', measured_at) AS bucket,
    AVG(rms_current) AS avg_rms_current,
    AVG(rms_voltage) AS avg_rms_voltage,
    AVG(active_power) AS avg_active_power,
    AVG(reactive_power) AS avg_reactive_power,
    AVG(power_factor) AS avg_power_factor,
    AVG(thd) AS avg_thd,
    AVG(temperature_celsius) AS avg_temperature,
    AVG(hf_noise_level) AS avg_hf_noise,
    SUM(micro_arc_count) AS total_micro_arcs,
    SUM(transient_count) AS total_transients,
    MAX(temperature_celsius) AS max_temperature,
    MAX(hf_noise_level) AS max_hf_noise,
    COUNT(*) AS sample_count
FROM electrical_telemetry
GROUP BY tenant_id, device_id, building_id, electrical_panel_id, bucket
WITH NO DATA;

-- 15-minute average (used by risk scoring)
CREATE MATERIALIZED VIEW telemetry_15min
WITH (timescaledb.continuous) AS
SELECT
    tenant_id,
    device_id,
    building_id,
    electrical_panel_id,
    time_bucket('15 minutes', measured_at) AS bucket,
    AVG(rms_current) AS avg_rms_current,
    AVG(rms_voltage) AS avg_rms_voltage,
    AVG(active_power) AS avg_active_power,
    AVG(reactive_power) AS avg_reactive_power,
    AVG(power_factor) AS avg_power_factor,
    AVG(thd) AS avg_thd,
    AVG(temperature_celsius) AS avg_temperature,
    AVG(hf_noise_level) AS avg_hf_noise,
    SUM(micro_arc_count) AS total_micro_arcs,
    SUM(transient_count) AS total_transients,
    MAX(temperature_celsius) AS max_temperature,
    MAX(hf_noise_level) AS max_hf_noise,
    MAX(rms_current) AS max_rms_current,
    STDDEV(rms_current) AS stddev_rms_current,
    STDDEV(temperature_celsius) AS stddev_temperature,
    COUNT(*) AS sample_count
FROM electrical_telemetry
GROUP BY tenant_id, device_id, building_id, electrical_panel_id, bucket
WITH NO DATA;

-- 1-hour average (used by reporting and trend analysis)
CREATE MATERIALIZED VIEW telemetry_1hour
WITH (timescaledb.continuous) AS
SELECT
    tenant_id,
    device_id,
    building_id,
    electrical_panel_id,
    time_bucket('1 hour', measured_at) AS bucket,
    AVG(rms_current) AS avg_rms_current,
    AVG(rms_voltage) AS avg_rms_voltage,
    AVG(active_power) AS avg_active_power,
    AVG(reactive_power) AS avg_reactive_power,
    AVG(power_factor) AS avg_power_factor,
    AVG(thd) AS avg_thd,
    AVG(temperature_celsius) AS avg_temperature,
    AVG(hf_noise_level) AS avg_hf_noise,
    SUM(micro_arc_count) AS total_micro_arcs,
    SUM(transient_count) AS total_transients,
    MAX(temperature_celsius) AS max_temperature,
    MAX(hf_noise_level) AS max_hf_noise,
    MAX(rms_current) AS max_rms_current,
    COUNT(*) AS sample_count
FROM electrical_telemetry
GROUP BY tenant_id, device_id, building_id, electrical_panel_id, bucket
WITH NO DATA;

-- Daily risk score summary (used by daily reports and alerting thresholds)
CREATE MATERIALIZED VIEW telemetry_daily
WITH (timescaledb.continuous) AS
SELECT
    tenant_id,
    device_id,
    building_id,
    electrical_panel_id,
    time_bucket('1 day', measured_at) AS bucket,
    AVG(rms_current) AS avg_rms_current,
    AVG(active_power) AS avg_active_power,
    AVG(thd) AS avg_thd,
    AVG(temperature_celsius) AS avg_temperature,
    SUM(micro_arc_count) AS total_micro_arcs,
    SUM(transient_count) AS total_transients,
    MAX(temperature_celsius) AS max_temperature,
    MAX(hf_noise_level) AS max_hf_noise,
    MAX(rms_current) AS peak_current,
    COUNT(*) AS sample_count
FROM electrical_telemetry
GROUP BY tenant_id, device_id, building_id, electrical_panel_id, bucket
WITH NO DATA;
