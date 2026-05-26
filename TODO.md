# PyroSense AI Platform - TODO & Progress Tracker

## Status: MVP 1 Complete | MVP 2 Planned | Pilot Transition Planned

---

## DONE

### Project Structure
- [x] Parent POM with centralized dependency management
- [x] Maven profiles: local, test, docker, prod
- [x] Plugin configuration: compiler, surefire, failsafe, jacoco, spotless, owasp
- [x] Spring Boot plugin on executable modules only
- [x] .gitignore (secrets, IDE, build artifacts)
- [x] .env.example (documented environment variables)
- [x] owasp-suppressions.xml

### Shared Kernel (FULLY IMPLEMENTED - 77 tests passing)
- [x] DomainEvent interface
- [x] AggregateRoot base class (with event registration)
- [x] DomainEntity interface
- [x] ValueObject marker interface
- [x] IntegrationEvent record (Kafka envelope)
- [x] Exception hierarchy: BusinessException, NotFoundException, ValidationException, InvalidStateTransitionException
- [x] ErrorCode enum (maps to HTTP status codes)
- [x] Strongly typed IDs: TenantId, UserId, DeviceId, BuildingId, ElectricalPanelId, CircuitId, AlertId, RiskAssessmentId
- [x] RiskScore value object (0-100, severity mapping, comparable)
- [x] Percentage value object (0-100, fraction conversion)
- [x] Money value object (BigDecimal + Currency, arithmetic)
- [x] AlertSeverity enum (INFO, WARNING, CRITICAL with levels)
- [x] DefectType enum (7 types: INSULATION_DEGRADATION, LOOSE_CONNECTION, MICRO_ARC, OVERLOAD, HARMONIC_DISTORTION, TEMPERATURE_RISE, UNKNOWN)
- [x] ClockProvider (testable clock abstraction)
- [x] IdGenerator utility (UUID v4)
- [x] AuditMetadata record (createdAt, updatedAt, createdBy, updatedBy)
- [x] Page / PageRequest pagination model (framework-agnostic)
- [x] ApiErrorResponse (RFC 7807 inspired, standard error format)
- [x] Unit tests: AggregateRootTest, RiskScoreTest (16 tests), PercentageTest (11 tests), StronglyTypedIdTest (7 tests), ExceptionTest (8 tests)
- [x] ArchUnit rules: no Spring deps, no JPA, no Kafka, domain isolation, exception hierarchy, ID ValueObject compliance

### Device Service (FULLY IMPLEMENTED - 32 tests passing)
- [x] Domain model: Device aggregate (state machine), DeviceStatus, ConnectivityType
- [x] Domain events: DeviceRegistered, DeviceProvisioned, DeviceActivated, DeviceOfflineDetected, DeviceRevoked
- [x] Ports in: RegisterDeviceUseCase, ProvisionDeviceUseCase, ActivateDeviceUseCase, RevokeDeviceUseCase, GetDeviceQuery, RecordHeartbeatUseCase
- [x] Ports out: DeviceRepositoryPort, DeviceEventPublisherPort, EnrollmentKeyGeneratorPort
- [x] Use case implementations (6 services)
- [x] REST controller with OpenAPI, role-based security (@PreAuthorize ADMIN/DEVICE_MANAGER)
- [x] DTOs with Jakarta Validation
- [x] GlobalExceptionHandler (RFC 7807 ApiErrorResponse)
- [x] JPA entity + Spring Data repository + DevicePersistenceAdapter + mapper
- [x] Kafka event publisher (IntegrationEvent envelope)
- [x] HmacEnrollmentKeyGenerator (SecureRandom + SHA-256, never exposes plaintext after creation)
- [x] SecurityConfig (OAuth2 JWT + Keycloak realm role extraction)
- [x] Flyway V001 (devices table with indexes)
- [x] Unit tests: 20 domain, 4 use case, 7 ArchUnit
- [x] Integration tests: 5 persistence (H2)
- [x] Context load test

### Ingestion Service (FULLY IMPLEMENTED - 38 tests passing)
- [x] Domain model: TelemetryReading (builder, with building/panel/circuit IDs), DeviceHeartbeat
- [x] Domain validation: TelemetryValidator (range checks, clock drift, multi-violation)
- [x] Domain events: TelemetryReceivedEvent, MicroArcDetectedEvent, HeartbeatReceivedEvent
- [x] Ports in: IngestTelemetryUseCase, IngestHeartbeatUseCase
- [x] Ports out: TelemetryRepositoryPort, TelemetryEventPublisherPort, DeviceAuthorizationPort, IdempotencyPort, TelemetryQueryPort, RejectionRepositoryPort, HeartbeatRepositoryPort
- [x] Use case implementations: IngestTelemetryService (validation, auth, idempotency, event publishing), IngestHeartbeatService (persist + publish)
- [x] MQTT inbound adapter (HiveMQ client, topic pattern pyrosense/{tenantId}/{deviceId}/telemetry|heartbeat|events)
- [x] REST inbound adapter (fallback ingestion endpoint)
- [x] JDBC batch persistence adapter (bypasses JPA for time-series write performance)
- [x] JDBC query adapter (raw + continuous aggregate queries, RowMapper-based)
- [x] JDBC rejection repository (audit trail)
- [x] JDBC heartbeat repository
- [x] Kafka outbound adapter (telemetry-events topic + DLQ)
- [x] Redis idempotency adapter (deviceId + timestamp + payloadHash, 24h TTL)
- [x] HTTP device authorization adapter (checks device-service for active status + tenant ownership)
- [x] Backpressure: PayloadSizeLimitFilter (8KB max)
- [x] Micrometer metrics: ingestion_received_total, ingestion_rejected_total, ingestion_latency_ms, device_heartbeat_total
- [x] SecurityConfig (stateless, ingestion endpoints open for device auth)
- [x] KafkaConfig (telemetry-events 6 partitions, ingestion-dlq)
- [x] Sensor simulator (Java, MQTT publisher, configurable interval)
- [x] Unit tests: 7 domain model, 12 validator, 8 use case telemetry, 2 heartbeat, 6 ArchUnit
- [x] Integration tests: 3 JDBC persistence (H2)
- [x] Context load test

