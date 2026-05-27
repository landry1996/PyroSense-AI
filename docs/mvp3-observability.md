# MVP 3 Observability — Real Device Monitoring

## Overview

This document describes the observability stack specific to MVP 3 real device monitoring. It extends the existing platform observability with metrics, alerts, logging, and tracing tailored to the real sensor pipeline (MQTT v1 protocol, HMAC signatures, replay detection, offline buffer management).

## Architecture

```
Real Device (ESP32) → MQTT Broker → MqttTelemetryListener → Protocol Validation Pipeline
                                                                      ↓
                                            ┌─────────────────────────┴──────────────────────┐
                                            │                                                │
                                      [ACCEPTED]                                       [REJECTED]
                                            │                                                │
                                   Quality Assessment                            Rejection Counter
                                            │                                     (by reason)
                                   Ingest Use Case                                       │
                                            │                                    Security Alerts
                                     Kafka Publish                              (signature/replay)
                                            │
                                   Signal Analysis → Risk Scoring
```

## Metrics (14 total)

### Counters

| Metric | Description | Labels |
|--------|-------------|--------|
| `pyrosense_mvp3_real_device_telemetry_received_total` | Messages from real devices | `source=mqtt_v1` |
| `pyrosense_mvp3_signature_invalid_total` | Invalid HMAC-SHA256 signatures | — |
| `pyrosense_mvp3_replay_detected_total` | Replay attacks blocked | — |
| `pyrosense_mvp3_low_signal_quality_total` | Low signal quality events | — |
| `pyrosense_mvp3_clock_drift_total` | Clock drift events detected | — |
| `pyrosense_mvp3_offline_queue_flush_total` | Offline buffer flush events | — |

### Gauges

| Metric | Description |
|--------|-------------|
| `pyrosense_mvp3_device_offline_duration_seconds` | Max offline duration across devices |
| `pyrosense_mvp3_signal_quality_score` | Latest signal quality (0-100) |
| `pyrosense_mvp3_data_quality_score` | Latest data quality (0-100) |
| `pyrosense_mvp3_firmware_version_count` | Distinct firmware versions in fleet |
| `pyrosense_mvp3_pilot_active_devices` | Active devices in pilot program |
| `pyrosense_mvp3_pilot_data_quality_average` | Average data quality across pilot |
| `pyrosense_mvp3_pilot_incidents_total` | Total pilot incidents |

### Timers (Histogram)

| Metric | Description | Percentiles |
|--------|-------------|-------------|
| `pyrosense_mvp3_validation_duration_seconds` | Protocol validation pipeline duration | P50, P95, P99 |

## Prometheus Alert Rules (10 rules — group `pyrosense-mvp3`)

| Alert | Condition | Severity | Description |
|-------|-----------|----------|-------------|
| `Mvp3SignatureInvalidSpike` | rate > 0.5/s for 2m | critical | Possible device compromise |
| `Mvp3ReplayAttackDetected` | increase > 3 in 5m | critical | Active replay attack |
| `Mvp3DeviceOfflineLong` | offline > 1h for 5m | warning | Device connectivity issue |
| `Mvp3LowSignalQualityHigh` | rate > 0.3/s for 5m | warning | Placement or interference |
| `Mvp3ClockDriftFrequent` | rate > 0.2/s for 10m | warning | NTP sync failure |
| `Mvp3NoRealDeviceData` | rate = 0 for 15m | critical | All real devices down |
| `Mvp3ValidationLatencyHigh` | P95 > 500ms for 3m | warning | Pipeline bottleneck |
| `Mvp3DataQualityLow` | score < 60 for 10m | warning | Below acceptable threshold |
| `Mvp3PilotDevicesInactive` | active < 5 for 10m | warning | Pilot coverage gap |
| `Mvp3FirmwareVersionDrift` | versions > 3 for 30m | warning | Update campaign needed |

## Structured Logging

### MDC Fields (automatically included in JSON logs)

| Field | Source | Purpose |
|-------|--------|---------|
| `traceId` | Micrometer Tracing (W3C traceparent) | Distributed trace correlation |
| `spanId` | Micrometer Tracing | Span-level correlation |
| `correlationId` | X-Correlation-Id header / generated UUID | Request-level correlation |
| `tenantId` | JWT claim / MQTT topic | Tenant isolation audit |
| `deviceId` | MQTT topic / request param | Device-level filtering |
| `firmwareVersion` | Telemetry payload | Version-specific debugging |
| `schemaVersion` | Payload `v` field | Protocol version tracking |
| `rejectionReason` | Validation pipeline result code | Rejection analysis |

