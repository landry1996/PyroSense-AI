# PyroSense Observability

## Stack

| Component | Role | Port |
|-----------|------|------|
| Spring Boot Actuator | Health checks, info, metrics endpoints | Per service |
| Micrometer | Metrics instrumentation (counters, gauges, histograms) | - |
| Prometheus | Metrics collection and alerting | 9090 |
| Grafana | Dashboards and visualization | 3000 |
| Loki | Log aggregation | 3100 |
| OpenTelemetry Collector | Distributed tracing and telemetry pipeline | 4317/4318 |
| Logstash Logback Encoder | JSON structured logging | - |

## Actuator Endpoints

### Exposed (all services)

| Endpoint | Access | Purpose |
|----------|--------|---------|
| `/actuator/health` | Public | Service health check (K8s probes) |
| `/actuator/info` | Public | Service version/build info |
| `/actuator/prometheus` | Prometheus scrape | Metrics in Prometheus format |
| `/actuator/metrics` | Authenticated | Metrics browser |

### Disabled/Protected

| Endpoint | Policy |
|----------|--------|
| `/actuator/env` | Not exposed |
| `/actuator/configprops` | Not exposed |
| `/actuator/beans` | Not exposed |
| `/actuator/heapdump` | Not exposed |
| `/actuator/threaddump` | Not exposed |
| `/actuator/shutdown` | Disabled |

Only `health`, `info`, `prometheus`, and `metrics` are exposed. All other actuator endpoints are excluded from web exposure.

## Business Metrics

### Ingestion Service

| Metric | Type | Labels | Description |
|--------|------|--------|-------------|
| `pyrosense_telemetry_received_total` | Counter | tenant_id | Telemetry readings received |
| `pyrosense_telemetry_rejected_total` | Counter | tenant_id, reason | Telemetry rejected (validation, auth, duplicate) |
| `pyrosense_telemetry_processing_duration_seconds` | Histogram | - | Ingestion processing time |
| `pyrosense_device_heartbeat_total` | Counter | tenant_id | Heartbeats received |

### Signal Analysis Service

| Metric | Type | Labels | Description |
|--------|------|--------|-------------|
| `pyrosense_analysis_events_processed_total` | Counter | - | Telemetry events analyzed |
| `pyrosense_anomalies_detected_total` | Counter | type | Anomalies detected by type |
| `pyrosense_analysis_processing_duration_seconds` | Histogram | - | Analysis processing time |

### Risk Scoring Service

| Metric | Type | Labels | Description |
|--------|------|--------|-------------|
| `pyrosense_risk_score_updated_total` | Counter | severity | Risk scores calculated |
| `pyrosense_risk_score_value` | Histogram | - | Risk score value distribution |
| `pyrosense_risk_score_current` | Gauge | device_id | Current risk score per device |

### Alerting Service

| Metric | Type | Labels | Description |
|--------|------|--------|-------------|
| `pyrosense_alerts_created_total` | Counter | severity | Alerts created by severity |
| `pyrosense_alerts_critical_active` | Gauge | - | Currently active critical alerts |
| `pyrosense_alerts_by_status` | Gauge | status | Alert count by status |

### Notification Service

| Metric | Type | Labels | Description |
|--------|------|--------|-------------|
| `pyrosense_notifications_sent_total` | Counter | channel | Notifications sent by channel |
| `pyrosense_notifications_failed_total` | Counter | channel | Notifications failed by channel |
| `pyrosense_notifications_retrying` | Gauge | - | Notifications in retry state |

### Device Service

| Metric | Type | Labels | Description |
|--------|------|--------|-------------|
| `pyrosense_devices_active_total` | Gauge | - | Currently active devices |
| `pyrosense_devices_offline_total` | Gauge | - | Currently offline devices |

## Distributed Tracing

### Configuration

All services use Micrometer Tracing with OpenTelemetry bridge:
- Protocol: OTLP HTTP (port 4318)
- Sampling: Configurable via `TRACING_SAMPLING` env var (default: 1.0 = 100%)
- Propagation: W3C Trace Context (default)

### Trace Context Propagation

Traces propagate through:
- HTTP requests (W3C `traceparent` header)
- Kafka messages (header-based propagation)
- Gateway adds `X-Correlation-Id` aligned with trace ID

### Key Spans

| Service | Span Name | Description |
|---------|-----------|-------------|
| Gateway | `gateway.request` | Full request lifecycle |
| Ingestion | `telemetry.ingest` | Telemetry processing pipeline |
| Analysis | `signal.analyze` | Signal analysis execution |
| Scoring | `risk.calculate` | Risk score calculation |
| Alerting | `alert.create` | Alert creation |
| Notification | `notification.dispatch` | Notification dispatch |

