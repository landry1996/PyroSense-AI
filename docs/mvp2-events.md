# MVP 2 - Event-Driven Integration (Kafka)

## Overview

PyroSense services communicate asynchronously via Apache Kafka events. Each service publishes domain events to its own topic and subscribes to topics it needs to react to. This avoids direct REST coupling between services for reactions.

## Topics

| Topic | Publisher | Consumers | Partitions |
|-------|-----------|-----------|:----------:|
| `pyrosense.alerts.events` | Alerting | Maintenance, Notification, Dashboard | 6 |
| `pyrosense.maintenance.events` | Maintenance | Notification, Dashboard | 6 |
| `pyrosense.reports.events` | Reporting | Notification | 6 |
| `pyrosense.notifications.events` | Notification | Audit | 6 |
| `pyrosense.audit.events` | Audit | — | 6 |
| `pyrosense.dead-letter.events` | All (DLQ) | Monitoring/Ops | 3 |

Legacy topics (MVP 1 pipeline):
- `pyrosense.scoring.events` — Risk Scoring → Alerting, Notification
- `pyrosense.analysis.events` — Signal Analysis → Scoring, Alerting
- `pyrosense.device.events` — Device → Notification

## Event Envelope (IntegrationEvent)

All events are wrapped in the shared `IntegrationEvent` record:

```java
public record IntegrationEvent(
    UUID eventId,          // unique per event instance
    String eventType,      // dot-notation: "alerting.alert.created"
    int version,           // schema version (starts at 1)
    Instant occurredAt,    // when the domain event happened
    String sourceService,  // "pyrosense-alerting-service"
    String tenantId,       // multi-tenancy isolation
    String correlationId,  // trace across service chain
    String causationId,    // direct parent event ID
    String payload         // JSON-serialized domain event
) {}
```

### Field Descriptions

| Field | Purpose |
|-------|---------|
| `eventId` | Idempotency key for consumers |
| `eventType` | Routing/filtering by consumers |
| `version` | Schema evolution without breaking consumers |
| `occurredAt` | Business timestamp (not publish time) |
| `sourceService` | Origin service for debugging |
| `tenantId` | Tenant-scoped processing in consumers |
| `correlationId` | End-to-end trace (e.g., original alert → intervention → notification) |
| `causationId` | Direct parent event that caused this one |
| `payload` | JSON string, loose coupling (consumer reads what it needs) |

### Backward Compatibility

The `@JsonIgnoreProperties(ignoreUnknown = true)` annotation allows consumers to deserialize events even when new fields are added. The deprecated 5-arg constructor maintains compatibility with existing publishers during migration.

## Event Catalog

### Alert Events (pyrosense.alerts.events)

| Event Type | Version | Payload Fields |
|-----------|:-------:|----------------|
| `alerting.alert.created` | 1 | alertId, tenantId, deviceId, severity, type |
| `alerting.alert.acknowledged` | 1 | alertId, acknowledgedBy |
| `alerting.alert.resolved` | 1 | alertId, resolvedBy, falsePositive |
| `alerting.alert.escalated` | 1 | alertId, previousLevel, newLevel, severity |

### Maintenance Events (pyrosense.maintenance.events)

| Event Type | Version | Payload Fields |
|-----------|:-------:|----------------|
| `maintenance.intervention.created` | 1 | interventionId, tenantId, sourceAlertId, deviceId, type, priority |
| `maintenance.intervention.assigned` | 1 | interventionId, tenantId, electricianId, scheduledAt |
| `maintenance.intervention.completed` | 1 | interventionId, tenantId, deviceId, result, riskScoreBefore, riskScoreAfter |

### Report Events (pyrosense.reports.events)

| Event Type | Version | Payload Fields |
|-----------|:-------:|----------------|
| `reporting.report.generated` | 1 | reportId, reportNumber, tenantId, buildingId, type |

### Notification Events (pyrosense.notifications.events)

| Event Type | Version | Payload Fields |
|-----------|:-------:|----------------|
| `notification.notification.sent` | 1 | notificationId, tenantId, recipientId, channel, subject |
| `notification.notification.failed` | 1 | notificationId, tenantId, recipientId, channel, failureReason, retryCount |

## Event Flow Diagram

```
Alert Created
    ├── → Maintenance: auto-create intervention (CRITICAL)
    ├── → Maintenance: create recommendation (WARNING)
    ├── → Notification: send alert notification
    └── → Dashboard: invalidate cache

Intervention Assigned
    └── → Notification: notify electrician

Intervention Completed
    ├── → Notification: notify stakeholders
    └── → Dashboard: invalidate cache

Report Generated
    └── → Notification: notify report requester

Alert Escalated
    └── → Notification: escalation notification
```

