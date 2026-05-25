# PyroSense AI Platform - Domain Model

> Predictive maintenance for electrical panels. This document describes the domain model
> following Domain-Driven Design tactical patterns. PyroSense monitors IoT sensors, ingests
> telemetry, computes risk scores, generates alerts, and handles multi-tenant architecture.
>
> **Architecture**: Hexagonal (Ports & Adapters), Java 21 / Spring Boot 3.4.1, 11 Maven modules.
>
> **Maturity**: MVP phase -- statistical detection only. ML/prediction aspects are simulated
> behind port interfaces (NoOp adapters). Real hardware is simulated via an IoT simulator tool.

---

## 1. Bounded Contexts

The platform is decomposed into 9 bounded contexts, each deployed as an independent
microservice with its own database (Database-per-Service pattern). Communication between
contexts is asynchronous via Apache Kafka domain events.

| # | Bounded Context | Service | Port | Responsibility |
|---|-----------------|---------|------|----------------|
| 1 | Identity & Access | pyrosense-identity-service | 8081 | Tenant lifecycle, user management, role-based access control (RBAC), membership, authentication delegation to Keycloak |
| 2 | Device Management | pyrosense-device-service | 8082 | Device registration, provisioning to tenants/buildings/panels, firmware tracking, lifecycle state machine (REGISTERED -> ACTIVE -> OFFLINE -> REVOKED) |
| 3 | Telemetry Ingestion | pyrosense-ingestion-service | 8083 | MQTT reception of raw sensor measurements, schema validation, idempotency check (Redis, TTL 24h), persistence to TimescaleDB, event publication |
| 4 | Signal Analysis | pyrosense-signal-analysis-service | 8084 | Time-series baseline learning (Welford's online algorithm), z-score deviation detection, micro-arc pattern detection, temperature trend analysis, THD drift detection |
| 5 | Risk Scoring | pyrosense-risk-scoring-service | 8085 | Composite risk score computation (0-100) from weighted anomaly factors, trend detection (IMPROVING/STABLE/DEGRADING/CRITICAL), incident prediction window |
| 6 | Alerting | pyrosense-alerting-service | 8086 | Alert creation from risk threshold exceedance, deduplication (device + type), SLA tracking, escalation levels (NONE -> FIRST -> SECOND -> EMERGENCY), full lifecycle state machine |
| 7 | Notification | pyrosense-notification-service | 8087 | Multi-channel delivery (email, SMS, push, webhook, dashboard), severity-based routing, retry with exponential backoff, GDPR consent enforcement, anti-spam deduplication |
| 8 | Maintenance | pyrosense-maintenance-service | 8089 | Intervention lifecycle from alert to field resolution, technician assignment, field diagnostics, false-positive feedback loop to Signal Analysis |
| 9 | Reporting | pyrosense-reporting-service | 8088 | Report generation (PDF), compliance certificates, insurer exports, download token management |

Additionally, a **Shared Kernel** (`pyrosense-shared-kernel`) provides cross-cutting primitives:
strongly-typed IDs, base classes (AggregateRoot, DomainEvent, ValueObject), common enums, and
audit metadata. It contains zero business logic.

An **API Gateway** (`pyrosense-api-gateway`, port 8080) handles JWT validation, rate limiting,
CORS, and routing. It is not a bounded context -- it is infrastructure.

---

## 2. Aggregates & Entities

### 2.1 Identity & Access

| Element | Role | Key Fields |
|---------|------|------------|
| **User** (aggregate root) | Human platform user | userId (UserId), email, fullName, status (ACTIVE/INACTIVE/SUSPENDED/LOCKED), failedLoginAttempts, lockedUntil, lastLoginAt |
| Membership (entity) | Ties a user to a tenant with roles | membershipId (UUID), userId, tenantId, roles (Set of Role), active flag |
| **Tenant** (aggregate root) | Organization subscribing to PyroSense | tenantId (TenantId), name, slug, active, createdAt |
| ServiceAccount (entity) | Machine-to-machine identity for devices | accountId, tenantId, deviceId, keyHash, status |
| DeviceCredential (entity) | Authentication material for a device | credentialId, deviceId, keyHash, issuedAt, revokedAt |

**Roles** (enum): PLATFORM_ADMIN, TENANT_ADMIN, PROPERTY_MANAGER, OCCUPANT, ELECTRICIAN, INSURANCE_PARTNER, DEVICE, SUPPORT_READONLY

Each role maps to a fixed set of **Permissions** (enum with 20+ values such as DEVICE_REGISTER, ALERT_ACKNOWLEDGE, RISK_EXPORT, TELEMETRY_INGEST, etc.).

### 2.2 Device Management

| Element | Role | Key Fields |
|---------|------|------------|
| **Device** (aggregate root) | IoT sensor installed on an electrical panel | deviceId (DeviceId), serialNumber, tenantId, buildingId, panelId, firmwareVersion, hardwareRevision, connectivityType, status (DeviceStatus), lastSeenAt, installationDate, enrollmentKeyHash |

**DeviceStatus** states: REGISTERED, PROVISIONED, ACTIVE, OFFLINE, MAINTENANCE, REVOKED

State machine:
```
REGISTERED --(provision)--> PROVISIONED --(activate)--> ACTIVE
ACTIVE --(missedHeartbeat)--> OFFLINE --(heartbeat)--> ACTIVE
ACTIVE / OFFLINE --(revoke)--> REVOKED (terminal)
ACTIVE --(markMaintenance)--> MAINTENANCE
```

**ConnectivityType** enum: WIFI, ETHERNET, LORAWAN, CELLULAR_4G, CELLULAR_5G

### 2.3 Telemetry Ingestion

| Element | Role | Key Fields |
|---------|------|------------|
| **TelemetryReading** (aggregate root, immutable) | Single sensor measurement snapshot | id (UUID), deviceId, tenantId, buildingId, electricalPanelId, circuitId, timestamp, ingestedAt, payloadHash |
| DeviceHeartbeat (entity) | Liveness signal from a device | deviceId, receivedAt, firmwareVersion |

**Measurement fields on TelemetryReading** (all doubles/ints):

| Field | Unit | Description |
|-------|------|-------------|
| rmsCurrent | Amperes | RMS current on circuit |
| rmsVoltage | Volts | RMS voltage |
| activePower | Watts | Active power |
| reactivePower | VAR | Reactive power |
| powerFactor | ratio 0-1 | cos(phi) |
| thd | % | Total Harmonic Distortion |
| temperatureCelsius | C | Conductor temperature |
| hfNoiseLevel | dB | High-frequency noise level |
| microArcCount | count | Micro-arc events in sampling window |
| transientCount | count | Transient events in sampling window |
| samplingWindowMs | ms | Duration of measurement window |

### 2.4 Signal Analysis

| Element | Role | Key Fields |
|---------|------|------------|
| **SignalBaseline** (aggregate root) | Per-device statistical baseline built via Welford's algorithm | deviceId, featureStats (JSONB: mean, stdDev, min, max, p5/p25/p50/p75/p95, count per feature), status (BUILDING/READY), lastUpdatedAt |
| AnalysisResult (entity, immutable) | Result of analyzing one telemetry reading against baseline | id, deviceId, analyzedAt, anomalies (list), aggregateRiskScore, baselineAvailable |
| SignalAnomaly (value object) | Single detected anomaly | type (AnomalyType), confidence [0,1], zScore, weightedScore, metric, observedValue, baselineValue |

**AnomalyType** enum: THD_ABNORMAL, MICRO_ARC_RECURRENT, TEMPERATURE_RISING, HF_NOISE_ELEVATED, TRANSIENT_ABNORMAL, POWER_FACTOR_DEGRADED, BASELINE_DRIFT

**Detection methods** (implemented in MVP):
- ZScoreDetector: deviation > 3 sigma from baseline
- MicroArcPatternDetector: recurrence count in window > threshold
- TemperatureTrendDetector: absolute max + rise rate per hour
- ThdDriftDetector: THD exceeding baseline + absolute max
- ExponentialSmoothingDetector: smoothed value drift detection
- BaselineBuilder: Welford's online algorithm (numerically stable)
- MachineLearningInferencePort: **NoOp adapter in MVP** (returns empty; prepared for future model)

### 2.5 Risk Scoring

| Element | Role | Key Fields |
|---------|------|------------|
| **RiskAssessment** (aggregate root) | Composite risk evaluation for a device | assessmentId (RiskAssessmentId), deviceId, tenantId, assessedAt, score (RiskScore 0-100), level (RiskLevel), trend (RiskTrend), factors (list of RiskFactor), predictedIncidentDays, recommendation |
| RiskFactor (value object) | Single contributing factor to the score | name, description, weight, normalizedValue, contribution (weight x value x 100) |
| RiskScore (value object) | The score itself, clamped [0, 100] | value (int) |
| ScoringWeights (value object) | Configurable weight configuration | per-factor weights, decay factor, repetition boost |
| AnomalyInput (value object) | Input from Signal Analysis for scoring | type, confidence, zScore, detectedAt, repetitionCount |

**Factor Weights** (configurable, default values):

| Factor | Weight | Signal Source |
|--------|--------|---------------|
| Temperature | 0.35 | TEMPERATURE_RISING anomalies |
| Current | 0.25 | Overcurrent / power factor anomalies |
| Vibration | 0.20 | Transient events (proxy for mechanical vibration in MVP) |
| Humidity | 0.10 | Insulation degradation indicators |
| Arc fault | 0.10 | MICRO_ARC_RECURRENT anomalies |

> **Note**: The actual implementation uses a slightly different weight set optimized for
> electrical fire prevention (micro-arc: 0.30, THD drift: 0.20, temperature: 0.20,
> transients: 0.10, HF noise: 0.10, sensor reliability: 0.10). The weights above represent
> the simplified pedagogical model. Both are configurable via `application.yml`.

**RiskLevel** enum: LOW (0-29), MODERATE (30-59), HIGH (60-79), CRITICAL (80-100)

**RiskTrend** enum: IMPROVING, STABLE, DEGRADING, CRITICAL

**Modifiers**:
- Recency decay: `0.95^(hours_ago / 24)` -- recent anomalies weigh more
- Repetition boost: `min(1 + (count-1) * 0.1, 1.3)` -- repeated patterns amplify
- Device offline penalty: +50% on reliability factor
- No baseline penalty: +30% on reliability factor

**ML Extension Point** (implemented as port): `RiskModelPort` interface with `NoOpRiskModelAdapter` in MVP. The ML port is advisory only -- it logs predictions for comparison but does not override rule-based scoring.

### 2.6 Alerting

| Element | Role | Key Fields |
|---------|------|------------|
| **Alert** (aggregate root) | Risk event requiring human attention | alertId (AlertId), tenantId, deviceId, type (AlertType), severity (AlertSeverity), title, description, status (AlertStatus), deduplicationKey, slaDeadline, escalationLevel, occurrenceCount, lastOccurrenceAt, assignedTo, acknowledgedAt/By, resolvedAt/By, resolutionNote |
| AlertComment (entity) | Audit trail note on alert | author (UserId), content, createdAt |
| DeduplicationKey (value object) | Composite key: deviceId + alertType | deviceId, alertType |
| SlaPolicy (value object) | SLA deadlines per severity | criticalDeadline (24h), warningDeadline (7d), infoDeadline (30d), escalationInterval (4h) |
| EscalationLevel (value object/enum) | Escalation progression | NONE -> FIRST -> SECOND -> EMERGENCY |

**AlertStatus** states: OPEN, ACKNOWLEDGED, IN_PROGRESS, RESOLVED, FALSE_POSITIVE

**AlertType** enum (with default severity):

| Type | Default Severity |
|------|------------------|
| MICRO_ARC_DETECTED | CRITICAL |
| OVERHEATING | CRITICAL |
| CRITICAL_RISK_SCORE | CRITICAL |
| INSULATION_DEGRADATION | WARNING |
| LOOSE_CONNECTION | WARNING |
| ABNORMAL_TRANSIENT | WARNING |
| HARMONIC_DISTORTION | WARNING |
| HIGH_RISK_SCORE | WARNING |
| LOAD_IMBALANCE | INFO |
| SENSOR_OFFLINE | INFO |
| BASELINE_DEVIATION | INFO |

### 2.7 Notification

| Element | Role | Key Fields |
|---------|------|------------|
| **Notification** (aggregate root) | Single delivery attempt to one user via one channel | id (UUID), tenantId, recipientId (UserId), channel (NotificationChannel), severity (AlertSeverity), subject, body, alertFingerprint, status (NotificationStatus), retryCount, nextRetryAt, sentAt, failureReason |
| DeduplicationKey (value object) | Anti-spam composite key | recipientId + channel + alertFingerprint |
| ChannelRoutingPolicy (value object) | Maps severity to channels | severity -> set of channels |
| Recipient (value object) | Delivery target with consent flags | userId, email, phone, consentEmail, consentSms, consentPush |
| NotificationTemplate (value object) | Message template with variables | severity, subject template, body template, variables: {alertType}, {deviceId}, {occurredAt} |

**NotificationChannel** enum: EMAIL, SMS, PUSH, WEBHOOK, DASHBOARD

**NotificationStatus** states: PENDING -> SENT | RETRYING -> SENT | FAILED

### 2.8 Maintenance

| Element | Role | Key Fields |
|---------|------|------------|
| **Intervention** (aggregate root) | Maintenance task from alert to resolution | id (UUID), tenantId, sourceAlertId (AlertId), deviceId, type (InterventionType), priority (InterventionPriority), status (InterventionStatus), assignedElectricianId, scheduledAt, diagnostic (FieldDiagnostic), result (InterventionResult), riskImpact (RiskImpact) |
| FieldDiagnostic (value object) | Technician field observations | observations (JSONB), photos, measurements |
| RiskImpact (value object) | Effectiveness measurement | riskScoreBefore, riskScoreAfter, avoidedIncidentEstimateDays |

**InterventionType** enum: PREVENTIVE, CORRECTIVE, PREDICTIVE, EMERGENCY

**InterventionPriority** enum: LOW, MEDIUM, HIGH, URGENT

**InterventionStatus** states: CREATED -> PLANNED -> ASSIGNED -> IN_PROGRESS -> COMPLETED | CANCELLED

**InterventionResult** enum: CONFIRMED_DEFECT, NO_DEFECT_FOUND, REPAIRED, REPLACED_COMPONENT, NEEDS_FOLLOW_UP

### 2.9 Reporting

| Element | Role | Key Fields |
|---------|------|------------|
| **Report** (aggregate root) | Generated document (PDF) | id (UUID), reportNumber, tenantId, buildingId, type (ReportType), periodStart, periodEnd, status (ReportStatus), metadata (ReportMetadata), signature (ReportSignature), content (bytes), fileName |
| ReportMetadata (value object) | Report metadata | title, author, pageCount, generationDurationMs |
| ReportSignature (value object) | Integrity verification | algorithm (SHA-256), hash, computedAt |
| DownloadToken (value object) | Temporary download access | token (UUID string), expiresAt (24h TTL) |

**ReportType** enum: MONTHLY_HEALTH, CONTINUOUS_MONITORING_CERTIFICATE, CRITICAL_ALERT_REPORT, INTERVENTION_REPORT, ROI_AVOIDED_INCIDENTS, INSURER_EXPORT

**ReportStatus** states: PENDING -> GENERATING -> GENERATED | FAILED

---

## 3. Value Objects

Value objects are immutable, compared by value (not identity), and carry no lifecycle. They
live in the Shared Kernel or within their bounded context's domain layer.

### 3.1 Strongly-Typed Identifiers (Shared Kernel)

All IDs are `record` types wrapping a `UUID`, implementing the `ValueObject` marker interface.

| Value Object | Package | Purpose |
|--------------|---------|---------|
| TenantId | `com.pyrosense.shared.id` | Tenant identity, partition key for multi-tenancy |
| DeviceId | `com.pyrosense.shared.id` | Sensor/device identity |
| UserId | `com.pyrosense.shared.id` | Human user identity |
| AlertId | `com.pyrosense.shared.id` | Alert identity |
| RiskAssessmentId | `com.pyrosense.shared.id` | Risk assessment identity |
| BuildingId | `com.pyrosense.shared.id` | Building identity |
| ElectricalPanelId | `com.pyrosense.shared.id` | Electrical panel identity |
| CircuitId | `com.pyrosense.shared.id` | Circuit identity |

Each provides `generate()` (new UUID) and `from(String)` (parse existing).

### 3.2 Domain Enums & Value Objects

| Value Object | Context | Values / Description |
|--------------|---------|----------------------|
| AlertSeverity | Shared Kernel | INFO (level 1), WARNING (level 2), CRITICAL (level 3). Ordered, with `isHigherThan()` and `isAtLeast()` comparison methods. |
| AlertType | Alerting | OVERHEATING, MICRO_ARC_DETECTED, CURRENT_SPIKE (mapped to CRITICAL_RISK_SCORE), VIBRATION_ANOMALY (mapped to ABNORMAL_TRANSIENT), HUMIDITY_ALERT (mapped to INSULATION_DEGRADATION), + 6 others. Each carries a default severity. |
| MeasurementValue | Ingestion | Immutable record of one sensor reading: value (double), unit (String), timestamp |
| Temperature | Ingestion (implicit) | Represented as `temperatureCelsius` (double) on TelemetryReading. Physical bounds: [-40, 200] C |
| Location | Device | Composite of buildingId + panelId + circuitId -- identifies physical installation point |
| SlaPolicy | Alerting | Deadlines per severity: CRITICAL < 24h, WARNING < 7d, INFO < 30d. Escalation interval: 4h |
| DeduplicationKey (Alert) | Alerting | Composite: `deviceId + alertType`. Two alerts with same key within active window are deduplicated. |
| DeduplicationKey (Notification) | Notification | Composite: `recipientId + channel + alertFingerprint`. Window: 30 minutes. |
| DeduplicationKey (Telemetry) | Ingestion | Based on `payloadHash` (SHA-256 of raw payload). Checked via Redis with 24h TTL. |
| RiskScore | Shared Kernel | `record RiskScore(int value)` clamped to [0, 100]. Provides `level()` method returning RiskLevel. |
| Percentage | Shared Kernel | `record Percentage(double value)` validated [0, 100]. |
| Money | Shared Kernel | `record Money(BigDecimal amount, Currency currency)`. Non-negative. Planned for maintenance cost tracking. |
| DefectType | Shared Kernel | MICRO_ARC, OVERHEATING, LOOSE_CONNECTION, INSULATION_DEGRADATION, HARMONIC_DISTORTION, OVERCURRENT, POWER_QUALITY |
| AuditMetadata | Shared Kernel | createdAt, createdBy, updatedAt, updatedBy -- attached to all aggregates |

---

## 4. Domain Events

Domain events are immutable records published to Kafka topics. They represent facts that
happened in the system. Partition key is always `deviceId` (ensures ordering per device).

### 4.1 Device Management Events

| Event | Published When | Key Payload |
|-------|----------------|-------------|
| DeviceRegisteredEvent | Device created in system | deviceId, serialNumber |
| DeviceProvisionedEvent | Device assigned to tenant/building/panel | deviceId, tenantId, buildingId, panelId |
| DeviceActivatedEvent | First telemetry received | deviceId |
| DeviceOfflineDetectedEvent | Heartbeat timeout exceeded | deviceId, lastSeenAt |
| DeviceRevokedEvent | Device permanently decommissioned | deviceId, reason |

### 4.2 Telemetry Ingestion Events

| Event | Published When | Key Payload |
|-------|----------------|-------------|
| TelemetryReceivedEvent | Valid telemetry persisted to TimescaleDB | readingId, deviceId, tenantId, timestamp, all metrics |
| HeartbeatReceivedEvent | Device liveness signal processed | deviceId, receivedAt |
| MicroArcDetectedEvent | Non-zero microArcCount in reading (early signal) | deviceId, microArcCount, timestamp |

### 4.3 Signal Analysis Events

| Event | Published When | Key Payload |
|-------|----------------|-------------|
| SignalAnomalyDetectedEvent | Statistical anomaly detected | deviceId, anomalyType, confidence, zScore, metric |
| BaselineBuiltEvent | Baseline reaches minimum sample threshold | deviceId, sampleCount, features |
| BaselineDriftDetectedEvent | Baseline mean shifted beyond threshold | deviceId, feature, driftPercent |

### 4.4 Risk Scoring Events

| Event | Published When | Key Payload |
|-------|----------------|-------------|
| RiskScoreUpdatedEvent | Every scoring computation | deviceId, score, level, trend, factors |
| CriticalRiskDetectedEvent | Score >= 80 | deviceId, score, factors, recommendation |
| RiskLevelChangedEvent | Level transitions (e.g., MODERATE -> HIGH) | deviceId, previousLevel, newLevel, score |

### 4.5 Alerting Events

| Event | Published When | Key Payload |
|-------|----------------|-------------|
| AlertCreatedEvent | New alert created (not deduplicated) | alertId, tenantId, deviceId, severity, alertType |
| AlertAcknowledgedEvent | User acknowledges alert | alertId, acknowledgedBy |
| AlertAssignedEvent | Alert assigned to technician | alertId, assigneeId, assignedBy |
| AlertResolvedEvent | Alert resolved or marked false positive | alertId, resolvedBy, isFalsePositive |
| AlertEscalatedEvent | Alert escalated to next level | alertId, previousLevel, newLevel, severity |

### 4.6 Notification Events

| Event | Published When | Key Payload |
|-------|----------------|-------------|
| NotificationSentEvent | Notification delivered successfully | notificationId, channel, recipientId, sentAt |
| NotificationFailedEvent | All retry attempts exhausted (max 3) | notificationId, channel, attemptCount, failureReason |

### 4.7 Maintenance Events

| Event | Published When | Key Payload |
|-------|----------------|-------------|
| MaintenanceInterventionCreatedEvent | Intervention created from alert | interventionId, alertId, deviceId, priority |
| MaintenanceInterventionCompletedEvent | Intervention completed | interventionId, result, riskImpact |
| ElectricalDefectConfirmedEvent | Result = CONFIRMED/REPAIRED/REPLACED | interventionId, defectType (positive feedback for AI) |
| FalsePositiveConfirmedEvent | Result = NO_DEFECT_FOUND | interventionId, alertId (negative feedback for threshold adjustment) |

### 4.8 Reporting Events

| Event | Published When | Key Payload |
|-------|----------------|-------------|
| ReportGeneratedEvent | Report PDF ready | reportId, reportNumber, tenantId, type, fileName |
| ComplianceCertificateGeneratedEvent | Compliance certificate issued | reportId, tenantId, buildingId, periodEnd |

---

## 5. Business Rules (Invariants)

### 5.1 Alert Lifecycle Rules

| Rule | Description | Enforcement |
|------|-------------|-------------|
| No acknowledge after resolve | An alert cannot be acknowledged if already in RESOLVED or FALSE_POSITIVE state | `AlertStatus.canTransitionTo()` method throws `InvalidStateTransitionException` |
| No re-resolve | Terminal states (RESOLVED, FALSE_POSITIVE) cannot transition to any other state | State machine validation in Alert aggregate |
| Severity never downgraded | Alert severity can only be escalated, never reduced | Not exposed as a mutation method on the aggregate |

### 5.2 Deduplication Rules

| Rule | Description | Enforcement |
|------|-------------|-------------|
| Alert deduplication | Same deviceId + alertType within active (non-resolved) window -> increment `occurrenceCount` on existing alert, do NOT create new alert | `CreateAlertUseCase` checks active alerts by DeduplicationKey before creating |
| Telemetry idempotency | Duplicate readings identified by `payloadHash` are silently discarded | Redis-based idempotency store with 24h TTL in Ingestion service |
| Notification anti-spam | Same recipient + channel + alertFingerprint within 30-minute window -> skip | `DeduplicationPort` checked before dispatch |

### 5.3 SLA & Escalation Rules

| Rule | Description | Enforcement |
|------|-------------|-------------|
| SLA breach -> auto-escalate | Alert unacknowledged beyond SLA deadline -> escalate to next level | `EscalationScheduler` runs every 5 minutes, calls `Alert.shouldEscalate()` |
| Escalation interval | CRITICAL alerts escalate every 4 hours if unactioned | Configurable via `pyrosense.alerting.sla.escalation-interval` |
| SLA deadlines | CRITICAL: 24h, WARNING: 7d, INFO: 30d | `SlaPolicy` value object applied at alert creation |

### 5.4 Risk Scoring Rules

| Rule | Description | Enforcement |
|------|-------------|-------------|
| Score range | Risk score always between 0 and 100 inclusive | `RiskScore` value object constructor clamps value |
| Composite formula | Score = sum of (weight_i x normalizedValue_i) x 100 | `RiskScoringEngine` domain service |
| Weighted factors | Temperature: 0.35, Current: 0.25, Vibration: 0.20, Humidity: 0.10, Arc: 0.10 | `ScoringWeights` value object, configurable via application.yml |
| Minimum confidence | Anomalies with confidence < 30% excluded from scoring | Filtered in use case before passing to engine |

### 5.5 Tenant Isolation Rules

| Rule | Description | Enforcement |
|------|-------------|-------------|
| Device belongs to tenant | A device must be provisioned to exactly one tenant | `Device.provision()` requires non-null TenantId |
| No cross-tenant data access | Every query includes tenantId filter | `TenantContext` thread-local + repository-level filtering |
| Tenant-scoped events | All domain events carry tenantId | Event record fields, partition strategy |

### 5.6 Telemetry Validation Rules

| Rule | Description | Enforcement |
|------|-------------|-------------|
| Schema validation | All required fields present with correct types | `TelemetryValidator` domain service |
| Physical bounds | Temperature: [-40, 200]C, Voltage: [0, 500]V, Current: [0, 1000]A | Validation rules in Ingestion domain |
| Timestamp sanity | Not in future (> 5 min ahead), not too old (> 24h) | `TelemetryValidator` |
| Idempotency | Deduplicated by payloadHash (SHA-256 of raw MQTT payload) | Redis IdempotencyStore, TTL 24h |

### 5.7 Notification Rules

| Rule | Description | Enforcement |
|------|-------------|-------------|
| Consent required | No notification sent without recipient consent for that channel | `Recipient.canReceive(channel)` check before dispatch |
| Max retries | 3 retry attempts with exponential backoff (30s, 2m, 10m) | `Notification.markFailed()` increments count, computes nextRetryAt |
| Channel routing | INFO: dashboard only. WARNING: email + dashboard. CRITICAL: SMS + push + email + dashboard | `ChannelRoutingPolicy` value object |

### 5.8 Domain Purity Rules (Architecture)

| Rule | Description | Enforcement |
|------|-------------|-------------|
| No framework in domain | Domain layer has zero Spring/JPA/Kafka annotations | ArchUnit tests in every service (10+ rules each) |
| Single aggregate per transaction | Each use case modifies at most one aggregate | Code review + architectural convention |
| Immutable events | Domain events are Java records, never modified after publication | Record types (final fields) |

---

## 6. Context Map (Relationships)

Bounded contexts communicate primarily via domain events through Apache Kafka (KRaft mode).
Synchronous REST calls are used only for client-facing queries via the API Gateway.

### 6.1 Event Flow Diagram

```
                    ┌──────────────────┐
                    │  IoT Sensors     │
                    │  (MQTT 5.0)      │
                    └────────┬─────────┘
                             │ MQTT (TLS)
                             ▼
┌─────────────┐    ┌──────────────────┐
│   Device    │◄───│    Ingestion     │──── TelemetryReceivedEvent ────┐
│ Management  │    │    Service       │                                │
└─────────────┘    └──────────────────┘                                │
       │                                                               ▼
       │ (validates device                              ┌──────────────────────┐
       │  origin for                                    │   Signal Analysis    │
       │  telemetry)                                    │                      │
       │                                                └───────────┬──────────┘
       │                                                            │
       │                               SignalAnomalyDetectedEvent   │
       │                               BaselineDriftDetectedEvent   │
       │                                                            ▼
       │                                                ┌──────────────────────┐
       │                                                │   Risk Scoring       │
       │                                                │                      │
       │                                                └───────────┬──────────┘
       │                                                            │
       │                               RiskScoreUpdatedEvent        │
       │                               CriticalRiskDetectedEvent    │
       │                                                            ▼
       │                                                ┌──────────────────────┐
       │                                                │    Alerting          │
       │                                                │                      │
       │                                                └──────┬───────────────┘
       │                                                       │
       │                                    AlertCreatedEvent   │
       │                                    AlertEscalatedEvent │
       │                               ┌───────────────────────┼─────────────┐
       │                               ▼                       ▼             ▼
       │                ┌──────────────────────┐  ┌─────────────────┐  ┌───────────┐
       │                │   Notification       │  │  Maintenance    │  │ Reporting │
       │                │                      │  │                 │  │           │
       │                └──────────────────────┘  └────────┬────────┘  └───────────┘
       │                                                   │
       │        FalsePositiveConfirmedEvent                 │
       │        ElectricalDefectConfirmedEvent              │
       │        ┌──────────────────────────────────────────┘
       │        │         (feedback loop)
       │        ▼
       │  ┌──────────────────────┐
       └─►│   Signal Analysis    │ (threshold adjustment)
          └──────────────────────┘
```

### 6.2 Relationship Types

| Upstream Context | Downstream Context | Relationship | Mechanism |
|------------------|--------------------|--------------|-----------|
| Device Management | Ingestion | Conformist | Ingestion validates device existence via device registration data |
| Ingestion | Signal Analysis | Published Language | `TelemetryReceivedEvent` via Kafka topic `telemetry-events` |
| Signal Analysis | Risk Scoring | Published Language | `SignalAnomalyDetectedEvent` via Kafka topic `analysis-events` |
| Risk Scoring | Alerting | Published Language | `CriticalRiskDetectedEvent`, `RiskLevelChangedEvent` via Kafka topic `scoring-events` |
| Alerting | Notification | Published Language | `AlertCreatedEvent`, `AlertEscalatedEvent` via Kafka topic `alerting-events` |
| Alerting | Maintenance | Published Language | `AlertCreatedEvent` (CRITICAL/WARNING) via Kafka topic `alerting-events` |
| Maintenance | Signal Analysis | Published Language | `FalsePositiveConfirmedEvent` via Kafka topic `maintenance-events` (feedback loop) |
| Maintenance | Risk Scoring | Published Language | `MaintenanceInterventionCompletedEvent` via Kafka topic `maintenance-events` (triggers re-evaluation) |
| Identity & Access | All contexts | Shared Kernel | JWT claims carry tenantId + roles; each service validates independently |

### 6.3 Kafka Topics

| Topic | Producer | Consumer(s) |
|-------|----------|-------------|
| `telemetry-events` | Ingestion | Signal Analysis |
| `analysis-events` | Signal Analysis | Risk Scoring, Alerting (high-confidence anomalies) |
| `scoring-events` | Risk Scoring | Alerting |
| `alerting-events` | Alerting | Notification, Maintenance |
| `maintenance-events` | Maintenance | Signal Analysis (feedback), Risk Scoring (re-evaluation) |

**Delivery guarantees**: At-least-once (idempotent consumers required). Partition key = deviceId (ensures per-device ordering).

---

## 7. Glossary

| Term | Definition |
|------|------------|
| **Tenant** | An organization (property manager, building owner, insurer) subscribing to PyroSense. All data is strictly isolated per tenant. |
| **Building** | A physical structure monitored by PyroSense. Contains one or more electrical panels. Identified by BuildingId. |
| **Electrical Panel** | A distribution board (French: "tableau electrique") housing circuit breakers. Sensors are installed at this level. |
| **Circuit** | A single electrical circuit within a panel (e.g., "Kitchen outlets", "HVAC compressor"). Identified by breaker position. |
| **Device / Sensor** | A physical IoT device installed on an electrical panel. Measures electrical and thermal parameters continuously and transmits via MQTT. In MVP, devices are simulated by the IoT simulator tool. |
| **Telemetry Reading** | A single measurement snapshot from a sensor at a point in time. Contains 10+ metrics. Immutable once ingested. Stored in TimescaleDB. |
| **Baseline** | A statistical model of "normal" behavior for a specific device, built incrementally using Welford's online algorithm. Requires a minimum number of samples before anomaly detection activates. |
| **Anomaly** | A detected deviation from the statistical baseline or rule-based threshold. Has a type, confidence score [0,1], and z-score. May indicate an emerging electrical fault. |
| **Risk Score** | A composite numeric score (0-100) representing the probability and severity of an electrical fire risk for a device. Computed from weighted anomaly factors. Always explainable. |
| **Alert** | A notification-worthy event created when risk exceeds a threshold or a critical anomaly is detected. Has severity (INFO/WARNING/CRITICAL), lifecycle (OPEN -> RESOLVED), and SLA tracking. |
| **Deduplication** | The process of preventing duplicate entities. Applied to telemetry (payloadHash), alerts (deviceId + alertType), and notifications (recipient + channel + fingerprint). |
| **SLA (Service Level Agreement)** | Time-bound commitment for alert response. CRITICAL: acknowledge within 24h. Breach triggers automatic escalation. |
| **Escalation** | Automatic widening of notification audience when an alert remains unactioned beyond its SLA. Levels: NONE -> FIRST -> SECOND -> EMERGENCY. |
| **Intervention** | A physical maintenance task performed by an electrician, triggered by an alert. Tracks the full lifecycle from creation to field resolution. |
| **False Positive** | An alert that, after field inspection, is determined to not represent an actual defect. Feeds back into Signal Analysis for threshold calibration. |
| **Welford's Algorithm** | An online algorithm for computing running mean and variance in a numerically stable way (avoids catastrophic cancellation). Used for baseline learning without storing all historical readings. |
| **Hexagonal Architecture** | Architectural pattern (Ports & Adapters) where the domain has zero dependencies on frameworks. Driving adapters (REST, Kafka consumers) call use case ports; driven adapters (JPA, Kafka producers) implement repository/publisher ports. |
| **Shared Kernel** | A small, carefully curated library (`pyrosense-shared-kernel`) containing only cross-cutting primitives: base classes, strongly-typed IDs, common value objects, and exceptions. Contains no business logic. |
| **Domain Event** | An immutable record representing something that happened in the domain. Published to Kafka for asynchronous inter-service communication. Never modified after publication. |
| **Idempotency** | The property that processing the same input multiple times produces the same result as processing it once. Enforced via Redis-stored deduplication keys with TTL. |
| **Multi-tenancy** | Architecture where a single platform instance serves multiple isolated tenants. Every database query is filtered by tenantId. No tenant can access another tenant's data. |

---

## Implementation Status

| Aspect | Status | Notes |
|--------|--------|-------|
| 9 bounded contexts with hexagonal architecture | Implemented | Full domain models, use cases, adapters |
| Statistical anomaly detection (z-score, Welford's) | Implemented | 6 detector types active |
| Rule-based risk scoring with weighted factors | Implemented | Configurable weights, recency/repetition modifiers |
| Alert lifecycle with deduplication and escalation | Implemented | Full state machine, SLA tracking |
| Multi-channel notification with retry | Implemented | Logging adapters only (simulated delivery) |
| Maintenance with feedback loop | Implemented | False positive events published back to Analysis |
| Report generation (PDF) | Implemented | Integrity signature, download tokens |
| ML-based anomaly detection | Simulated (NoOp) | Port interface ready, adapter returns empty |
| ML-based risk prediction | Simulated (NoOp) | Port interface ready, logs predictions only |
| Real notification providers (Twilio, SendGrid) | Not implemented | Logging providers simulate delivery |
| Real IoT hardware | Not implemented | Simulated via `pyrosense-iot-simulator` tool |
| Multi-region deployment | Not implemented | Single-region only in MVP |
| Edge inference | Not implemented | All processing is cloud-side |