## Structured Logging

### Format

- **Local/Test**: Human-readable with traceId/spanId
  ```
  10:30:45.123 [main] INFO  c.p.ingestion.Service [traceId=abc123 spanId=def456] - message
  ```

- **Docker/Prod**: JSON (logstash-logback-encoder)
  ```json
  {"@timestamp":"2025-01-15T10:30:45.123Z","level":"INFO","logger":"c.p.ingestion.Service","thread":"main","message":"Telemetry received","service":"pyrosense-ingestion-service","traceId":"abc123","spanId":"def456"}
  ```

### MDC Fields

| Field | Source | Description |
|-------|--------|-------------|
| `traceId` | OpenTelemetry | Distributed trace identifier |
| `spanId` | OpenTelemetry | Current span identifier |
| `correlationId` | X-Correlation-Id header | Gateway correlation ID |
| `tenantId` | TenantContext | Current tenant |

### Security Rules

- **Never log**: passwords, tokens, API keys, PII (phone numbers, emails)
- **Mask in logs**: Device credentials show only last 4 chars
- **No secrets**: Environment variables containing secrets are never logged
- **No payload content**: Notification bodies never appear in logs

## Prometheus Alerts

### Service Health

| Alert | Condition | Severity | Duration |
|-------|-----------|----------|----------|
| ServiceDown | `up == 0` | critical | 1m |
| HighMemoryUsage | Heap > 85% | warning | 5m |
| HighErrorRate | 5xx > 5% | warning | 5m |
| HighResponseLatency | P95 > 2s | warning | 5m |

### Ingestion

| Alert | Condition | Severity | Duration |
|-------|-----------|----------|----------|
| IngestionDown | Received rate = 0 | critical | 10m |
| HighRejectionRate | Rejection > 10% | warning | 5m |

### Kafka

| Alert | Condition | Severity | Duration |
|-------|-----------|----------|----------|
| KafkaConsumerLagHigh | Lag > 1000 | warning | 5m |
| KafkaConsumerLagCritical | Lag > 10000 | critical | 2m |

### Devices

| Alert | Condition | Severity | Duration |
|-------|-----------|----------|----------|
| DevicesOfflineHigh | Offline > 10 | warning | 5m |
| DeviceOfflineCritical | Offline > 50 | critical | 2m |

### Risk & Alerting

| Alert | Condition | Severity | Duration |
|-------|-----------|----------|----------|
| HighRiskScore | Score > 80 | critical | 2m |
| CriticalAlertsActive | Active critical > 10 | critical | 1m |

### Notifications

| Alert | Condition | Severity | Duration |
|-------|-----------|----------|----------|
| NotificationFailureRateHigh | Failure > 20% | warning | 5m |
| NotificationsBacklog | Retrying > 50 | warning | 5m |

## Grafana Dashboards

### Platform Overview (`pyrosense-overview`)
- Service health status (up/down count)
- Telemetry ingestion rate
- Anomalies detected
- Risk score distribution
- Alerts created by severity
- Notifications sent/failed
- Device status
- HTTP request/error rates
- JVM heap usage
- Kafka consumer lag

### Ingestion Pipeline (`pyrosense-ingestion`)
- Telemetry throughput (received/rejected)
- Ingestion latency percentiles (P50/P95/P99)
- Rejection reasons breakdown
- Active devices count
- Kafka producer metrics
- Signal analysis processing rate
- Analysis duration

### Alerting & Notifications (`pyrosense-alerting`)
- Alerts created by severity
- Active critical alerts count
- Notifications by channel
- Notification failures
- Alert lifecycle by status

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `TRACING_SAMPLING` | Trace sampling probability (0.0-1.0) | 1.0 |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OTLP endpoint for traces | http://localhost:4318/v1/traces |
| `GRAFANA_PASSWORD` | Grafana admin password | admin |

### Production Recommendations

| Setting | Value | Reason |
|---------|-------|--------|
| Sampling | 0.1 (10%) | Reduce trace volume in production |
| Retention (metrics) | 15 days | Prometheus default |
| Retention (logs) | 30 days | Loki config |
| Scrape interval | 15s | Balance granularity vs load |

## Architecture

```
Services (10) ──────── /actuator/prometheus ────── Prometheus ──── Grafana
     │                                                  │
     ├──── OTLP (traces) ──── OpenTelemetry Collector ──┤
     │                                                  │
     └──── JSON logs (stdout) ──── Docker log driver ── Loki ──── Grafana
```

## Health Check Integration

All services expose:
- `GET /actuator/health` — liveness probe (always accessible)
- `GET /actuator/health/readiness` — readiness (checks DB, Kafka, Redis)

Docker Compose and Kubernetes can use these for health management.