### Time-Series Storage (TimescaleDB)
- [x] V001: Enable TimescaleDB extension
- [x] V002: `electrical_telemetry` hypertable (7-day chunks, no UUID PK, composite indexes)
- [x] V003: `device_heartbeats` hypertable
- [x] V004: `ingestion_rejections` hypertable (30-day chunks)
- [x] V005: Continuous aggregates (1min, 15min, 1hour, daily) with AVG/MAX/SUM/STDDEV
- [x] V006: Retention policies (raw 90d, 1min 180d, 15min 1y, 1h 2y, daily indefinite) + refresh schedules
- [x] Covering index for risk-related queries (INCLUDE micro_arc, hf_noise, temperature, thd)
- [x] Sparse indexes (WHERE micro_arc_count > 0, WHERE circuit_id IS NOT NULL)
- [x] JDBC batch write adapter (10-50x faster than JPA for append-only workload)
- [x] JDBC read query adapter (raw + aggregate views, query routing by time range)
- [x] Multi-tenancy via tenant_id column + tenant-prefixed indexes (chunk exclusion)
- [x] GDPR: anonymization/export design (DELETE/UPDATE + COPY TO CSV)
- [x] docs/timeseries-storage.md (schema design, capacity planning, retention, GDPR)

### Signal Analysis Service (FULLY IMPLEMENTED - 66 tests passing)
- [x] Domain model: SignalWindow, SignalFeature, SignalAnomaly, AnomalyType, AnalysisResult, BaselineProfile, StatisticalRange, DetectionThresholds
- [x] Detection algorithms (pure Java, no Spring): ZScoreDetector, MicroArcPatternDetector, TemperatureTrendDetector, ThdDriftDetector, ExponentialSmoothingDetector
- [x] BaselineBuilder (Welford's online algorithm, running stats, percentiles)
- [x] SignalAnalysisEngine (orchestrates all detectors, deduplicates, computes aggregate risk)
- [x] Domain events: SignalAnomalyDetectedEvent, BaselineBuiltEvent, BaselineDriftDetectedEvent
- [x] Ports in: AnalyzeSignalUseCase, BuildBaselineUseCase, DetectSignalDriftUseCase, DetectMicroArcPatternUseCase
- [x] Ports out: AnalysisEventPublisherPort, BaselineProfileRepositoryPort, AnomalyRepositoryPort, MachineLearningInferencePort, TelemetryQueryPort
- [x] Use case implementations: AnalyzeSignalService, BuildBaselineService, DetectSignalDriftService, DetectMicroArcPatternService
- [x] Kafka consumer (telemetry-events topic, parses IntegrationEvent, feeds analysis pipeline)
- [x] REST API (GET /api/v1/analysis/anomalies/{deviceId}, GET /api/v1/analysis/baseline/{deviceId})
- [x] Kafka producer (analysis-events topic)
- [x] JDBC persistence: BaselineProfileRepository (JSONB), AnomalyRepository
- [x] ML port: MachineLearningInferencePort + NoOpMachineLearningAdapter (stub for MVP)
- [x] Externalized thresholds in application.yml (z-score, THD max, temperature max/rate, micro-arc recurrence, etc.)
- [x] Micrometer metrics (analysis.events.received/processed/failed, analysis.processing.duration)
- [x] Flyway migrations V001-V002 (baseline_profiles, analysis_results, signal_anomalies)
- [x] Unit tests: 15 domain model, 28 detection algorithms, 9 use case (mocked ports)
- [x] ArchUnit tests: 10 rules (hexagonal enforcement, no Spring in domain)
- [x] Context load test (embedded Kafka)
- [x] Documentation: docs/signal-analysis.md

### Risk Scoring Service (FULLY IMPLEMENTED - 58 tests passing)
- [x] Domain model: RiskScore (0-100, clamped), RiskLevel (LOW/MODERATE/HIGH/CRITICAL), RiskTrend, RiskFactor, RiskAssessment, AnomalyInput, ScoringWeights
- [x] RiskScoringEngine (pure domain logic): weighted multi-factor formula, recency decay, repetition boost, device reliability penalty
- [x] Scoring factors: micro-arc (0.30), THD drift (0.20), temperature (0.20), transient (0.10), HF noise (0.10), reliability (0.10)
- [x] Incident prediction: estimated days based on score level × trend multiplier
- [x] Trend detection: IMPROVING/STABLE/DEGRADING/CRITICAL from score history
- [x] Explainable output: per-factor contribution, recommendation text, predicted incident window
- [x] Domain events: RiskScoreUpdatedEvent, CriticalRiskDetectedEvent, RiskLevelChangedEvent
- [x] Ports in: CalculateRiskUseCase, GetRiskScoreQuery
- [x] Ports out: RiskAssessmentRepositoryPort, ScoringEventPublisherPort, RiskModelPort
- [x] Use case implementations: CalculateRiskService, GetRiskScoreService
- [x] Kafka consumer (analysis-events topic) + Kafka producer (scoring-events topic)
- [x] REST API: GET /devices/{id}/latest, GET /panels/{id}/history, GET /buildings/{id}/summary
- [x] JDBC persistence (risk_assessments table with JSONB factors)
- [x] ML port: RiskModelPort + NoOpRiskModelAdapter (stub for MVP)
- [x] Externalized weights in application.yml
- [x] Micrometer metrics (scoring.events.received/processed, scoring.calculation.duration)
- [x] Flyway migration V001 (risk_assessments table with indexes)
- [x] Unit tests: 24 domain model, 13 scoring engine, 5 use case, 8 ArchUnit, 1 context load
- [x] Documentation: docs/risk-scoring.md

### Alerting Service (FULLY IMPLEMENTED - 85 tests passing)
- [x] Domain model: Alert aggregate (state machine), AlertStatus (OPEN/ACKNOWLEDGED/IN_PROGRESS/RESOLVED/FALSE_POSITIVE), AlertType (11 types), EscalationLevel, SlaPolicy, DeduplicationKey, AlertComment
- [x] Domain events: AlertCreatedEvent, AlertAcknowledgedEvent, AlertAssignedEvent, AlertResolvedEvent, AlertEscalatedEvent
- [x] Ports in: CreateAlertUseCase (with deduplication), ManageAlertUseCase (acknowledge/assign/resolve/false-positive/comment), GetAlertQuery (multi-criteria), EscalateAlertsUseCase
- [x] Ports out: AlertRepositoryPort, AlertEventPublisherPort
- [x] Use case implementations: CreateAlertService, ManageAlertService, GetAlertService, EscalateAlertsService
- [x] Kafka consumer (scoring-events + analysis-events topics, maps to CreateAlertCommand)
- [x] REST API: GET /alerts (filters), GET /alerts/{id}, GET /alerts/critical, GET /alerts/statistics, POST acknowledge/assign/resolve/false-positive/comments
- [x] JDBC persistence (alerts table with JSONB comments)
- [x] Kafka producer (alerting-events topic)
- [x] GlobalExceptionHandler (RFC 7807 ApiErrorResponse)
- [x] Scheduled escalation (EscalationScheduler, configurable interval)
- [x] Externalized SLA/escalation config via @ConfigurationProperties
- [x] Flyway migration V001 (alerts table with indexes, deduplication partial index)
- [x] Unit tests: 23 domain model, 14 AlertStatus, 3 EscalationLevel, 3 SlaPolicy, 3 DeduplicationKey, 13 use case
- [x] Integration tests: 8 JDBC persistence (H2)
- [x] ArchUnit tests: 10 rules (hexagonal enforcement)
- [x] Context load test (embedded Kafka)
- [x] Documentation: docs/alerting-service.md

### Identity Service & Platform Security (FULLY IMPLEMENTED - 30 tests passing)
- [x] Domain model: User (login tracking, lockout), Role (8 roles → 34 permissions), Tenant, Membership, ServiceAccount, DeviceCredential
- [x] UserStatus state machine: ACTIVE, INACTIVE, SUSPENDED, LOCKED (auto-lock after 5 failures, auto-unlock after 30min)
- [x] Device credentials: HMAC-SHA256 tokens, SHA-256 hash storage, rotation, revocation, expiry, auth counting
- [x] Ports in: RegisterUserUseCase, ManageDeviceCredentialUseCase, ManageTenantUseCase
- [x] Ports out: UserRepository, TenantRepository, DeviceCredentialRepository, AuditLogRepository
- [x] Use case implementations: RegisterUserService, ManageDeviceCredentialService, ManageTenantService
- [x] SecurityConfig: OAuth2 JWT + PyroSenseGrantedAuthoritiesConverter (realm_access.roles + permissions), TenantContextFilter, CSP, CORS
- [x] AOP audit logging: @Audited annotation, captures userId/tenantId/action/resource/IP/user-agent
- [x] REST controllers: UserController, DeviceAuthController, TenantController with @PreAuthorize
- [x] In-memory repositories (MVP, to be replaced with JDBC)
- [x] Unit tests: UserTest (7), DeviceCredentialTest (6), RolePermissionTest (7), RegisterUserServiceTest (3), ManageDeviceCredentialServiceTest (6), ContextLoadTest (1)

### Platform-Wide Security
- [x] Shared Kernel: PlatformRole enum, Permission enum (34), TenantContext (ThreadLocal), SecurityContext record, AuditEntry record
- [x] Shared Kernel: PayloadIntegrity utility (HMAC-SHA256 sign/verify, 5min replay window, constant-time comparison)
- [x] Shared Kernel: @AllowedFields annotation (anti mass-assignment)
### API Gateway (FULLY IMPLEMENTED - 55 tests passing)
- [x] Routing: 11 routes to 9 downstream services (configurable URLs via env vars)
- [x] JWT validation: OAuth2 Resource Server with Keycloak (realm_access.roles + permissions extraction)
- [x] Tenant extraction: JwtHeaderPropagationFilter (X-Tenant-Id from JWT tenant_id claim)
- [x] Header propagation: X-Correlation-Id, X-Tenant-Id, X-User-Id, X-Roles to downstream services
- [x] Tenant header sanitization: strips X-Tenant-Id/X-User-Id/X-Roles from incoming requests (prevents spoofing)
- [x] Rate limiting: Redis-based (RateLimitStore port), 60/10/120 req/min by endpoint type, X-RateLimit headers
- [x] CORS: configurable origins, exposed headers, credentials, max-age
- [x] Request correlation: generates UUID if X-Correlation-Id missing, propagates and returns in response
- [x] Structured logging: logback-spring.xml with JSON output in prod (logstash-logback-encoder), MDC correlationId
- [x] Security headers: HSTS, X-Content-Type-Options, X-XSS-Protection, Referrer-Policy, Permissions-Policy, CSP, X-Frame-Options DENY
- [x] Error masking: ErrorMaskingFilter hides internal details, returns generic messages with correlationId
- [x] Payload size limit: PayloadSizeLimitFilter (configurable, default 1MB, returns 413)
- [x] Internal endpoint protection: /actuator/** requires ADMIN role (except health/info)
- [x] Health checks: /actuator/health, /actuator/info publicly accessible
- [x] Public endpoints: device auth validation, report download (token-gated)
- [x] SecurityConfig: CSP, frame-options DENY, cache disabled, PyroSenseGrantedAuthoritiesConverter
- [x] Filter ordering: sanitization(-20) → payload(-15) → tracing(-10) → propagation(-5) → headers(-2) → rate-limit(0)
- [x] Profiles: application.yml (base), application-local.yml (debug), application-prod.yml (strict), application-test.yml
- [x] Unit tests: 5 SecurityConfig, 3 RequestTracing, 5 TenantSanitization, 4 PayloadSizeLimit, 6 RateLimiting, 2 SecurityHeaders, 4 ErrorMasking, 5 JwtPropagation, 3 FilterOrder, 2 GatewayConfig
- [x] Integration tests: 4 GatewayApplicationTest (context load, health, 401, info)
- [x] Cross-service SecurityConfig: alerting, risk-scoring, device, ingestion — all with JWT + TenantContext + PyroSenseGrantedAuthoritiesConverter
- [x] application-secret.example.yml with Vault migration path
- [x] docs/security.md: STRIDE threat model, OWASP ASVS, role/permission matrix, IoT rules, attack protections

### Maintenance Service (FULLY IMPLEMENTED - 57 tests passing)
- [x] Domain model: Intervention aggregate (state machine), InterventionStatus (6 states), InterventionType, InterventionResult (6 outcomes incl. FALSE_POSITIVE), InterventionPriority, FieldDiagnostic, RiskImpact, InterventionComment
- [x] State machine: CREATED → PLANNED → ASSIGNED → IN_PROGRESS → COMPLETED (+ CANCELLED from any non-terminal)
- [x] Business rules: CRITICAL/WARNING alerts auto-create interventions, one per alert (unique constraint), severity → type/priority mapping
- [x] Business rule: diagnostic required before completion (Intervention.complete() throws if diagnostic == null)
- [x] Business rule: cancellation requires non-blank reason (Intervention.cancel(reason))
- [x] Domain events (8): MaintenanceInterventionCreatedEvent, PlannedEvent, AssignedEvent, StartedEvent, CompletedEvent, CancelledEvent, ElectricalDefectConfirmedEvent, FalsePositiveConfirmedEvent
- [x] Ports in: CreateInterventionUseCase, ManageInterventionUseCase (schedule/assign/start/diagnostic/complete/risk-impact/cancel), GetInterventionQuery (statistics), AddInterventionCommentUseCase
- [x] Ports out: InterventionRepositoryPort, MaintenanceEventPublisherPort, RiskScoreReevaluationPublisherPort, AlertLookupPort, TechnicianLookupPort, AuditLogPort
- [x] Use case implementations: CreateInterventionService, ManageInterventionService (publishes events on every transition + risk reevaluation on complete), GetInterventionService, AddInterventionCommentService
- [x] Kafka consumer: alerting-events → auto-creates interventions for CRITICAL/WARNING alerts
- [x] Kafka producer: maintenance-events (all domain events) + risk-reevaluation-requests (on completion)
- [x] REST API: POST /from-alert/{alertId}, POST /interventions, lifecycle (schedule/assign/start/diagnostic/complete/risk-impact/cancel), POST /{id}/comments, GET queries (list/by-id/electrician/device/overdue/kanban/statistics)
- [x] JDBC persistence: JdbcInterventionRepository with JSONB (diagnostic, risk_impact), cancellation_reason
- [x] SecurityConfig: OAuth2 JWT + TenantContext + @PreAuthorize per role (5 roles, ELECTRICIAN restricted)
- [x] GlobalExceptionHandler (RFC 7807 ApiErrorResponse + AccessDeniedException → 403)
- [x] Flyway V001: interventions table with indexes (tenant, status, device, electrician, alert unique)
- [x] Flyway V002: intervention_comments table + cancellation_reason column
- [x] Risk impact measurement: riskScoreBefore, riskScoreAfter, avoidedIncidentEstimate, riskReduction()
- [x] Feedback loop: FalsePositiveConfirmedEvent → signal-analysis threshold adjustment, ElectricalDefectConfirmedEvent → positive reinforcement
- [x] Stub adapters: StubAlertLookupAdapter, StubTechnicianLookupAdapter, LoggingAuditLogAdapter
- [x] Unit tests: 15 Intervention, 6 InterventionStatus, 4 RiskImpact, 4 CreateInterventionService, 9 ManageInterventionService
- [x] Security tests: 8 InterventionSecurityTest (role-based access validation)
- [x] ArchUnit tests: 10 rules (hexagonal enforcement)
- [x] Context load test (embedded Kafka)
- [x] Documentation: docs/maintenance-service.md

### Reporting Service (FULLY IMPLEMENTED - 41 tests passing)
- [x] Domain model: Report aggregate (state machine PENDING → GENERATING → GENERATED/FAILED), ReportType (6 types), ReportStatus, ReportMetadata, ReportSignature (SHA-256), DownloadToken (SecureRandom 32-byte, 15min TTL)
- [x] Unique report numbering: {PREFIX}-{YYYYMM}-{SEQUENCE:05d} (e.g., MH-202503-00042)
- [x] Logical signature: SHA-256 hash computed on PDF content, stored with report
- [x] Secure download: single-use expiring tokens (15min TTL, invalidated after use)
- [x] Domain events: ReportGeneratedEvent, ComplianceCertificateGeneratedEvent
- [x] Ports in: GenerateReportUseCase, GetReportQuery (findById/Tenant/Type/Building, createDownloadToken, findByDownloadToken)
- [x] Ports out: ReportRepositoryPort, ReportRendererPort, ReportDataProviderPort, ReportEventPublisherPort, DownloadTokenStorePort
- [x] Use case implementations: GenerateReportService (orchestrates data → render → persist → events), GetReportService (queries + token management)
- [x] PDF renderer: OpenPdfReportRenderer (ReportRendererPort adapter, no business logic in renderer)
- [x] REST API: POST /reports/monthly, GET /{id}, GET /{id}/download-token, GET /{id}/download?token=, GET /reports?tenantId&type, GET /building/{id}
- [x] Security: JWT + TenantContext, download endpoint public (token-gated), insurer access restricted to isInsurerAccessible reports
- [x] JDBC persistence: JdbcReportRepository with JSONB metadata
- [x] InMemoryDownloadTokenStore (ConcurrentHashMap, thread-safe)
- [x] StubReportDataProvider (MVP placeholder, to be replaced by real cross-service data)
- [x] Kafka producer: reporting-events topic
- [x] Flyway V001: reports table with indexes (tenant, building, type, status)
- [x] Unit tests: 12 ReportTest, 7 DownloadTokenTest, 4 ReportSignatureTest, 5 GenerateReportServiceTest, 8 GetReportServiceTest
- [x] Integration tests: 4 OpenPdfReportRendererTest (real PDF generation verification)
- [x] ArchUnit tests: 10 rules (hexagonal enforcement)
- [x] Context load test (embedded Kafka)
- [x] Documentation: docs/reporting.md

### Notification Service (FULLY IMPLEMENTED - 48 tests passing)
- [x] Domain model: Notification aggregate (retry state machine PENDING → SENT / RETRYING → FAILED), NotificationChannel (5: EMAIL, SMS, PUSH, WEBHOOK, DASHBOARD), NotificationStatus, RecipientType (4), DeduplicationKey
- [x] Channel routing policy: INFO→DASHBOARD, WARNING→EMAIL+DASHBOARD, CRITICAL→SMS+PUSH+EMAIL+DASHBOARD (pure domain logic)
- [x] Recipient model: consent flags (consentEmail, consentSms, consentPush), canReceive(channel) checks consent before dispatch
- [x] Templates per severity with variable substitution ({alertType}, {deviceId}, {occurredAt})
- [x] Anti-spam / deduplication: alertFingerprint (alertId:deviceId:severity) + recipientId + channel, 30-minute window
- [x] Retry with exponential backoff: 30s → 2min → 10min (max 3 retries), then FAILED
- [x] Domain events consumed: alerting.alert.created (from alerting-events topic)
- [x] Ports in: SendNotificationUseCase (dispatchForAlert), GetNotificationQuery, RetryNotificationUseCase
- [x] Ports out: EmailProviderPort, SmsProviderPort, PushProviderPort, WebhookProviderPort, RecipientResolverPort, DeduplicationPort, NotificationRepositoryPort
- [x] Use case implementations: SendNotificationService (routing + consent + dedup + dispatch), NotificationDispatcher, GetNotificationService, RetryNotificationService
- [x] Kafka consumer: KafkaAlertEventListener (alerting-events topic, filters alerting.alert.created)
- [x] REST API: GET /notifications?tenantId&status, GET /{id}, GET /recipient/{recipientId}, GET /statistics
- [x] Simulated adapters: LoggingEmailProvider, LoggingSmsProvider, LoggingPushProvider, LoggingWebhookProvider (masked PII in logs)
- [x] StubRecipientResolver (MVP, deterministic recipients per tenant)
- [x] InMemoryDeduplicationAdapter (ConcurrentHashMap, 30-min window)
- [x] JDBC persistence: JdbcNotificationRepository with state replay from DB
- [x] SecurityConfig: OAuth2 JWT + TenantContext
- [x] RetryScheduler: @Scheduled with configurable interval (default 30s)
- [x] RGPD compliance: no PII in persistence/logs, maskedPhone (***1234), maskedEmail (j***e@domain.com), consent enforcement
- [x] Flyway V001: notifications table with partial index on RETRYING status
- [x] Unit tests: 8 NotificationTest, 6 ChannelRoutingPolicyTest, 7 RecipientTest, 6 NotificationTemplateTest, 7 SendNotificationServiceTest, 3 RetryNotificationServiceTest
- [x] ArchUnit tests: 10 rules (hexagonal enforcement)
- [x] Context load test (embedded Kafka)
- [x] Documentation: docs/notification.md

### IoT Simulator (tools/pyrosense-iot-simulator - 38 tests passing)
- [x] Standalone Maven module (not part of platform build)
- [x] Domain model: SimulatedTenant, SimulatedBuilding, SimulatedDevice, TelemetryReading (10 metrics)
- [x] 7 scenarios: NORMAL, INSULATION_DEGRADATION_PROGRESSIVE, LOOSE_CONNECTION, MICRO_ARC_RECURRENT, OVERLOAD, TEMPERATURE_RISE, DEVICE_OFFLINE
- [x] ScenarioEngine with realistic data generation (progressive degradation, random spikes, sinusoidal overload)
- [x] SimulationEngine: multi-tenant, scheduled publishing, time acceleration, per-device scenario override
- [x] MQTT publisher (HiveMQ client, topic format pyrosense/{tenantId}/{deviceId}/telemetry)
- [x] REST publisher (Java HttpClient, fallback to ingestion REST API)
- [x] Interactive CLI: start/stop, scenario selection, device listing, status, config display
- [x] Full CLI arg parsing (--tenants, --devices, --interval, --acceleration, --days, etc.)
- [x] Dockerfile (multi-stage, eclipse-temurin:21)
- [x] README with usage examples, scenario descriptions, payload reference
- [x] Tests: TelemetryReadingTest (2), SimulatedDeviceTest (4), SimulatedTenantTest (4), SimulatorConfigTest (3), SimulationEngineTest (6), ScenarioEngineTest (19)

### Observability (PRODUCTION-READY - all 10 services instrumented)
- [x] Spring Boot Actuator: health, info, prometheus, metrics endpoints exposed (all others excluded)
- [x] Actuator security: /actuator/** requires ADMIN role (except health/info public)
- [x] Micrometer metrics: application tag, percentile histograms on HTTP requests
- [x] Prometheus integration: micrometer-registry-prometheus on all services
- [x] Business metrics: pyrosense_telemetry_*, pyrosense_analysis_*, pyrosense_risk_*, pyrosense_alerts_*, pyrosense_notifications_*, pyrosense_devices_*
- [x] Distributed tracing: Micrometer Tracing bridge → OpenTelemetry (OTLP HTTP 4318)
- [x] Trace context propagation: W3C traceparent (HTTP) + Kafka headers + X-Correlation-Id alignment
- [x] Sampling: configurable via TRACING_SAMPLING env var (default 1.0, recommend 0.1 in prod)
- [x] Structured logging: logstash-logback-encoder v7.4, JSON in docker/prod, human-readable in local/test
- [x] MDC fields: traceId, spanId, correlationId, tenantId (auto-propagated)
- [x] Logback per service: logback-spring.xml with profile-based appender selection
- [x] OpenTelemetry Collector: otel-collector-config.yml (OTLP gRPC+HTTP receivers, batch processor, memory limiter)
- [x] Prometheus alert rules: 14 rules in 6 groups (service-health, ingestion, kafka, devices, risk-alerting, notifications)
- [x] Grafana dashboards: 3 provisioned (platform-overview, ingestion-pipeline, alerting-notifications)
- [x] Grafana datasources: Prometheus + Loki (with traceId derived field)
- [x] Docker Compose: otel-collector service (otel/opentelemetry-collector-contrib:0.96.0), dashboards volume mount
- [x] Security: no PII in logs, no secrets logged, masked credentials, no payload content in logs
- [x] Gateway ObservabilityConfigTest: 4 tests (MeterRegistry, application tag, JVM metrics, process metrics)
- [x] Documentation: docs/observability.md (full stack reference, metrics catalog, alerts, dashboards, config)

### Quality Engineering (FULLY CONFIGURED)
- [x] Testing strategy: docs/testing-strategy.md (pyramid, layers, conventions, rules)
- [x] JaCoCo: per-layer coverage enforcement (domain 90%, application 85%, config excluded)
- [x] JaCoCo exclusions: *Application.java, *Config.java, adapter/in/rest/dto/**
- [x] ArchUnit per service: 10 rules (hexagonal, ports are interfaces, no field injection, layered)
- [x] ArchUnit platform-wide: PlatformArchitectureRulesTest (no cycles, no cross-context, controllers↛repos)
- [x] ArchUnit naming: NamingConventionTest (*UseCase, *Port, *Adapter, *Event, *Exception, *Config)
- [x] Security tests: TenantSecurityTest (JWT, header stripping, RBAC, actuator protection)
- [x] Performance smoke: IngestionPerformanceSmokeTest (1000 msgs < 5s)
- [x] Performance smoke: RiskScoringPerformanceSmokeTest (500 calcs < 3s)
- [x] Testcontainers: PostgreSQL/TimescaleDB (DevicePersistenceIT)
- [x] Testcontainers: Kafka (KafkaEventPublisherIT)
- [x] Testcontainers: Redis (RedisIdempotencyIT)
- [x] Testcontainers: shared container configs (singleton pattern)
- [x] Spotless: Palantir Java Format, enforced in validate phase and CI
- [x] GitHub Actions CI: 6 jobs (build, integration, coverage, security, docker, quality)
- [x] CI concurrency: cancel in-progress runs for same branch
- [x] CI artifacts: test results (7d), coverage (14d), OWASP reports (14d)
- [x] Maven profiles: test (failsafe), docker, prod (OWASP)

### Docker & Local Development (FULLY CONFIGURED)
- [x] docker-compose.yml: full stack with profiles (services, simulator, full)
- [x] docker-compose.override.yml: JDWP debug ports for all services (5010-5019)
- [x] Docker network: pyrosense-network (bridge, dedicated)
- [x] Named volumes: 8 persistent volumes (pgdata, redis, kafka, mosquitto, prometheus, grafana, loki)
- [x] Healthchecks: all services with start_period, interval, retries
- [x] Dockerfile per service: multi-stage (build + JRE), non-root user, healthcheck
- [x] .dockerignore: optimized build context (excludes target/, IDE, docs, tools)
- [x] .env.docker: local development defaults (no real secrets)
- [x] .env.example: documented template for all variables
- [x] application-docker.yml: Spring profile for all 10 services (Docker DNS resolution)
- [x] scripts/start-local.sh: interactive start with profile selection
- [x] scripts/stop-local.sh: graceful stop (--remove option)
- [x] scripts/reset-local.sh: destructive reset with confirmation (--force option)
- [x] Maven docker profile: spring-boot-maven-plugin with image naming
- [x] Prometheus config: dual targets (Docker DNS + host.docker.internal)
- [x] Kafka: external listener on port 29092 for IDE development
- [x] Documentation: docs/local-dev.md (workflows, ports, debugging, troubleshooting)

### Infrastructure
- [x] Docker Compose (PostgreSQL, Redis, Kafka, Mosquitto, Keycloak, Prometheus, Grafana, Loki, OpenTelemetry Collector)
- [x] Multi-database init script (9 databases with TimescaleDB)
- [x] Mosquitto config
- [x] Prometheus scrape config (all 10 services, dual targets)
- [x] Prometheus alert rules (14 rules, 6 groups)
- [x] Loki config
- [x] Grafana provisioning (datasources + 3 dashboards)
- [x] OpenTelemetry Collector config (OTLP receivers, batch processor, Prometheus exporter)

### Documentation (COMPLETE - 17 documents)
- [x] README.md (project overview, quick start, profiles, access points, debug ports, full doc links)
- [x] docs/architecture.md (C4 diagrams, hexagonal architecture, bounded contexts, 14 ADRs, limitations)
- [x] docs/domain-model.md (aggregates, entities, value objects, 40+ domain events, business rules, glossary)
- [x] docs/api-documentation.md (all REST endpoints, request/response examples, error codes, rate limiting)
- [x] docs/iot-protocol.md (MQTT topics, payloads, device authentication, heartbeat, integrity, simulator)
- [x] docs/security.md (STRIDE threat model, RBAC matrix, JWT claims, tenant isolation, OWASP mitigations)
- [x] docs/devops.md (Docker, CI/CD 6 jobs, K8s-ready, observability stack, backup, scaling)
- [x] docs/testing-strategy.md (pyramid, layers, coverage targets, ArchUnit rules, security, perf, CI, conventions)
- [x] docs/ml-strategy.md (statistical MVP, Welford baseline, ML roadmap, data requirements, limitations)
- [x] docs/pedagogical-guide.md (non-technical explanation, doctor analogy, data journey, ROI, glossary)
- [x] docs/production-readiness-checklist.md (80+ items, maturity levels MVP/Pilot/Production, roadmap)
- [x] docs/local-dev.md (Docker setup, profiles, port map, workflows, debugging, troubleshooting)
- [x] docs/timeseries-storage.md (schema, aggregates, retention, GDPR)
- [x] docs/signal-analysis.md (detection pipeline, algorithms, ML extension, thresholds)
- [x] docs/risk-scoring.md (formula, factors, explainability, ML extension, trend/prediction)
- [x] docs/alerting-service.md (lifecycle, deduplication, escalation, SLA, API, events)
- [x] docs/maintenance.md (lifecycle, feedback loop, risk impact, API, events)
- [x] docs/reporting.md (report types, PDF generation, secure download, signature, API, events)
- [x] docs/notification.md (channels, routing, retry/backoff, deduplication, RGPD, templates, API)
- [x] docs/api-gateway.md (routing, filters, security, rate limiting, header propagation, CORS, error masking)
- [x] docs/observability.md (stack, actuator, business metrics, tracing, logging, alerts, dashboards, config)
- [x] docs/pilot-transition-plan.md (hardware, certification, cloud, insurance, pilot 10/100/1000, budget, roadmap 12 mois)
- [x] docs/mvp2-plan.md (scope, bounded contexts, 22 user stories, roles, screens, endpoints, events, tables, rules, roadmap 6 sprints)
- [x] README per module (11 service READMEs)

### Per-Service Structure
- [x] Hexagonal package layout (domain/application/adapter/config)
- [x] Application.java (Spring Boot entry point)
- [x] application.yml + application-local.yml + application-test.yml
- [x] Context load test (*ApplicationTest.java)
- [x] At least one Use Case interface (port in)
- [x] At least one Repository interface (port out)
- [x] Use Case implementation (application service)
- [x] UseCaseConfig (Spring @Bean wiring)

---

## TODO - Next Steps

### Phase 1-4: DONE (voir section DONE ci-dessus)

### Phase 4b: Audit Remediation (DONE sauf 1 item)
- [ ] Fix MEDIUM: Identity-service Flyway migrations (replace in-memory repos)

### Phase 5: Advanced Features
- [ ] ML-based anomaly detection service (NoOp adapter ready for real model)
- [ ] Federated learning module
- [x] WebSocket real-time dashboard (Sprint F: STOMP/SockJS, live toasts, connection indicator)
- [x] PDF report generation (OpenPDF) — implemented in pyrosense-reporting-service
- [ ] Mobile push notifications (Firebase)
- [ ] Keycloak realm export (users, roles, client configuration)

### Phase 6: MVP 2 — Dashboard (DELIVERED — 6 sprints committed)

**Livraison**: Sprints 1-6 commits sur main (c4c63d3). Fonctionnel mais incomplet vs spec cible.

- [x] Scaffold Angular 18 SPA (pyrosense-dashboard)
- [x] Routing, guards (auth + role), interceptors
- [x] Layout (sidebar, topbar, notifications badge)
- [x] Dashboard overview (5 stat cards, mock fallback)
- [x] Building list + detail (mock data, polling capteurs)
- [x] Device detail (Chart.js temperature + puissance/THD, time range)
- [x] Alert list (filtres severite/statut, pagination, stats cards)
- [x] Alert detail (timeline, acknowledge, resolve, false-positive, comments)
- [x] Intervention list (kanban 5 colonnes, stats)
- [x] Intervention detail (lifecycle, diagnostic, completion)
- [x] Report list (filtre type, download PDF securise token)
- [x] Notification list (stats cards, severity/channel/status)
- [x] Admin (users table, audit log)
- [x] Backend: AlertController enrichi (buildingId filter, from/to)
- [x] Backend: InterventionController /kanban endpoint
- [x] Backend: AuditLogController (GET /api/v1/audit-log)

### Phase 7: Dashboard — Sprints A-F FAIT, Ecarts Residuels

**Cadrage complet**: voir `docs/dashboard-cadrage-complet.md`

#### Sprints A-E — DONE (commits 76ab52f → b2c7574)
- [x] Dashboard complet (6 stat cards, risk chart 30j, raccourcis cliquables)
- [x] Buildings list + detail (API reelle, statut OK/WATCH/AT_RISK/CRITICAL, risque chart, alertes, interventions)
- [x] Device list + detail (stats, search, filter, telemetry charts, anomalies)
- [x] Alertes (filtres severite/statut/periode, actions acknowledge/resolve/false-positive/create-intervention)
- [x] Interventions kanban (5 colonnes, priorite, date prevue, electricien)
- [x] Reports (generation, download PDF, historique)
- [x] Notifications (historique, stats, preferences)
- [x] Settings (seuils, contacts urgence, info tenant)
- [x] Admin (users, audit log)
- [x] Backend: GET/PUT /tenants/:id/settings, CRUD /emergency-contacts, GET/PUT /notifications/preferences
- [x] Backend: Flyway V002 notification_preferences
- [x] Responsive (BreakpointObserver, media queries)
- [x] Accessibilite (aria-labels, role, aria-current, focus)
- [x] Skeleton loaders + empty states
- [x] Tests unitaires Karma/Jasmine (59 tests)
- [x] Tests E2E Cypress (6 fichiers)
- [x] UnsavedChangesGuard sur settings

#### Sprint F — WebSocket Temps Reel (commit 29e579e)
- [x] STOMP broker backend (notification-service)
- [x] WebSocketService Angular (@stomp/stompjs)
- [x] Live alert toasts (MatSnackBar)
- [x] Connection indicator (wifi/wifi_off)
- [x] Dashboard auto-refresh on WebSocket events

#### Ecarts Residuels vs Spec Cible — DONE (commit 221c6cf)
- [x] Alertes: filtre par batiment (MatSelect buildingId)
- [x] Alertes: action "Assigner" sur detail (POST /alerts/:id/assign)
- [x] Building detail: onglet Tableaux electriques (groupement par panelId)
- [x] Settings: onglet Preferences notification (channels, severite, heures calmes, digest)
- [x] Mode SUPPORT_READONLY (masquer actions via auth.isReadOnly())
- [x] Building list: badge highestAlertSeverity sur cartes

### Phase 8: Dashboard Query API Backend (pyrosense-dashboard-service)

#### Service complet — DONE (23 tests passing)
- [x] Module Maven enregistre dans parent POM (port 8088)
- [x] Architecture hexagonale: domain/application/adapter/config
- [x] Domain models: DashboardOverview, RiskyBuilding, RiskTrendPoint, RecentAlert, PriorityIntervention, DeviceHealthSummary
- [x] Ports in: 6 query interfaces (GetDashboardOverview, GetRiskyBuildings, GetRiskTrend, GetRecentAlerts, GetPriorityInterventions, GetDeviceHealth)
- [x] Ports out: DashboardReadModelPort, DashboardCachePort
- [x] Use cases: 6 services avec cache-aside pattern (Redis TTL configurable)
- [x] REST controller: GET /api/v1/dashboard/{overview,risky-buildings,risk-trend,recent-alerts,priority-interventions,device-health}
- [x] Tenant isolation: TenantContext.require() + JWT tenant_id
- [x] Role-based access: TENANT_ADMIN, PROPERTY_MANAGER, ELECTRICIAN, SUPPORT_READONLY, PLATFORM_ADMIN
- [x] ELECTRICIAN scope: interventions filtrées par assignee
- [x] JdbcDashboardReadModel: read-model projections avec requetes SQL optimisees
- [x] RedisDashboardCache: cache serialization JSON avec TTL
- [x] DashboardCacheInvalidationListener: Kafka consumer (5 topics) pour invalidation
- [x] SecurityConfig: OAuth2 JWT + TenantContext filter + method security
- [x] Flyway V001: tables buildings, devices, alerts, interventions, dashboard_risk_trend avec index
- [x] DashboardProperties: TTL configurable (overview 30s, buildings 60s, trend 5min, health 60s)
- [x] Tests unitaires: GetDashboardOverviewServiceTest, GetRiskyBuildingsServiceTest (5 tests)
- [x] Tests REST: DashboardControllerTest (6 tests), DashboardSecurityTest (6 tests)
- [x] Tests architecture: ArchitectureTest (6 tests ArchUnit)
- [x] Integration test: JdbcDashboardReadModelIntegrationTest (Testcontainers PostgreSQL, 9 tests)
- [x] Documentation: docs/dashboard-api.md

### Phase 9: Frontend — Conformite Spec (ecarts combles)

#### Organisation & Composants manquants — DONE
- [x] core/models/ directory: interfaces TypeScript centralisees (device, alert, intervention, building, report, notification, risk, user)
- [x] MetricCardComponent: carte KPI reutilisable (icon, value, label, link, variant)
- [x] ConfirmDialogComponent: dialog Material pour confirmations destructives (remplace window.confirm)
- [x] StatusChipComponent: chip colore par statut (OPEN, IN_PROGRESS, RESOLVED, etc.)
- [x] shared/pipes/: RiskLevelPipe, RiskColorPipe, RelativeTimePipe
- [x] shared/utils/: risk.utils.ts (getRiskLevel, getRiskColor, getSeverityColor)
- [x] unsavedChangesGuard migre vers MatDialog (ConfirmDialogComponent)

#### Layout — DONE
- [x] Tenant name affiche dans la topbar (business icon + ID tronque)
- [x] User menu complet (MatMenu: nom, email, parametres, deconnexion)
- [x] Notification bell avec routerLink vers /notifications

#### Tests unitaires — DONE (109 tests total, 107 passent, 2 pre-existants flaky)
- [x] auth.interceptor.spec.ts (2 tests)
- [x] correlation.interceptor.spec.ts (2 tests)
- [x] tenant.interceptor.spec.ts (2 tests)
- [x] error.interceptor.spec.ts (4 tests)
- [x] role.guard.spec.ts (3 tests)
- [x] api.service.spec.ts (10 tests)
- [x] metric-card.component.spec.ts (4 tests)
- [x] confirm-dialog.component.spec.ts (4 tests)
- [x] status-chip.component.spec.ts (4 tests)
- [x] severity-badge.component.spec.ts (4 tests)
- [x] risk-level.pipe.spec.ts (6 tests)
- [x] relative-time.pipe.spec.ts (5 tests)
- [x] unsaved-changes.guard.spec.ts refactorise pour MatDialog (4 tests)

#### Documentation — DONE
- [x] pyrosense-dashboard/README.md (stack, structure, commandes, architecture, roles, Docker)
- [x] docs/frontend-dashboard.md (architecture, flux auth, composants, ecrans, pipes, erreurs, WS, responsive, securite, tests)

### Phase 10: Maintenance Service — Conformite Spec (ecarts combles)

#### Domain enrichi — DONE
- [x] InterventionComment record (id, authorId, content, createdAt)
- [x] InterventionResult.FALSE_POSITIVE enum value (+ isFalsePositive() updated)
- [x] Diagnostic required before completion (complete() throws InvalidStateTransitionException if null)
- [x] Cancellation requires non-blank reason (cancel(String reason))

#### Evenements Kafka (8 total) — DONE
- [x] MaintenanceInterventionPlannedEvent (new)
- [x] MaintenanceInterventionAssignedEvent (new)
- [x] MaintenanceInterventionStartedEvent (new)
- [x] MaintenanceInterventionCancelledEvent (new)
- [x] Events publies dans ManageInterventionService (assign/start/complete/cancel)

#### Ports out supplementaires — DONE
- [x] RiskScoreReevaluationPublisherPort + KafkaRiskReevaluationPublisher (risk-reevaluation-requests topic)
- [x] AlertLookupPort (with AlertInfo inner record) + StubAlertLookupAdapter
- [x] TechnicianLookupPort + StubTechnicianLookupAdapter
- [x] AuditLogPort + LoggingAuditLogAdapter

#### Endpoints REST — DONE
- [x] POST /api/v1/interventions/from-alert/{alertId} (creation depuis alerte)
- [x] POST /api/v1/interventions/{id}/comments (ajout commentaire)
- [x] AddInterventionCommentUseCase + AddInterventionCommentService

#### Persistence — DONE
- [x] Flyway V002: intervention_comments table + cancellation_reason column
- [x] H2 test migration V002 equivalent
- [x] JdbcInterventionRepository: cancellation_reason in UPDATE/INSERT, cancel(reason) in row mapper

#### Tests — DONE (57 tests passing, 0 failures)
- [x] InterventionSecurityTest (8 tests: role-based access, 401 unauthenticated, 403 forbidden)
- [x] InterventionTest updated: diagnostic before complete, cancel(reason)
- [x] ManageInterventionServiceTest updated: 3-arg constructor, event count assertions, cancel(id, reason)
- [x] InMemoryInterventionRepository: countOverdueByTenantId implemented
- [x] GlobalExceptionHandler: AccessDeniedException → 403 (prevents catch-all from swallowing it)

#### Documentation — DONE
- [x] docs/maintenance-service.md (architecture, lifecycle, priorities, results, business rules, REST API, Kafka events, schema, roles, config)

### Phase 7: Pilote Terrain (docs/pilot-transition-plan.md)

#### Mois 1-3 — Prototype Labo
- [ ] Sélection capteurs (pinces ampéro, sondes T°, détecteurs arc)
- [ ] Sélection microcontrôleur/gateway (ESP32/STM32/RPi)
- [ ] Développement firmware MQTT
- [ ] Tests banc de labo (données réelles vs simulateur)
- [ ] Déploiement cloud (Kubernetes, managed PostgreSQL, managed Kafka)
- [ ] Calibration algorithmes statistiques sur données réelles

#### Mois 4-6 — Pilote 10 Capteurs
- [ ] Installation terrain (1 bâtiment, 10 capteurs)
- [ ] Monitoring 24/7, collecte données terrain
- [ ] Ajustement seuils détection (faux positifs < 5%)
- [ ] Certification EMC + tests thermiques
- [ ] Début dossier IEC 61439 / NF C 15-100

#### Mois 7-12 — Pilote 100 Capteurs + Industrialisation
- [ ] Extension à 5 bâtiments, 100 capteurs
- [ ] ML v1 (Isolation Forest, LSTM) sur données terrain accumulées
- [ ] Application mobile (notifications push)
- [ ] Haute disponibilité (multi-AZ, DR)
- [ ] Certification complète + assurance RC Pro
- [ ] Préparation commercialisation (tarification, support, SLA)

---

## Architectural Decisions

| ADR | Decision | Justification |
|-----|----------|---------------|
| ADR-001 | Microservices architecture | Independent deployment, team autonomy, bounded context alignment |
| ADR-002 | Hexagonal architecture per service | Testable domain, framework independence |
| ADR-003 | Java 21 | Virtual threads, records, pattern matching, sealed classes |
| ADR-004 | Spring Boot 3.4.x | Stable LTS, easy migration path to 4.x |
| ADR-005 | Kafka KRaft | Event-driven, replay capability, no Zookeeper |
| ADR-006 | Database per service | Loose coupling, independent schema evolution |
| ADR-007 | TimescaleDB | Time-series native SQL, continuous aggregates |
| ADR-008 | Spring Cloud Gateway | Reactive, Spring ecosystem, JWT built-in |
| ADR-009 | Keycloak | OIDC standard, multi-tenant, self-hosted |
| ADR-010 | Statistical analysis (z-score) | Interpretable, fast MVP, extensible via interface |
| ADR-011 | Shared Kernel as library | Minimal coupling, shared DomainEvent contract only |
| ADR-012 | Spotless (not Checkstyle) | Modern, fast, auto-fixable, Palantir style |
| ADR-013 | Flyway (not Liquibase) | Plain SQL, TimescaleDB DDL compatibility |
| ADR-014 | Maven (not Gradle) | Team familiarity, stable, IDE support |

---

## Service Port Mapping

| Service | Port | Profile |
|---------|------|---------|
| API Gateway | 8080 | All profiles |
| Identity | 8081 | local, docker, prod |
| Device | 8082 | local, docker, prod |
| Ingestion | 8083 | local, docker, prod |
| Signal Analysis | 8084 | local, docker, prod |
| Risk Scoring | 8085 | local, docker, prod |
| Alerting | 8086 | local, docker, prod |
| Notification | 8087 | local, docker, prod |
| Reporting | 8091 | local, docker, prod |
| Maintenance | 8089 | local, docker, prod |
