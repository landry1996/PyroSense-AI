# Real Device Ingestion — Backend Adaptation for MVP 3

## Overview

The ingestion service has been adapted to receive telemetry from real PyroSense firmware devices running Protocol v1, while maintaining backward compatibility with the simulator used in MVP 1.

## Architecture

```
Real Device (ESP32-S3)                         Simulator
       │                                           │
       │ pyrosense/v1/{tenant}/{device}/telemetry  │ pyrosense/{tenant}/{device}/telemetry
       │                                           │
       ▼                                           ▼
┌──────────────────────────────────────────────────────────┐
│                    MqttTelemetryListener                   │
│  ┌─────────────────────┐  ┌────────────────────────────┐ │
│  │  V1 Protocol Path   │  │  Legacy (Simulator) Path   │ │
│  │  ──────────────────  │  │  ──────────────────────── │ │
│  │  PayloadValidator    │  │  Direct deserialization    │ │
│  │  DeviceStatusChecker │  │  No signature/replay      │ │
│  │  SignatureVerifier   │  │                            │ │
│  │  AntiReplayGuard     │  │                            │ │
│  │  QualityValidator    │  │                            │ │
│  │  ClockDriftDetector  │  │                            │ │
│  └─────────────────────┘  └────────────────────────────┘ │
│                          │                                │
│                ┌─────────▼──────────┐                     │
│                │ IngestTelemetryUseCase │                  │
│                └──────────┬─────────┘                     │
└───────────────────────────┼───────────────────────────────┘
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
       TimescaleDB       Kafka        Redis
       (readings)     (events)    (idempotency)
```

## Topic Format

### V1 Protocol (Real Devices)
```
pyrosense/v1/{tenantId}/{deviceId}/telemetry
pyrosense/v1/{tenantId}/{deviceId}/heartbeat
pyrosense/v1/{tenantId}/{deviceId}/events
pyrosense/v1/{tenantId}/{deviceId}/command-acks
```

### Legacy (Simulator — backward compatible)
```
pyrosense/{tenantId}/{deviceId}/telemetry
pyrosense/{tenantId}/{deviceId}/heartbeat
pyrosense/{tenantId}/{deviceId}/events
```

## Protocol Validation Pipeline

For v1 messages, the full security pipeline is applied:

1. **Schema Validation** — Check `schemaVersion` is supported (`1.0`)
2. **Topic/Payload Coherence** — `deviceId` and `tenantId` in payload must match topic
3. **Field Validation** — All required fields present, ranges validated
4. **Device Authorization** — Device must exist and not be revoked
5. **Signature Verification** — HMAC-SHA256 with constant-time comparison
6. **Anti-Replay Protection** — Nonce uniqueness, message ID deduplication, sequence monotonicity
7. **Quality Assessment** — Signal quality scoring, HF noise level
8. **Clock Drift Detection** — Device vs server timestamp comparison

## Rejection Handling

All rejections are:
- Stored in `ingestion_rejections` table (tenant-isolated)
- Published as `TelemetryRejectedEvent` to Kafka
- Counted by specific metrics
- Logged with structured context

### Rejection Reasons

| Code | Description |
|------|-------------|
| `INVALID_SIGNATURE` | HMAC-SHA256 verification failed |
| `REPLAY_DETECTED` | Nonce reuse, duplicate message, or sequence regression |
| `DEVICE_REVOKED` | Device has been revoked |
| `UNKNOWN_DEVICE` | Device not registered |
| `INVALID_SCHEMA_VERSION` | Unsupported protocol version |
| `PAYLOAD_TOO_OLD` | Timestamp exceeds 72h age threshold |
| `INVALID_MEASUREMENT_RANGE` | Measurement values outside acceptable range |
| `LOW_SIGNAL_QUALITY` | Signal quality below 0.3 threshold |

## Signal Quality Assessment

Each telemetry message is evaluated for measurement reliability:

| Level | Score Range | Action |
|-------|-------------|--------|
| EXCELLENT | ≥ 0.9 | None |
| GOOD | 0.7 – 0.9 | None |
| DEGRADED | 0.5 – 0.7 | Warning logged |
| POOR | 0.3 – 0.5 | Alert event published |
| CRITICAL | < 0.3 | Alert event, data flagged unreliable |

Additional quality checks:
- HF noise level > 0.8 (interference)
- Voltage/current near noise floor
- Unusually short sampling windows

## Clock Drift Detection

| Severity | Drift Range | Action |
|----------|-------------|--------|
| NONE | < 30s | Normal |
| MINOR | 30s – 2min | Logged |
| MODERATE | 2min – 5min | Alert event published |
| SEVERE | > 5min | Alert event, investigate device |

## Events Published

| Event | Topic | Trigger |
|-------|-------|---------|
| `RealDeviceTelemetryReceivedEvent` | `telemetry-events` | Every successful v1 ingestion |
| `TelemetryRejectedEvent` | `telemetry-events` | Every rejection |
| `DeviceClockDriftDetectedEvent` | `telemetry-events` | Moderate/severe clock drift |
| `LowSignalQualityDetectedEvent` | `telemetry-events` | Signal quality ≤ POOR |

## Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `real_device_telemetry_received_total` | Counter | Successful v1 ingestions |
| `telemetry_signature_invalid_total` | Counter | HMAC verification failures |
| `telemetry_replay_detected_total` | Counter | Replay attacks blocked |
| `low_signal_quality_total` | Counter | Low quality alerts |
| `device_clock_drift_total` | Counter | Clock drift events |

## REST API — Device Quality Dashboard

### GET /api/v1/devices/quality/summary
Returns aggregated quality metrics (requires PLATFORM_ADMIN/TENANT_ADMIN/DEVICE_MANAGER).

### GET /api/v1/devices/quality/devices/{deviceId}
Returns quality details for a specific device.

## Ports (Hexagonal Architecture)

### New Inbound Ports
- None (MQTT listener handles routing directly)

### New Outbound Ports
| Port | Purpose |
|------|---------|
| `DeviceAuthenticationPort` | Check device status, retrieve HMAC keys |
| `DeviceSignatureVerificationPort` | Verify HMAC-SHA256 signatures |
| `DeviceCapabilityLookupPort` | Query device capabilities (real vs simulator) |

## Security Constraints

- No raw electrical data exposed in cloud APIs (processed aggregates only)
- HMAC keys never logged or exposed
- Constant-time signature comparison prevents timing attacks
- Anti-replay window: 24h nonce TTL, persistent sequence tracking
- Tenant isolation enforced at every level
- Rate limiting via Redis-backed idempotency

## Database Schema

Uses existing `ingestion_rejections` table (V004 migration):
```sql
CREATE TABLE ingestion_rejections (
    id BIGSERIAL,
    tenant_id UUID,
    device_id UUID,
    rejected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reason TEXT NOT NULL,
    source TEXT,
    payload_hash TEXT,
    violations TEXT
);
```

## Configuration

```yaml
pyrosense:
  mqtt:
    broker-url: tcp://mqtt-broker:1883
    enabled: true
  device-service:
    url: http://device-service:8082
```
