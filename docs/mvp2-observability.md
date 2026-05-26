# MVP 2 - Observabilite Complete

## Vue d'Ensemble

L'observabilite MVP 2 couvre 4 piliers : metriques metier (Micrometer/Prometheus), tracing distribue (OpenTelemetry), logs structures (Logstash JSON), et alertes proactives (Prometheus Alertmanager).

Tous les services MVP 2 (dashboard, maintenance, reporting, notification) partagent la meme configuration de base et exposent des metriques metier specifiques.

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│  Services MVP 2                                               │
│  ┌─────────┐ ┌────────────┐ ┌──────────┐ ┌──────────────┐   │
│  │Dashboard│ │Maintenance │ │Reporting │ │Notification  │   │
│  │ :8088   │ │   :8089    │ │  :8091   │ │    :8087     │   │
│  └────┬────┘ └─────┬──────┘ └────┬─────┘ └──────┬───────┘   │
│       │             │             │               │           │
│       ├─── /actuator/prometheus ──┼───────────────┤           │
│       ├─── OTLP traces ──────────┼───────────────┤           │
│       └─── JSON logs (stdout) ───┼───────────────┘           │
└───────┬─────────────┬────────────┬───────────────────────────┘
        │             │            │
   ┌────▼────┐   ┌───▼────┐  ┌───▼──────────┐
   │Prometheus│   │  OTEL  │  │   Loki       │
   │  :9090   │   │Collector│  │  :3100       │
   └────┬────┘   │:4317/18│  └───┬──────────┘
        │         └───┬────┘      │
   ┌────▼────────────▼───────────▼─┐
   │         Grafana :3000          │
   │  Dashboards + Alerting         │
   └────────────────────────────────┘
```

## Metriques Metier

### Dashboard Service

| Metrique | Type | Description |
|----------|------|-------------|
| `pyrosense.dashboard.overview.requests` | Counter | Requetes overview total |
| `pyrosense.dashboard.overview.latency` | Timer | Latence requetes overview (P50, P95, P99) |
| `pyrosense.dashboard.cache.hits` | Counter | Cache Redis hits |
| `pyrosense.dashboard.cache.misses` | Counter | Cache Redis misses |
| `pyrosense.dashboard.events.processed` | Counter | Events Kafka pour invalidation cache |
| `pyrosense.dashboard.access.denied` | Counter | Acces refuses (403) |
| `pyrosense.dashboard.suspicious.tenant.access` | Counter | Tentatives cross-tenant |

### Maintenance Service

| Metrique | Type | Description |
|----------|------|-------------|
| `pyrosense.maintenance.interventions.created` | Counter | Interventions creees |
| `pyrosense.maintenance.interventions.completed` | Counter | Interventions terminees |
| `pyrosense.maintenance.interventions.overdue` | Gauge | Interventions en retard (SLA depasse) |
| `pyrosense.maintenance.events.processed` | Counter | Events Kafka traites |
| `pyrosense.maintenance.dlq.events` | Counter | Events envoyes en DLQ |
| `pyrosense.maintenance.access.denied` | Counter | Acces refuses |
| `pyrosense.maintenance.suspicious.tenant.access` | Counter | Tentatives cross-tenant |

### Reporting Service

| Metrique | Type | Description |
|----------|------|-------------|
| `pyrosense.reporting.reports.requested` | Counter | Rapports demandes |
| `pyrosense.reporting.reports.generated` | Counter | Rapports generes avec succes |
| `pyrosense.reporting.reports.failed` | Counter | Echecs generation |
| `pyrosense.reporting.generation.duration` | Timer | Duree generation PDF (P50, P95, P99) |
| `pyrosense.reporting.downloads` | Counter | Telechargements PDF |
| `pyrosense.reporting.downloads.denied` | Counter | Telechargements refuses (token expire/invalide) |
| `pyrosense.reporting.events.processed` | Counter | Events Kafka traites |
| `pyrosense.reporting.dlq.events` | Counter | Events envoyes en DLQ |
| `pyrosense.reporting.access.denied` | Counter | Acces refuses |

### Notification Service

| Metrique | Type | Description |
|----------|------|-------------|
| `pyrosense.notification.sent` | Counter | Notifications envoyees avec succes |
| `pyrosense.notification.failed` | Counter | Echecs livraison |
| `pyrosense.notification.suppressed` | Counter | Notifications supprimees (dedup, quiet hours, consent) |
| `pyrosense.notification.delivery.duration` | Timer | Duree livraison (P50, P95, P99) |
| `pyrosense.notification.events.processed` | Counter | Events Kafka traites |
| `pyrosense.notification.dlq.events` | Counter | Events envoyes en DLQ |
| `pyrosense.notification.access.denied` | Counter | Acces refuses |
| `pyrosense.notification.suspicious.tenant.access` | Counter | Tentatives cross-tenant |

### Metriques Kafka (automatiques Spring Boot)

| Metrique | Description |
|----------|-------------|
| `kafka_consumer_fetch_manager_records_lag` | Lag consommateur par partition |
| `kafka_producer_record_send_total` | Messages publies |
| `kafka_consumer_fetch_manager_records_consumed_total` | Messages consommes |

### Metriques Securite (transversales)

| Metrique | Services | Description |
|----------|----------|-------------|
| `*.access.denied` | Tous | Tentatives refusees (403) |
| `*.suspicious.tenant.access` | Dashboard, Maintenance, Notification | Acces cross-tenant detectes |
| `pyrosense.reporting.downloads.denied` | Reporting | Telechargements token invalide |

## Actuator

Chaque service expose :

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: when-authorized
```