## Producer Configuration

All producers use:
- `acks=all` — wait for all replicas to acknowledge
- `enable.idempotence=true` — exactly-once semantics within a partition
- `retries=3` — automatic retry on transient failures
- Message key = `eventId` (UUID) — ensures ordering per event chain
- Observation enabled for distributed tracing

### Publishing Pattern

```java
IntegrationEvent integrationEvent = IntegrationEvent.builder()
    .eventId(domainEvent.eventId())
    .eventType(domainEvent.eventType())
    .version(1)
    .occurredAt(domainEvent.occurredAt())
    .sourceService(SOURCE_SERVICE)
    .tenantId(tenantId)
    .correlationId(correlationId)
    .causationId(domainEvent.eventId().toString())
    .payload(objectMapper.writeValueAsString(domainEvent))
    .build();

kafkaTemplate.send(topic, eventId.toString(), integrationEvent);
```

## Consumer Configuration

### Retry with Exponential Backoff

All consumers use `DefaultErrorHandler` with `ExponentialBackOff`:
- Initial interval: 1000ms
- Multiplier: 2x
- Max attempts: 3 (configurable per service)

After exhausting retries, messages are sent to the Dead Letter Queue (DLQ).

### Dead Letter Queue (DLQ)

Failed messages are published to `pyrosense.dead-letter.events` via `DeadLetterPublishingRecoverer`. DLQ messages retain:
- Original topic/partition/offset
- Exception details (headers)
- Original event payload

DLQ retention: 7 days (`retention.ms=604800000`).

### Idempotent Consumers

Consumers protect against double processing:

1. **Alerting → Maintenance**: `recommendationRepository.findByAlertId()` prevents duplicate interventions
2. **Notification service**: `DeduplicationPort` with configurable window (30 min default)
3. **General pattern**: `IdempotentEventConsumer` interface in shared-kernel

```java
public interface IdempotentEventConsumer {
    boolean isAlreadyProcessed(UUID eventId);
    void markAsProcessed(UUID eventId);
}
```

## Consumer Group IDs

| Service | Group ID | Purpose |
|---------|----------|---------|
| Alerting | `alerting-group` | Consumes scoring + analysis events |
| Maintenance | `maintenance-group` | Consumes alert events |
| Notification | `notification-group` | Consumes all event topics |
| Dashboard | `dashboard-group` | Cache invalidation |

## Observability

### Metrics (Micrometer)

- `kafka.producer.record.send.total` — events published per topic
- `kafka.consumer.records.consumed.total` — events consumed per topic
- `kafka.consumer.records.lag` — consumer lag per partition
- Custom: `pyrosense.events.published` (tagged by eventType, service)
- Custom: `pyrosense.events.consumed` (tagged by eventType, service)
- Custom: `pyrosense.events.failed` (tagged by eventType, service)

### Tracing (OpenTelemetry)

- `observationEnabled=true` on all KafkaTemplate and listener containers
- `correlationId` propagated across service boundaries
- Spans created for publish/consume operations

### Structured Logging

```
Published event=alerting.alert.created eventId=abc-123 to topic=pyrosense.alerts.events partition=2 offset=1547
```

## Configuration Reference

### application.yml (per service)

```yaml
pyrosense:
  <service>:
    kafka:
      output-topic: ${KAFKA_TOPIC_XXX:pyrosense.xxx.events}
      dlq-topic: ${KAFKA_TOPIC_DLQ:pyrosense.dead-letter.events}
      retry:
        max-attempts: 3
        backoff-ms: 1000
        backoff-multiplier: 2
```

### Docker Compose

```yaml
kafka:
  image: apache/kafka:3.8.0
  environment:
    KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    KAFKA_NUM_PARTITIONS: 6
    KAFKA_DEFAULT_REPLICATION_FACTOR: 1
    KAFKA_LOG_RETENTION_HOURS: 24
```

## Schema Evolution Strategy

1. Add fields freely (consumers ignore unknown via `@JsonIgnoreProperties`)
2. Never remove fields without version bump
3. Increment `version` for breaking changes
4. Consumers route by `eventType`, optionally check `version`
5. Old consumers continue working with v1 payloads

## Constraints

- No synchronous REST calls between services for event reactions
- Events are eventual-consistency (not real-time guarantees)
- REST reserved for user-facing queries and commands
- Each service owns its topic (single writer principle)
- Consumers must be idempotent (at-least-once delivery)
- No shared database between services