### Security Constraints

- **No payload content in logs** — raw telemetry payloads are never logged
- **No device secrets** — HMAC keys, credentials never appear in any log
- **No PII** — device serial numbers masked in non-debug levels
- **Rejection reason only** — logs the code/category, not the raw data that failed

### Logback Configuration

- **Local/test profiles**: Console with pattern including `[traceId=%X{traceId:-} spanId=%X{spanId:-}]`
- **Docker/prod profiles**: JSON structured logging via `LoggingEventCompositeJsonEncoder`
- **MDC provider**: Automatically includes all MDC fields in JSON output

## Distributed Tracing

### Trace Flow

```
MQTT Ingestion          Validation           Persistence         Kafka              Analysis           Scoring
     │                      │                     │                │                    │                  │
     ├─ mqtt.receive ──────►│                     │                │                    │                  │
     │                      ├─ validate.signature │                │                    │                  │
     │                      ├─ validate.replay    │                │                    │                  │
     │                      ├─ validate.schema    │                │                    │                  │
     │                      │◄────────────────────┤                │                    │                  │
     │                      │                     ├─ persist.batch │                    │                  │
     │                      │                     │◄───────────────┤                    │                  │
     │                      │                     │                ├─ kafka.produce ────►│                  │
     │                      │                     │                │                    ├─ analyze.signal──►│
     │                      │                     │                │                    │                  ├─ score.risk
```

### Configuration

```yaml
management:
  tracing:
    sampling:
      probability: ${TRACING_SAMPLING:1.0}
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}
```

- **Protocol**: OTLP HTTP (port 4318)
- **Propagation**: W3C traceparent across HTTP and Kafka headers
- **Sampling**: 100% in dev/pilot, configurable for production
- **Collector**: OpenTelemetry Collector with batch processor

## Grafana Dashboards (4)

| Dashboard | UID | Purpose |
|-----------|-----|---------|
| MVP3 - Device Fleet Health | `mvp3-device-fleet-health` | Telemetry rates, signal/data quality gauges, offline duration, firmware versions |
| MVP3 - Pilot Monitoring | `mvp3-pilot-monitoring` | Active devices, quality averages, incidents, security events timeline |
| MVP3 - Ingestion Security | `mvp3-ingestion-security` | Signature failures, replay attacks, validation latency, rejection breakdown |
| MVP3 - Data Quality | `mvp3-data-quality` | Quality scores, rejection rates, offline events, drift tracking |

### Dashboard Provisioning

Dashboards are provisioned via Grafana's file-based provisioning:
- Path: `infra/grafana/provisioning/dashboards/json/mvp3-*.json`
- Auto-discovery via existing `dashboards.yml` provider configuration

## Testing

### Metrics Test (`Mvp3ObservabilityConfigTest.java`)

Validates:
- All 14 metrics are registered with correct names
- Counter increments work
- Gauge updates reflect correct values
- Timer records durations with percentiles
- No sensitive data in metric names or labels

### Integration Verification

- `/actuator/prometheus` endpoint exposes all MVP3 metrics
- Labels are correctly applied (`source=mqtt_v1`)
- Histograms produce `_bucket`, `_count`, `_sum` suffixes

## Operational Runbook

### Alert: Mvp3SignatureInvalidSpike

1. Check if a specific device is the source (filter by deviceId in logs)
2. Verify the device's HMAC key hasn't been rotated without updating the cloud
3. Check for key provisioning issues in the identity service
4. If widespread: possible key compromise → revoke affected device credentials

### Alert: Mvp3NoRealDeviceData

1. Check MQTT broker health (`emqx_client_connected`)
2. Verify device network connectivity (heartbeat status)
3. Check ingestion service health (`/actuator/health`)
4. Review recent deployments for breaking changes

### Alert: Mvp3DeviceOfflineLong

1. Check device heartbeat history in dashboard
2. Verify network infrastructure (WiFi AP, LoRaWAN gateway)
3. Check if device is in offline buffer mode (offline_queue_flush events expected on reconnect)
4. Contact field team if >4h offline