| Endpoint | Acces | Usage |
|----------|-------|-------|
| `/actuator/health` | Public | Docker healthchecks, load balancer |
| `/actuator/info` | Public | Version, build info |
| `/actuator/prometheus` | Interne | Scrape Prometheus |
| `/actuator/metrics` | Protege | Debug metriques individuelles |

## Tracing OpenTelemetry

### Configuration

```yaml
management:
  tracing:
    sampling:
      probability: ${TRACING_SAMPLING:1.0}
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true
```

### Propagation de Contexte

1. **X-Correlation-Id** : genere par le gateway si absent, propage a tous les services
2. **W3C traceparent** : propage automatiquement par Micrometer Tracing bridge
3. **Kafka headers** : trace context injecte dans les headers Kafka (`traceparent`)
4. **MDC** : `traceId`, `spanId`, `correlationId`, `tenantId` dans tous les logs

### Spans crees automatiquement

- HTTP server requests (Spring MVC)
- HTTP client requests (RestTemplate/WebClient)
- Kafka producer send
- Kafka consumer receive
- JDBC queries (si DataSource wrapping actif)
- Redis operations

## Logs Structures

### Format JSON (Docker/Prod)

```json
{
  "@timestamp": "2026-05-27T14:30:00.123Z",
  "level": "INFO",
  "logger": "c.p.maintenance.adapter.out.KafkaM",
  "thread": "kafka-consumer-1",
  "message": "Published event=maintenance.intervention.created eventId=abc-123 to topic=pyrosense.maintenance.events",
  "traceId": "a1b2c3d4e5f6",
  "spanId": "1234567890ab",
  "correlationId": "req-456",
  "tenantId": "tenant-789",
  "service": "pyrosense-maintenance-service"
}
```

### Format Console (Local/Test)

```
14:30:00.123 [kafka-consumer-1] INFO  c.p.m.adapter.out.KafkaMaintenanceEP [traceId=a1b2c3d4 spanId=1234abcd] - Published event=maintenance.intervention.created
```

### Regles PII

- **JAMAIS** de donnees personnelles (email, telephone, nom) dans les logs
- **JAMAIS** de payload complet de notification (contient potentiellement PII)
- **JAMAIS** de secrets (tokens, mots de passe, cles)
- Masquage automatique via `JdbcAuditLogRepository.sanitizeForStorage()` (regex: phone, email, password, token)
- `failureReason` masque si contient Exception ou >200 caracteres

### Logback Configuration

Chaque service MVP 2 a `logback-spring.xml` avec :
- Profil `local,test` : format texte lisible avec traceId/spanId
- Profil `docker,prod` : JSON structure (LoggingEventCompositeJsonEncoder)
- MDC automatique : traceId, spanId, correlationId, tenantId
- Tag service name dans chaque ligne JSON

## Alertes Prometheus

### Groupe `pyrosense-mvp2`

| Alerte | Expression | Seuil | Severite |
|--------|-----------|-------|----------|
| NotificationFailuresHigh | `rate(pyrosense_notification_failed_total[5m])` | > 0.5/s pendant 3m | WARNING |
| ReportGenerationFailures | `increase(pyrosense_reporting_reports_failed_total[15m])` | > 3 en 15m | WARNING |
| KafkaDlqNonEmpty | `sum(increase(*_dlq_events_total[5m]))` | > 0 pendant 1m | WARNING |
| InterventionOverdueHigh | `pyrosense_maintenance_interventions_overdue` | > 10 pendant 5m | WARNING |
| ApiErrorRateHigh | `5xx / total requests` | > 5% pendant 3m | WARNING |
| ReportDownloadDeniedSpike | `increase(downloads_denied[5m])` | > 10 en 5m | WARNING |
| SuspiciousTenantAccessDetected | `sum(suspicious_tenant_access)` | > 5 en 5m | CRITICAL |

### Alertes existantes applicables

| Alerte | Couvre MVP 2 | Severite |
|--------|-------------|----------|
| ServiceDown | Oui (tous les jobs) | CRITICAL |
| HighMemoryUsage | Oui (JVM heap > 85%) | WARNING |
| HighErrorRate | Oui (5xx > 5%) | WARNING |
| HighResponseLatency | Oui (P95 > 2s) | WARNING |
| KafkaConsumerLagHigh | Oui (lag > 1000) | WARNING |
| KafkaConsumerLagCritical | Oui (lag > 10000) | CRITICAL |

## Grafana Dashboard

Le dashboard **PyroSense MVP 2 Services** (`mvp2-services.json`) affiche :

| Panel | Type | Metriques |
|-------|------|-----------|
| MVP 2 Service Health | Stat | `count(up{job=~"..."} == 1)` |
| HTTP Request Rate | Timeseries | `rate(http_server_requests_seconds_count)` by job |
| HTTP Response Latency P95 | Timeseries | `histogram_quantile(0.95, ...)` by job |
| Kafka Events Published | Timeseries | `rate(kafka_producer_record_send_total)` by job |
| Kafka Consumer Lag | Timeseries | `kafka_consumer_records_lag` by job |
| Alerts Created by Severity | Timeseries | `rate(pyrosense_alerts_created_total)` by severity |
| Interventions by Status | Stat | `pyrosense_maintenance_interventions_total` by status |
| Notifications Sent/Failed | Timeseries | `rate(sent_total)` + `rate(failed_total)` |
| JVM Heap Usage | Timeseries | `used / max * 100` by job |
| Reports Generated (24h) | Stat | `increase(reports_generated_total[24h])` |
| Dashboard Cache Hit Rate | Gauge | `hits / (hits + misses) * 100` |

Acces : http://localhost:3000 → PyroSense AI Platform → PyroSense MVP 2 Services

## Configuration de Reference

### application.yml (bloc management complet)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: when-authorized
  tracing:
    sampling:
      probability: ${TRACING_SAMPLING:1.0}
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true
```

### Prometheus scrape (prometheus.yml)

```yaml
- job_name: 'pyrosense-dashboard'
  metrics_path: /actuator/prometheus
  static_configs:
    - targets: ['dashboard-service:8088', 'host.docker.internal:8088']
```

### Variables d'environnement

| Variable | Defaut | Description |
|----------|--------|-------------|
| TRACING_SAMPLING | 1.0 | Taux echantillonnage (1.0 = 100%, 0.1 = 10% prod) |
| OTEL_EXPORTER_OTLP_ENDPOINT | http://localhost:4318/v1/traces | Collecteur OTLP |

## Contraintes

- Pas de donnees personnelles dans les logs (email, telephone, nom)
- Pas de secret dans les logs ou metriques (tokens, passwords, API keys)
- Pas de payload complet de notification dans les logs (contient PII potentiel)
- Metriques ne contiennent JAMAIS de valeurs sensibles (uniquement compteurs, timers, gauges)
- Tags metriques : service, status, channel, severity — jamais tenantId ou userId dans les labels Prometheus
- Logs : tenantId en MDC pour correlation, mais pas de donnees du tenant
- En production, reduire `TRACING_SAMPLING` a 0.1 (10%) pour limiter le volume

## Integration avec l'Infrastructure Existante

Le stack d'observabilite existant couvre deja :
- **Prometheus** : scrape tous les services, retention 15j
- **Grafana** : 4 dashboards provisionnes (platform-overview, ingestion, alerting, mvp2)
- **Loki** : aggrege les logs JSON stdout de tous les conteneurs Docker
- **OpenTelemetry Collector** : recoit traces OTLP, exporte vers Prometheus
- **Alerting** : 14 + 7 regles d'alerte (service-health, ingestion, kafka, devices, risk, notifications, mvp2)
