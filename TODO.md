# PyroSense AI Platform - TODO & Progress Tracker

## Status: MVP 1 Complete | MVP 2 Complete | MVP 3 Complete | Pilot CONDITIONAL GO (pending hardware 24h test + electrician)

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

### Maintenance Service (FULLY IMPLEMENTED - 84 tests passing)
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

### Reporting Service (FULLY IMPLEMENTED - 79 tests passing)
- [x] Domain model: Report aggregate (state machine REQUESTED → GENERATING → GENERATED/FAILED/EXPIRED), ReportType (6 types), ReportStatus (5 states), ReportMetadata (enriched), ReportSignature (SHA-256), DownloadToken (SecureRandom 32-byte, 15min TTL), ReportPeriod, ReportRecipient, ReportFileReference
- [x] Unique report numbering: {PREFIX}-{YYYYMM}-{SEQUENCE:05d} (e.g., MH-202503-00042)
- [x] Logical signature: SHA-256 hash computed on PDF content, stored with report
- [x] Secure download: single-use expiring tokens (15min TTL, invalidated after use)
- [x] Report expiry: 90-day TTL, content purged on expiration, isExpired() check
- [x] Domain events: ReportGeneratedEvent, ComplianceCertificateGeneratedEvent
- [x] Ports in: GenerateReportUseCase, GetReportQuery, RequestReportUseCase (4 dedicated commands: MonthlyHealth, MonitoringCertificate, CriticalAlert, Intervention)
- [x] Ports out: ReportRepositoryPort, ReportRendererPort, ReportDataProviderPort, ReportEventPublisherPort, DownloadTokenStorePort, FileStoragePort, ReportAuditLogPort
- [x] Use case implementations: GenerateReportService (orchestrates data → render → persist → events), GetReportService (queries + token management), RequestReportService (dedicated report requests + file storage + audit)
- [x] PDF renderer: OpenPdfReportRenderer (ReportRendererPort adapter, no business logic in renderer, enriched sections)
- [x] REST API: POST /monthly-health, POST /monitoring-certificate, POST /critical-alert/{alertId}, POST /intervention/{interventionId}, POST /monthly (legacy), GET /{id}, GET /{id}/download-token, GET /{id}/download?token=, GET /reports?type, GET /building/{id}
- [x] Security: JWT + TenantContext, download endpoint public (token-gated), insurer access restricted to isInsurerAccessible + not expired
- [x] GlobalExceptionHandler: AccessDeniedException → 403 (prevents catch-all from swallowing)
- [x] JDBC persistence: JdbcReportRepository with JSONB metadata, EXPIRED status support
- [x] InMemoryDownloadTokenStore (ConcurrentHashMap, thread-safe)
- [x] LocalFileStorageAdapter (local filesystem, S3-compatible port for prod)
- [x] LoggingReportAuditLogAdapter (structured audit logging)
- [x] StubReportDataProvider (enriched MVP data: offline sensors, risk evolution, top risk buildings, ROI summary)
- [x] Kafka producer: reporting-events topic
- [x] Flyway V001: reports table with indexes (tenant, building, type, status)
- [x] Flyway V002: expires_at, source_alert_id, source_intervention_id, file storage columns, requester info
- [x] Monthly report content: période, capteurs actifs/offline, score moyen, alertes par sévérité, interventions créées/complétées, défauts confirmés, faux positifs, bâtiments les plus à risque, recommandations, évolution du risque, synthèse ROI
- [x] Certificate content: bâtiment, capteurs, taux disponibilité, statut monitoring, signature logique, numéro unique
- [x] Unit tests: 12 ReportTest, 7 DownloadTokenTest, 4 ReportSignatureTest, 4 ReportPeriodTest, 4 ReportExpiryTest
- [x] Service tests: 5 GenerateReportServiceTest, 8 GetReportServiceTest, 5 RequestReportServiceTest
- [x] Adapter tests: 4 OpenPdfReportRendererTest, 3 LocalFileStorageAdapterTest
- [x] Security tests: 7 ReportControllerSecurityTest (role-based access)
- [x] ArchUnit tests: 10 rules (hexagonal enforcement)
- [x] Context load test (embedded Kafka)
- [x] Documentation: docs/reporting-service.md

### Notification Service (FULLY IMPLEMENTED - 72 tests passing)
- [x] Domain model: Notification aggregate (retry state machine PENDING → SENT / RETRYING → FAILED / CANCELLED / SUPPRESSED), NotificationChannel (5: EMAIL, SMS, PUSH, WEBHOOK, DASHBOARD), NotificationStatus (6 states), RecipientType (4), DeduplicationKey, NotificationDeliveryAttempt
- [x] Channel routing policy: INFO→DASHBOARD, WARNING→EMAIL+DASHBOARD, CRITICAL→SMS+PUSH+EMAIL+DASHBOARD (pure domain logic)
- [x] Recipient model: consent flags (consentEmail, consentSms, consentPush), canReceive(channel) checks consent before dispatch
- [x] Templates per severity with variable substitution ({alertType}, {deviceId}, {occurredAt})
- [x] TemplateRendererPort + DefaultTemplateRenderer adapter (9 template keys, French content)
- [x] Anti-spam / deduplication: alertFingerprint (alertId:deviceId:severity) + recipientId + channel, configurable window
- [x] Redis deduplication: RedisDeduplicationAdapter (@Primary, key prefix notif:dedup:, TTL-based window)
- [x] InMemory deduplication: fallback for tests (@Profile("test"))
- [x] Retry with exponential backoff: 30s → 2min → 10min (max 3 retries), then FAILED
- [x] Preference enforcement: respect user preferences (email/sms/push disabled) except for CRITICAL severity
- [x] Domain events consumed (8 types): alerting.alert.created, alerting.alert.escalated, scoring.critical.risk.detected, maintenance.intervention.created, maintenance.intervention.assigned, maintenance.intervention.completed, reporting.report.generated, device.offline.detected
- [x] Ports in: SendNotificationUseCase, ProcessNotificationEventUseCase (8 methods), GetNotificationQuery, RetryNotificationUseCase, ManageNotificationPreferencesUseCase
- [x] Ports out: EmailProviderPort, SmsProviderPort, PushProviderPort, WebhookProviderPort, RecipientResolverPort, DeduplicationPort, NotificationRepositoryPort, NotificationPreferencesRepository, TemplateRendererPort, AuditLogPort
- [x] Use case implementations: SendNotificationService (routing + consent + prefs + dedup + dispatch), ProcessNotificationEventService (8 event handlers), NotificationDispatcher, GetNotificationService, RetryNotificationService, ManageNotificationPreferencesService
- [x] Kafka consumer: KafkaAlertEventListener (5 topics: alerting, scoring, maintenance, reporting, device)
- [x] REST API: GET /notifications, GET /{id}, POST /{id}/retry, GET /notification-preferences/me, PUT /notification-preferences/me, GET /notifications/statistics
- [x] Simulated adapters: LoggingEmailProvider, LoggingSmsProvider, LoggingPushProvider, LoggingWebhookProvider (masked PII in logs)
- [x] StubRecipientResolver (MVP, deterministic recipients per tenant)
- [x] AuditLogPort + LoggingAuditLogAdapter (structured audit logging)
- [x] JDBC persistence: JdbcNotificationRepository, JdbcNotificationPreferencesRepository
- [x] SecurityConfig: OAuth2 JWT + TenantContext
- [x] RetryScheduler: @Scheduled with configurable interval (default 30s)
- [x] RGPD compliance: no PII in persistence/logs, maskedPhone (***1234), maskedEmail (j***e@domain.com), consent enforcement
- [x] Flyway V001: notifications table with partial index on RETRYING status
- [x] Flyway V002: notification_preferences table
- [x] Flyway V003: notification_delivery_attempts table
- [x] Unit tests: 8 NotificationTest, 5 NotificationDeliveryAttemptTest, 6 ChannelRoutingPolicyTest, 7 RecipientTest, 6 NotificationTemplateTest, 7 SendNotificationServiceTest, 3 RetryNotificationServiceTest, 9 ProcessNotificationEventServiceTest, 6 DeduplicationAntiSpamTest, 4 PreferenceBypassTest
- [x] ArchUnit tests: 10 rules (hexagonal enforcement)
- [x] Context load test (embedded Kafka, 5 topics)
- [x] Documentation: docs/notification-service.md, docs/notification-rules.md

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
- [x] docs/mvp3-cadrage.md (cadrage complet 18 livrables: perimetre, hardware, firmware, MQTT, enrollment, securite IoT, labo, terrain, roadmap)
- [x] docs/edge-cloud-architecture.md (architecture edge-cloud detaillee: sequences, responsabilites, features edge/cloud, offline 72h, compression, versioning, compat firmware/backend)
- [x] docs/hardware-prototype-strategy.md (comparaison 3 options prototype: A lab BT 250 EUR, B non-invasif 765 EUR, C pre-certif 6050 EUR; BOM, risques, recommandations progressives, interfaces, validation expert)
- [x] docs/firmware-architecture.md (architecture firmware ESP32-S3 complete: ESP-IDF 5.x, 8 modules, 4 tasks FreeRTOS, 7 etats device, payloads types, erreurs, logs, tests, OTA, pseudo-code)
- [x] docs/firmware-getting-started.md (guide demarrage firmware: build host, build ESP32, config, tests, MQTT, troubleshooting)
- [x] docs/mqtt-protocol-v1.md (protocole MQTT v1 complet: 6 topics, 7 payloads, securite HMAC/anti-replay, pipeline validation 10 etapes, codes erreur, versioning)
- [x] docs/device-provisioning.md (protocole enrolement securise: claim token, credentials, rotation, revocation, rate limiting, audit)
- [x] firmware/pyrosense-device/ (squelette firmware complet: 8 modules, mock sensors, state machine, MQTT, offline queue, HMAC, 43 tests host)
- [x] docs/mvp2-plan.md (scope, bounded contexts, 22 user stories, roles, screens, endpoints, events, tables, rules, roadmap 6 sprints)
- [x] docs/mvp2-testing-strategy.md (strategie tests MVP 2, inventaire 863+ backend + 132+ frontend, objectifs couverture, CI pipeline, regles critiques)
- [x] docs/mvp2-local-run.md (Docker Compose MVP 2, profil mvp2, architecture, ports, modes dev, scripts, Grafana)
- [x] docs/mvp2-observability.md (metriques metier, alertes Prometheus, tracing OTEL, logs JSON, Grafana dashboard, contraintes PII)
- [x] docs/mvp2-overview.md (objectifs, perimetre, hors perimetre, dependances MVP 1, architecture, roles, chiffres cles)
- [x] docs/production-readiness-mvp2.md (checklist 46 items, risques, distinction prototype/pilote/production)
- [x] docs/audit-mvp2.md (audit securite complet, 4 vulnerabilites corrigees, 9 verifications strictes)
- [x] docs/mvp2-production-readiness-checklist.md (criteres par etape prototype/pilote/production, 8 categories)
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
- [x] Keycloak realm export (users, roles, client configuration) — infra/keycloak/realm-export.json + init-keycloak.sh

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

### Phase 11: Alert → Intervention Workflow Complet (84 tests passing)

#### Domain Model — DONE
- [x] InterventionRecommendation (PENDING → ACCEPTED | REJECTED | EXPIRED)
- [x] RecommendationStatus enum (PENDING, ACCEPTED, REJECTED, EXPIRED)
- [x] SlaPolicy record (response + resolution deadlines per priority)
- [x] InterventionPriorityPolicy (severity × riskScore → priority + type + SLA)

#### Workflow Logic — DONE
- [x] CRITICAL alert → auto-creates intervention (URGENT, SLA 4h response/24h resolution)
- [x] WARNING alert → creates InterventionRecommendation (priority based on risk score)
- [x] Manager accept/reject recommendation via use case + REST endpoints
- [x] Idempotence: unique index on alert_id (recommendations) + source_alert_id (interventions)
- [x] KafkaAlertEventListener checks for existing recommendation before processing

#### Ports & Use Cases — DONE
- [x] RecommendationRepositoryPort (save, findById, findByAlertId, findByTenantIdAndStatus, findPendingExpired)
- [x] ManageRecommendationUseCase (acceptRecommendation, rejectRecommendation, findPendingByTenant)
- [x] ManageRecommendationService (creates intervention on accept, publishes events, audit trail)

#### Events — DONE
- [x] RecommendationCreatedEvent (published on WARNING alert)
- [x] RecommendationAcceptedEvent (published on accept)
- [x] RecommendationRejectedEvent (published on reject)

#### REST API — DONE
- [x] GET /api/v1/recommendations (list pending for tenant)
- [x] POST /api/v1/recommendations/{id}/accept (creates intervention)
- [x] POST /api/v1/recommendations/{id}/reject (with mandatory reason)
- [x] @PreAuthorize: PLATFORM_ADMIN, TENANT_ADMIN, PROPERTY_MANAGER

#### Persistence — DONE
- [x] JdbcRecommendationRepository (upsert, queries, alert unique index)
- [x] Flyway V003: intervention_recommendations table with indexes
- [x] H2 test migration V003

#### Tests — DONE (84 tests total, 0 failures)
- [x] InterventionRecommendationTest (8 tests: create, accept, reject, expire, SLA breach, validation)
- [x] SlaPolicyTest (5 tests: deadlines per priority, breach detection)
- [x] InterventionPriorityPolicyTest (8 tests: severity/score mapping)
- [x] AlertToInterventionWorkflowTest (5 tests: full E2E critical path, warning+accept path, rejection, idempotence)
- [x] ConcurrencyTest (1 test: 10 threads racing on same alert, validates deduplication)

#### Audit & Error Handling — DONE
- [x] AuditLogPort.log(action, tenantId, details) default method for system actions
- [x] Audit entries: AUTO_INTERVENTION_CREATED, RECOMMENDATION_CREATED, ACCEPTED, REJECTED
- [x] Clear error messages: "already exists", "already decided", "Recommendation is already ACCEPTED"

#### Documentation — DONE
- [x] docs/alert-to-intervention-workflow.md (diagram, step-by-step, domain model, idempotence, audit, errors, config)

### Phase 12: Templates PDF Professionnels (pyrosense-reporting-service)

#### Architecture & Modele — DONE
- [x] ReportViewModel record (Header, Section, KeyValue, TableData, LegalDisclaimer, Signature, Footer)
- [x] Enums: SectionType (10 types), RiskLevel (4), Severity (4)
- [x] ReportViewModelBuilder: factory statique par type (buildMonthlyHealth, buildMonitoringCertificate, buildCriticalAlert, buildIntervention)
- [x] classifyRisk() / classifySeverity() (seuils 25/50/75)
- [x] PdfRendererPort (port hexagonal)
- [x] ProfessionalPdfRenderer (adapter OpenPDF, @Component)

#### 4 Types de rapports — DONE
- [x] Bilan Mensuel (6 sections: synthese, KPI, risques, alertes, interventions, recommandations)
- [x] Attestation de Surveillance (4 sections: objet, parametres, detections, conclusion)
- [x] Rapport d'Alerte Critique (4 sections: nature, contexte, evaluation, actions)
- [x] Rapport d'Intervention (3-4 sections: resume, indicateurs, impact, recommandations)

#### Design PDF — DONE
- [x] Format A4, marges 40pt, Helvetica 7-16pt
- [x] En-tete: bande sombre (45,55,72) + titre blanc
- [x] Badge risque colore (CRITICAL rouge, HIGH orange, MODERATE gris, LOW vert)
- [x] KPI cards grises (3/ligne) avec valeurs colorees par severite
- [x] Separateurs fins entre sections
- [x] Footer par page (rapport N°, pagination, generated by + version + date)
- [x] Bloc signature logique (SHA-256 + timestamp)
- [x] Compatible impression N&B

#### Conformite legale — DONE
- [x] Disclaimer standard: "aide a la decision", "monitoring predictif", "ne constitue en aucun cas"
- [x] Disclaimer certificat: reference decret n°2010-1016, "ne se substitue pas"
- [x] Pas de garantie absolue d'absence d'incendie
- [x] Recommandations: "preconisations d'inspection" + "professionnel qualifie"

#### Integration — DONE
- [x] GenerateReportService: 5-arg constructor (PdfRendererPort nullable, backward compatible)
- [x] UseCaseConfig: bean injecte PdfRendererPort
- [x] Switch expression par ReportType dans buildViewModel()
- [x] Fallback: ancien ReportRendererPort si PdfRendererPort absent

#### Tests — DONE (107 tests total, 0 failures)
- [x] ReportViewModelBuilderTest (12 tests: types, risk, header, period, KPI, recommendations, severity)
- [x] ProfessionalPdfRendererTest (17 tests: PDF valide, sections, disclaimer, signature, footer, wording)

#### Documentation — DONE
- [x] docs/pdf-templates.md (architecture, sections, design, legal, retrocompatibilite, tests)

### Phase 14: Preferences de Notification Avancees (pyrosense-notification-service)

#### Modele enrichi — DONE
- [x] NotificationPreferences: +language, +criticalOverrideEnabled, +phoneVerified, +emailVerified, +pushTokenRegistered
- [x] isChannelAllowed(channel, severity): logique combinee (pref user + verification technique + quiet hours + critical override)
- [x] isChannelTechnicallyReachable(channel): SMS→phoneVerified, Push→pushTokenRegistered, Email→emailVerified
- [x] isInQuietHours(time): gestion plage horaire (same-day + overnight crossing)
- [x] TenantNotificationPolicy: modele politique par tenant (defaults, criticalOverrideMandatory, requirePhoneVerification, requirePushToken)

#### Regles metier (7 regles) — DONE
- [x] Regle 1: utilisateur peut desactiver email pour INFO/WARNING
- [x] Regle 2: criticalOverride force les alertes CRITICAL si role impose (tenant policy mandatory)
- [x] Regle 3: quiet hours ne s'applique pas aux CRITICAL
- [x] Regle 4: SMS uniquement si telephone verifie
- [x] Regle 5: Push uniquement si token enregistre
- [x] Regle 6: gestionnaire configure les regles par defaut du tenant (TenantNotificationPolicy)
- [x] Regle 7: preferences auditees (AuditLogPort.log sur chaque modification)

#### Ports & Use Cases — DONE
- [x] ManageNotificationPreferencesUseCase: UpdatePreferencesCommand enrichi (11 champs)
- [x] ManageTenantNotificationPolicyUseCase: getPolicy + updatePolicy (UpdatePolicyCommand)
- [x] ManageNotificationPreferencesService: applique policy du tenant (criticalOverride, phoneVerification, pushToken)
- [x] ManageTenantNotificationPolicyService: CRUD policy + audit

#### Endpoints REST — DONE
- [x] GET /api/v1/notification-preferences/me
- [x] PUT /api/v1/notification-preferences/me (enrichi: language, criticalOverride, phone/email/push verification)
- [x] GET /api/v1/tenants/{tenantId}/notification-policy (@PreAuthorize PLATFORM_ADMIN, TENANT_ADMIN)
- [x] PUT /api/v1/tenants/{tenantId}/notification-policy (@PreAuthorize PLATFORM_ADMIN, TENANT_ADMIN)

#### Persistence — DONE
- [x] Flyway V004: ALTER notification_preferences (+5 colonnes), CREATE tenant_notification_policy
- [x] JdbcNotificationPreferencesRepository: reconstituteFull, save avec 15 colonnes
- [x] JdbcTenantNotificationPolicyRepository: findByTenantId, save (upsert)

#### Tests — DONE (189 tests total, 0 failures)
- [x] NotificationPreferencesRulesTest (15 tests: critical override, quiet hours, phone verification, push token, dashboard always allowed, overnight quiet hours)
- [x] ManagePreferencesServiceTest (9 tests: normal user, critical override force, SMS blocked unverified, push blocked, quiet hours, audit, language)
- [x] ManageTenantPolicyServiceTest (6 tests: default policy, manager update, audit, disable critical override, relax verification, existing policy update)
- [x] PreferenceBypassTest: mis a jour pour new model (verified channels)

### Phase 13: Templates de Notification (pyrosense-notification-service)

#### Architecture — DONE
- [x] NotificationTemplateCode enum (9 codes: ALERT_WARNING, ALERT_CRITICAL, CRITICAL_RISK_DETECTED, INTERVENTION_CREATED/ASSIGNED/COMPLETED, REPORT_GENERATED, DEVICE_OFFLINE, DEVICE_BACK_ONLINE)
- [x] NotificationPriority enum (LOW, MEDIUM, HIGH, URGENT)
- [x] NotificationTemplateDefinition record (4 channel contents, priority, CTA, privacy rules, required variables, missing variable detection)
- [x] NotificationTemplateRegistry (registre statique des 9 templates complets)
- [x] TemplateRendererPort enrichi (API channel-aware + backward-compatible legacy API)
- [x] DefaultTemplateRenderer (impl. substitution {var}, delegation au registre pour API typee)

#### 9 Templates complets (4 canaux chacun) — DONE
- [x] ALERT_WARNING: priorite MEDIUM, ton informatif, action sous 48h
- [x] ALERT_CRITICAL: priorite URGENT, ton calme, pas de panique, professionnel qualifie, pas de donnees brutes, pas de garantie incendie
- [x] CRITICAL_RISK_DETECTED: priorite URGENT, score presente comme estimation probabiliste
- [x] INTERVENTION_CREATED: priorite MEDIUM, information de suivi
- [x] INTERVENTION_ASSIGNED: priorite HIGH, notification electricien
- [x] INTERVENTION_COMPLETED: priorite LOW, rapport disponible
- [x] REPORT_GENERATED: priorite LOW, lien telechargement
- [x] DEVICE_OFFLINE: priorite HIGH, zone non surveillee, verifier connectivite
- [x] DEVICE_BACK_ONLINE: priorite LOW, surveillance retablie, aucune action

#### Regles de confidentialite — DONE
- [x] NO_RAW_ELECTRICAL_DATA (tous les templates)
- [x] NO_GUARANTEE_FIRE_PREDICTION (CRITICAL, RISK)
- [x] NO_EXACT_LOCATION (CRITICAL, RISK, DEVICE_OFFLINE)
- [x] NO_PERSONAL_INFO_IN_PUSH (INTERVENTION_ASSIGNED)
- [x] MASK_DEVICE_ID_IN_SMS (DEVICE_OFFLINE, DEVICE_BACK_ONLINE)

#### Evenement #9 — DONE
- [x] DeviceBackOnlineCommand (ProcessNotificationEventUseCase)
- [x] processDeviceBackOnline() handler (ProcessNotificationEventService)
- [x] KafkaAlertEventListener: "device.back.online" event type routing

#### Tests — DONE (159 tests total, 0 failures)
- [x] NotificationTemplateRegistryTest (64 tests: tous les codes ont template, 4 canaux, priorite, CTA, privacy, variables requises, rendu, variables manquantes, conformite editoriale CRITICAL, SMS ≤160 chars, dashboard ≤200 chars)
- [x] DefaultTemplateRendererTest (23 tests: legacy API, channel-aware API, tous codes/canaux, email > SMS, disclaimer CRITICAL)
- [x] NotificationTemplateTest (6 tests pre-existants: severity mapping, rendu)

#### Documentation — DONE
- [x] docs/notification-templates.md (canaux, 9 templates, regles editoriales, privacy, variables, tests, roadmap)

### Phase 15: Systeme d'Audit Transversal (pyrosense-notification-service)

#### Modele enrichi — DONE
- [x] AuditEntry enrichi (shared-kernel): +actorRole, +correlationId, +metadata (13 champs total)
- [x] Backward-compatible: ancien constructeur 10 args preserve pour identity-service
- [x] Nouveau factory: AuditEntry.createFull() avec 11 args (id + timestamp auto-generes)

#### Architecture — DONE
- [x] AuditLogRepository (port out): findById, findByTenant (pagine), findByTenantAndAction, findByTenantAndResource, countByTenant
- [x] GetAuditLogQuery (port in): AuditLogFilter (from, to, action, resourceType, page, size), AuditLogPage
- [x] GetAuditLogService (use case): routage filtre, isolation tenant sur findById
- [x] JdbcAuditLogRepository (adapter): persistance JDBC, masquage PII (regex: phone, email, password, token)
- [x] LoggingAuditLogAdapter: persiste via AuditLogRepository en plus du SLF4J

#### Endpoints REST — DONE
- [x] GET /api/v1/audit-logs (pagine, filtres: from, to, action, resourceType, page, size)
- [x] GET /api/v1/audit-logs/{id} (detail avec isolation tenant)
- [x] @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")

#### Securite — DONE
- [x] Acces restreint: PLATFORM_ADMIN et TENANT_ADMIN uniquement
- [x] Isolation tenant: findById filtre par tenantId (pas de cross-tenant)
- [x] PII masquee: telephone, email, password, token/secret rediges avant persistance
- [x] userAgent stocke mais NON expose dans les reponses API
- [x] Pagination obligatoire: taille max 200, defaut 50

#### Persistence — DONE
- [x] Flyway V005: table audit_log (13 colonnes + timestamp auto)
- [x] 5 index: tenant+time, action+time, resource+time, user+time, correlation_id
- [x] Masquage PII dans JdbcAuditLogRepository (sanitizeForStorage via regex)

#### Tests — DONE (206 tests total, 0 failures)
- [x] GetAuditLogServiceTest (9 tests: pagination, filtre action, filtre resource, isolation tenant, not found, page cap 200, negative page, total pages)
- [x] AuditLogSecurityTest (8 tests: annotation classe, roles admin only, mapping, parametres filtre, endpoint getById, pas de secrets, pas de userAgent, pagination response)

#### Documentation — DONE
- [x] docs/audit.md (architecture, modele, API, securite, PII, pagination, usage)

### Phase MVP 3.0: MQTT Protocol v1 (docs/mqtt-protocol-v1.md)

#### Documentation — DONE
- [x] docs/mqtt-protocol-v1.md (specification complete: topics, payloads, securite, validation pipeline, QoS, error codes, versioning)

#### Backend Protocol Validation (pyrosense-ingestion-service) — DONE
- [x] MqttProtocolConstants.java (schema versions, limits, topic patterns)
- [x] TelemetryPayloadV1.java (record: 11 features + security block)
- [x] HeartbeatPayloadV1.java (record: state, uptime, RSSI, buffer)
- [x] DeviceEventPayloadV1.java (record: 7 event types, severity, context)
- [x] ProvisioningRequestPayloadV1.java (record: claim token, capabilities)
- [x] ProvisioningResponsePayloadV1.java (record: MQTT config, telemetry config, security config)
- [x] DeviceCommandPayloadV1.java (record: 6 command types, payload map)
- [x] CommandAckPayloadV1.java (record: status enum)
- [x] PayloadValidator.java (schema version, topic coherence, timestamp freshness, field ranges, drain mode 72h)
- [x] SignatureVerifier.java (HMAC-SHA256, constant-time comparison, signature removal before compute)
- [x] AntiReplayGuard.java (nonce uniqueness, sequence monotonicity, messageId dedup, pluggable stores)
- [x] ProtocolValidationPipeline.java (5-step pipeline: validate → device auth → signature → anti-replay → firmware track)

#### Backend Tests — DONE (30 tests)
- [x] PayloadValidatorTest (10 tests: schema, mismatch, timestamp, drain, ranges, nonce, heartbeat, events)
- [x] SignatureVerifierTest (6 tests: valid, invalid, null key, empty key, disabled string, tampered)
- [x] AntiReplayGuardTest (7 tests: first message, duplicate messageId, nonce reuse, sequence regression, equal seq, increasing, independent devices)
- [x] ProtocolValidationPipelineTest (8 tests: valid, revoked, unknown, replay, duplicate msg, sequence regression, old timestamp, firmware track)

#### Firmware Protocol Conformance — DONE
- [x] Topic format aligned to v1: pyrosense/v1/{tenantId}/{deviceId}/{messageType}
- [x] MqttClient.set_tenant_id() for topic construction
- [x] PayloadBuilder: messageId (UUID v4) added to every telemetry payload
- [x] PayloadBuilder: isDrain flag for buffer drain mode
- [x] Tests updated: topic v1 assertion, messageId uniqueness, isDrain modes

### Phase MVP 3.1: Secure Device Provisioning (docs/device-provisioning.md)

#### Domain Model — DONE
- [x] ClaimToken (single-use, time-limited, SHA-256 hashed, invalidation)
- [x] DeviceCredential (HMAC key, hash-stored, versioned, revocable)
- [x] CredentialStatus enum (ACTIVE, REVOKED)
- [x] DeviceProvisioningSession (audit trail: serial, model, firmware, IP, status, failure reason)

#### Events — DONE
- [x] DeviceClaimTokenCreatedEvent (device.claim_token.created)
- [x] DeviceCredentialRotatedEvent (device.credential.rotated)
- [x] DeviceProvisionedEvent (existing, reused)
- [x] DeviceRevokedEvent (existing, reused)

#### Ports In (Use Cases) — DONE
- [x] CreateClaimTokenUseCase (generate single-use token, configurable validity)
- [x] DeviceProvisioningUseCase (claim token verification, credential issuance, MQTT config)
- [x] RotateDeviceCredentialUseCase (revoke old + issue new, version increment)
- [x] RevokeDeviceCredentialUseCase (revoke all credentials + claim tokens, device REVOKED)

#### Ports Out (Repositories) — DONE
- [x] ClaimTokenRepositoryPort (save, findActive, invalidateAll, countRecentByIp)
- [x] DeviceCredentialRepositoryPort (save, findActive, findAll, nextVersion)
- [x] ProvisioningSessionRepositoryPort (save, findByDevice, countRecentFailedByIp)
- [x] ProvisioningAuditPort (log claim, provisioning, rotation, revocation)

#### Use Case Implementations — DONE
- [x] CreateClaimTokenService (validates state, invalidates old tokens, publishes event, audits)
- [x] DeviceProvisioningService (rate limit, serial lookup, token verify, credential issue, session audit)
- [x] RotateDeviceCredentialService (revoke old, issue new, audit, publish)
- [x] RevokeDeviceCredentialService (revoke all creds + tokens, revoke device, audit, publish)

#### REST API — DONE
- [x] POST /api/v1/devices/{deviceId}/claim-token (ADMIN/DEVICE_MANAGER/PLATFORM_ADMIN/TENANT_ADMIN)
- [x] POST /api/v1/devices/provision (public — called by device, rate-limited)
- [x] POST /api/v1/devices/{deviceId}/credentials/rotate (ADMIN/DEVICE_MANAGER/PLATFORM_ADMIN)
- [x] POST /api/v1/devices/{deviceId}/revoke (ADMIN/DEVICE_MANAGER/PLATFORM_ADMIN)

#### Persistence — DONE
- [x] JdbcClaimTokenRepository (upsert, active query, invalidation)
- [x] JdbcDeviceCredentialRepository (upsert, version tracking, active query)
- [x] JdbcProvisioningSessionRepository (upsert, IP-based rate limit query)
- [x] LoggingProvisioningAuditAdapter (structured audit logs, no secrets)
- [x] Flyway V003: claim_tokens, device_credentials, provisioning_sessions (with indexes)

#### Security — DONE
- [x] Claim token: single-use, time-limited (24h default), hash-stored
- [x] Credentials: HMAC key shown once, SHA-256 hash stored, never returned again
- [x] Rate limiting: 10 failed attempts per IP per 15 minutes
- [x] Brute force: failed session tracking per IP
- [x] Replay: token consumed atomically, credential versioned
- [x] Audit: all operations logged without secrets
- [x] Revocation: immediate effect on credentials + tokens + device status
- [x] Provisioning endpoint public (device has no JWT yet), protected by claim token

#### Tests — DONE (26 tests)
- [x] ClaimTokenTest (8 tests: create, match, mismatch, consume, double-consume, expired, consume-expired, unique)
- [x] DeviceCredentialTest (6 tests: issue, revoke, revoke-idempotent, unique-keys, version, hash-format)
- [x] CreateClaimTokenServiceTest (5 tests: success, not-found, revoked, active-state, invalidates-old)
- [x] DeviceProvisioningServiceTest (7 tests: success, rate-limit, unknown-serial, invalid-token, expired, revoked, failed-session)
- [x] RotateDeviceCredentialServiceTest (4 tests: revoke-old-issue-new, without-existing, revoked-device, unknown)
- [x] RevokeDeviceCredentialServiceTest (4 tests: revokes-all, unknown, idempotent, invalidates-tokens)
- [x] ProvisioningSecurityTest (5 tests: role annotations, public provision, request validation)

#### Configuration — DONE
- [x] pyrosense.provisioning.claim-token-validity (default PT24H)
- [x] pyrosense.mqtt.broker-uri, pyrosense.mqtt.port

#### Documentation — DONE
- [x] docs/device-provisioning.md (workflow diagram, domain model, REST API, security properties, DB schema, config)

### Phase 7: Edge-Cloud Architecture Implementation (docs/edge-cloud-architecture.md)

#### Edge — Firmware ESP32-S3 (FreeRTOS)
- [ ] Acquisition task: ADC DMA 16kHz, buffer circulaire, ISR 1s
- [ ] Processing task: RMS (1s), THD/FFT 1024pts (10s), crest factor, zero-crossing
- [ ] Processing task: arcEnergy (50-100kHz band), transientCount, hfNoiseLevel
- [ ] Processing task: temperature (deltaT, rate of change °C/min), powerFactor
- [ ] Processing task: signalQuality score (self-diagnostic 0-100)
- [ ] Communication task: MQTT 5.0 client, TLS 1.3, X.509 mTLS (ATECC608B)
- [ ] Communication task: CBOR encoding, LZ4 compression, HMAC-SHA256 signature
- [ ] Communication task: buffer manager SPIFFS (72h, mode degrade, FIFO eviction)
- [ ] Communication task: drain algorithm (10 msg/s throttled, events first)
- [ ] Health task: NTP sync, watchdog feed, OTA check, cert rotation check
- [ ] State machine: BOOTING → PROVISIONING → CONNECTING → LEARNING → ACTIVE → OFFLINE
- [ ] Provisioning mode: CSR generation (ATECC608B), capability declaration
- [ ] OTA: A/B partitions, ECDSA signature verify, rollback auto (3 boot failures)
- [ ] Offline detection: mode REDUCED (24h), MINIMAL (48h), EMERGENCY (72h+)
- [ ] Time subsystem: SNTP client, monotonic sequence counter, drift monitor
- [ ] Payload versioning: v=2 field in every message, additive-only schema

#### Cloud — Backend Java/Spring Boot (Hexagonal)
- [ ] Ingestion adapter: CBOR decoder (cbor-java), LZ4 decompressor (lz4-java)
- [ ] Ingestion adapter: PayloadVersionRouter (v1 JSON legacy + v2 CBOR features)
- [ ] Ingestion adapter: MQTT 5.0 user properties extraction (cert-fp, fw-ver, payload-ver, compress)
- [ ] Ingestion adapter: HMAC-SHA256 signature verification
- [ ] Ingestion adapter: buffer drain mode (relaxed timestamp validation 72h window)
- [ ] Ingestion port: IngestFeaturesUseCase (FeatureFrame domain model)
- [ ] Ingestion port: IngestEventUseCase (ElectricalEvent domain model)
- [ ] Device port: EnrollDeviceUseCase (CSR validation, capability negotiation, cert issuance)
- [ ] Device port: RotateCertificateUseCase (nonce challenge, re-sign, grace period)
- [ ] Device port: CertificateAuthorityPort (cloud KMS signing adapter)
- [ ] Device port: CrlPublisherPort (S3 CRL file adapter)
- [ ] Device port: MqttCommandPublisherPort (EMQX publish QoS 2)
- [ ] Signal analysis: input FeatureFrame (pre-computed features, not raw measurements)
- [ ] Signal analysis: calibration coefficients per device (gain, offset per sensor)
- [ ] Signal analysis: data quality scoring (per device, impact on risk weight)
- [ ] Feature store: TimescaleDB continuous aggregates (1min, 15min, 1h)
- [ ] Feature store: retention policy (raw 90j, compressed 7j, aggregates permanent)
- [ ] Feature store: Parquet export batch job (S3, daily)

#### Communication Contract
- [ ] MQTT 5.0 topics: features/periodic, features/stats, events/electrical, status/heartbeat, status/health, command/*
- [ ] Payload schema v2: CBOR features frame (~180 bytes), JSON events (~300 bytes)
- [ ] Capability negotiation protocol: device declares → cloud responds with config
- [ ] Backward compatibility: cloud supports payload v1 (simulator) + v2 (sensor) simultaneously
- [ ] Device config push: featureInterval, statsInterval, heartbeatInterval, requiredFeatures

#### Infrastructure
- [ ] EMQX broker cluster (2 noeuds, Helm chart, mTLS termination, ACL per device)
- [ ] PKI: internal CA (cloud KMS), cert-manager, OCSP/CRL endpoint
- [ ] Shared subscriptions: $share/ingestion/pyrosense/+/+/features/# (load balancing)

### Phase 7a: Hardware Prototype (docs/hardware-prototype-strategy.md + docs/firmware-architecture.md)

#### Firmware Foundation (docs/firmware-architecture.md)
- [x] Projet ESP-IDF 5.x: CMakeLists, partitions.csv, sdkconfig.defaults
- [x] Modules: config (DeviceConfig, StateMachine), sensors (ISensorProvider, MockSensorProvider, SensorRegistry)
- [x] Modules: signal_processing (FeatureExtractor, RmsCalculator, SignalQuality)
- [x] Modules: telemetry (PayloadBuilder, HeartbeatBuilder), connectivity (MqttClient, WifiManager)
- [x] Modules: security (HmacSigner, NonceGenerator), storage (OfflineQueue), diagnostics (Logger, SelfTest)
- [x] State machine: 7 etats (BOOTING, PROVISIONING, CONNECTING, ACTIVE, OFFLINE_BUFFERING, DEGRADED, ERROR, REVOKED)
- [x] Host unit test framework: CMake host build + assert-based tests (43 tests, 7 suites)
- [ ] Component hal/: interfaces hardware abstraites (hal_i2c.h, hal_spi.h, hal_gpio.h, hal_adc.h)
- [ ] Component hal/mock/: mocks ESP-IDF pour tests host complets
- [ ] Task creation FreeRTOS: 4 tasks pinned (acquisition C1/P4, processing C0/P3, comms C0/P2, diag C0/P1)
- [ ] Inter-task queues FreeRTOS: raw_samples (64), feature (32), event (16), command (8)
- [ ] CI pipeline: build + host tests + static analysis (cppcheck) + size check

#### Sprint 3.1 — Option A: Lab Basse Tension (~250 EUR)
- [ ] Commande composants: ESP32-S3 DevKitC-1 N16R8, ADS1115, MAX31865, PT100, generateur signal
- [ ] Assemblage breadboard: ADC I2C + temperature SPI + LED status
- [ ] Firmware: driver ADS1115 (I2C, 860 SPS, 4 canaux differentiels)
- [ ] Firmware: driver MAX31865 (SPI, 3-wire PT100, compensation fil)
- [ ] Firmware: acquisition task (DMA-like timer ISR → buffer circulaire)
- [ ] Firmware: processing task (RMS, THD/FFT 1024pts sur signal generateur)
- [ ] Firmware: MQTT 5.0 publish features CBOR (WiFi, sans mTLS pour labo)
- [ ] Validation: comparaison valeurs mesurees vs valeurs injectees (±5% RMS, ±10% THD)
- [ ] Validation: stabilite 24h sans crash/watchdog/memory leak

#### Sprint 3.3 — Option B: Non-Invasif Supervise (~765 EUR)
- [ ] Commande composants: CT YHDC SCT-013-030, ZMPT101B, HLK-PM01, boitier DIN rail
- [ ] Installation par electricien habilite B2V+ (CT sur circuit reel, alimentation DIN)
- [ ] Firmware: calibration CT (facteur conversion A → mA, offset zero-crossing)
- [ ] Firmware: calibration tension (rapport transformateur ZMPT101B)
- [ ] Firmware: detection micro-arcs (bande 50-100 kHz, seuil adaptatif)
- [ ] Firmware: mode offline SPIFFS (buffer 72h, drain algorithm)
- [ ] Firmware: mTLS X.509 avec ATECC608B (CSR provisioning)
- [ ] Validation labo: injection defauts connus (arc serie, surcharge, harmoniques)
- [ ] Validation terrain: 1 tableau electrique, 2 circuits, 72h monitoring supervise
- [ ] Verification: precision ±2% RMS, ±5% THD vs reference Fluke 435-II

#### Sprint 3.6+ — Option C: Pre-Certification (si budget valide, ~6050 EUR)
- [ ] Conception PCB 4 couches (KiCad, separation analogique/numerique)
- [ ] Fabrication PCB prototype (JLCPCB, 5 exemplaires)
- [ ] Tests CEM pre-conformite (chambre semi-anechoique ou pre-scan)
- [ ] Tests thermiques (chambre climatique -10°C / +60°C)
- [ ] Validation precision ±1% (etalonnage vs reference certifiee)
- [ ] Dossier technique pre-IEC 61439 (schemas, nomenclature, tests)

### Phase 7b: Pilote Terrain (docs/pilot-transition-plan.md)

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

### Phase 16: MVP 2 Security Hardening (DONE)

#### Method Security — DONE
- [x] Dashboard: @PreAuthorize method-level sur getRiskTrend, getRecentAlerts, getPriorityInterventions, getDeviceHealth
- [x] Dashboard: ELECTRICIAN restreint a recent-alerts + priority-interventions (assignees)
- [x] Dashboard: OCCUPANT restreint a recent-alerts uniquement
- [x] Dashboard: SUPPORT_READONLY retire de overview, risky-buildings, risk-trend, device-health
- [x] Reporting: @AllowedFields sur tous les endpoints POST de generation
- [x] Notification: @AllowedFields sur preferences et policy PUT endpoints

#### Anti Mass-Assignment — DONE
- [x] AllowedFieldsInterceptor dans notification-service (enregistre dans WebMvcConfig)
- [x] AllowedFieldsInterceptor dans maintenance-service (nouveau)
- [x] AllowedFieldsInterceptor dans reporting-service (nouveau, ajoute a WebMvcConfig existant)
- [x] @AllowedFields annotation sur DTOs: notification preferences, tenant policy, report generation

#### Rate Limiting — DONE
- [x] Reporting: 5 req/min par tenant sur generation POST (existant)
- [x] Notification: 10 req/min par tenant sur retry POST (nouveau)
- [x] Notification: 20 req/min par tenant sur preferences PUT (nouveau)
- [x] RateLimitInterceptor enregistre dans WebMvcConfig notification-service

#### Sensitive Data Masking — DONE
- [x] Notification failureReason masque quand contient Exception ou >200 chars
- [x] Audit logs: PII masquee (phone, email, password, token) dans JdbcAuditLogRepository
- [x] userAgent stocke mais non expose dans API audit

#### PDF Download Protection — DONE
- [x] Token 32 bytes SecureRandom, 15min TTL, single-use (existant)
- [x] Headers securite: X-Content-Type-Options nosniff, Cache-Control no-store, Pragma no-cache
- [x] Content-Disposition attachment (force telechargement)
- [x] Report ID verifie contre token report ID

#### Tenant Isolation Tests — DONE
- [x] DashboardTenantIsolationTest (5 tests: overview query current tenant, risky-buildings, risk-trend, alerts, no-context fail)
- [x] ReportTenantIsolationTest (5 tests: list current tenant, cross-tenant rejected, same-tenant allowed, insurer blocked, electrician blocked)
- [x] InterventionTenantIsolationTest (4 tests: list, kanban, statistics, no-context fail)
- [x] NotificationTenantIsolationTest (5 tests: list, no-context, electrician blocked, occupant blocked, retry blocked)

#### Access Denied Tests — DONE
- [x] DashboardSecurityTest enrichi: electrician refus overview/risk-trend/device-health, support-readonly refus overview/risk-trend, occupant refus overview
- [x] ReportTenantIsolationTest: insurance_partner et electrician refus list
- [x] NotificationTenantIsolationTest: electrician/occupant refus list, electrician refus retry

#### Documentation — DONE
- [x] docs/mvp2-security.md (matrice acces corrigee, rate limiting, anti mass-assignment, PDF protection, masquage)

### Phase 21: MVP 2 Documentation Finale (DONE)

#### Documents Crees — DONE
- [x] docs/mvp2-overview.md (objectifs, perimetre, hors perimetre, dependances MVP 1, architecture, roles, chiffres)
- [x] docs/production-readiness-mvp2.md (checklist 46 items, risques techniques, distinction prototype/pilote/production)

#### Documents Existants Couvrant le Scope — DEJA FAIT
- [x] docs/dashboard-api.md + docs/frontend-dashboard.md (ecrans, roles, endpoints, regles UX)
- [x] docs/maintenance-service.md + docs/alert-to-intervention-workflow.md (cycle de vie, regles, SLA, workflow)
- [x] docs/reporting-service.md + docs/pdf-templates.md (types rapports, contenu, securite, limites legales)
- [x] docs/notification-service.md + docs/notification-templates.md + docs/notification-rules.md (canaux, templates, preferences, anti-spam)
- [x] docs/mvp2-security.md + docs/audit.md (RBAC, tenant isolation, audit, donnees personnelles)
- [x] docs/mvp2-events.md (topics Kafka, events, versioning, idempotence)
- [x] docs/mvp2-testing-strategy.md (strategie tests, comment lancer, couverture cible)
- [x] docs/mvp2-local-run.md (Docker, variables, monitoring, demarrage local)

#### README.md — DONE
- [x] Section MVP 2 ajoutee (demarrage rapide, endpoints principaux, comptes test)
- [x] Table documentation reorganisee (Architecture, Operations, MVP 2, Guides)
- [x] Profil Docker mvp2 documente

#### TODO.md — DONE
- [x] Phases 16-21 marquees DONE
- [x] Risques techniques listes
- [x] Prochaine etape MVP 3 proposee

### Phase 20: MVP 2 Observabilite Complete (DONE)

#### Metriques Metier — DONE
- [x] Dashboard: overview.requests, overview.latency, cache.hits, cache.misses, events.processed
- [x] Maintenance: interventions.created, interventions.completed, interventions.overdue (gauge), events.processed, dlq.events
- [x] Reporting: reports.requested, reports.generated, reports.failed, generation.duration, downloads, downloads.denied
- [x] Notification: sent, failed, suppressed, delivery.duration, events.processed, dlq.events
- [x] Securite: access.denied, suspicious.tenant.access, report.downloads.denied (sur chaque service)

#### Actuator + Micrometer + Prometheus — DONE
- [x] Dashboard service: management config complete (tracing, OTLP, metrics tags, histogram)
- [x] Maintenance service: deja configure (verifie)
- [x] Reporting service: deja configure (verifie)
- [x] Notification service: deja configure (verifie)
- [x] ObservabilityConfig.java avec metriques metier dans chaque service
- [x] Prometheus scrape job pour dashboard-service (port 8088)

#### Logs Structures JSON — DONE
- [x] Dashboard: logback-spring.xml (JSON docker/prod, texte local/test, traceId/spanId)
- [x] Maintenance: logback-spring.xml (deja en place)
- [x] Reporting: logback-spring.xml (deja en place)
- [x] Notification: logback-spring.xml (deja en place)
- [x] MDC: traceId, spanId, correlationId, tenantId dans tous les logs

#### Correlation ID + Tracing OpenTelemetry — DONE
- [x] X-Correlation-Id genere par gateway, propage a tous les services
- [x] W3C traceparent: propagation automatique (Micrometer Tracing bridge)
- [x] Kafka headers: trace context propage
- [x] OTLP endpoint configure sur tous les services MVP 2
- [x] Sampling configurable: TRACING_SAMPLING (defaut 1.0, recommande 0.1 en prod)

#### Alertes Prometheus — DONE (7 nouvelles regles)
- [x] NotificationFailuresHigh: rate(failed) > 0.5/s pendant 3m
- [x] ReportGenerationFailures: > 3 echecs en 15m
- [x] KafkaDlqNonEmpty: events en DLQ detectes
- [x] InterventionOverdueHigh: > 10 overdue pendant 5m
- [x] ApiErrorRateHigh: 5xx > 5% pendant 3m (par service MVP 2)
- [x] ReportDownloadDeniedSpike: > 10 refus en 5m
- [x] SuspiciousTenantAccessDetected: > 5 tentatives cross-tenant (CRITICAL)

#### Grafana Dashboard — DONE
- [x] mvp2-services.json: sante, HTTP rate/latency, Kafka lag, alertes, interventions, notifications, JVM, reports, cache

#### Contraintes Respectees — DONE
- [x] Pas de PII dans les logs (masquage regex: phone, email, password, token)
- [x] Pas de secret dans les metriques/logs
- [x] Pas de payload notification dans les logs
- [x] Tags metriques: service/status/channel/severity uniquement (jamais tenantId/userId)

#### Documentation — DONE
- [x] docs/mvp2-observability.md (metriques, alertes, tracing, logs, Grafana, contraintes)

### Phase 19: MVP 2 Docker Compose & Local Run (DONE)

#### Dockerfile — DONE
- [x] pyrosense-dashboard-service/Dockerfile (multi-stage, eclipse-temurin:21, non-root, healthcheck port 8088)

#### docker-compose.yml — DONE
- [x] Profil `mvp2` ajoute: gateway, dashboard-service, alerting, maintenance, reporting, notification, frontend
- [x] dashboard-service: PostgreSQL pyrosense_dashboard, Redis, Kafka, port 8088
- [x] dashboard-frontend: nginx, proxy /api/ vers gateway, port 4200
- [x] CORS_ORIGINS: http://localhost:4200,http://localhost:3000
- [x] DASHBOARD_SERVICE_URL ajoute au gateway
- [x] pyrosense_dashboard database dans init-multiple-dbs
- [x] Healthchecks sur tous les services

#### docker-compose.override.yml — DONE
- [x] dashboard-service: debug port 5020

#### .env.docker & .env.example — DONE
- [x] DASHBOARD_SERVICE_URL ajoute
- [x] CORS_ORIGINS inclut port 4200

#### Scripts — DONE
- [x] scripts/start-mvp2-local.sh (--no-build, --infra-only)
- [x] scripts/stop-mvp2-local.sh (--remove)
- [x] scripts/reset-mvp2-local.sh (--force)

#### Monitoring — DONE
- [x] Prometheus: scrape job pyrosense-dashboard (port 8088 + host.docker.internal)
- [x] Grafana dashboard: mvp2-services.json (sante, latence, Kafka lag, alertes, interventions, cache)

#### Documentation — DONE
- [x] docs/mvp2-local-run.md (architecture, ports, modes dev, flux Kafka, healthchecks, logs, troubleshooting)

### Phase 18: MVP 2 Strategie de Tests Complete (DONE)

#### Objectifs de Couverture — DONE
- [x] Domain/value objects: >95% (logique metier pure)
- [x] Application/use cases: >85% (orchestration avec ports mockes)
- [x] Adapters: >70% (verifies via tests d'integration)
- [x] Regles critiques (securite, dedup, SLA): >95%
- [x] Frontend services/guards/pipes: >85%
- [x] Frontend composants: >75%

#### Tests Backend Existants — Inventaire DONE
- [x] 863+ tests backend repartis sur 10 services
- [x] Tests unitaires domain: 389 tests (state machines, value objects, events)
- [x] Tests use case: 125 tests (orchestration avec mocks)
- [x] Tests REST (@WebMvcTest): 34 tests (controllers + DTO mapping)
- [x] Tests securite: 50 tests (RBAC, tenant isolation, rate limiting)
- [x] Tests ArchUnit: 87+ regles (hexagonal, naming, no cycles)
- [x] Tests integration: 178 tests (PostgreSQL, Kafka, Redis, PDF)

#### Tests Frontend Angular — Inventaire DONE
- [x] 132+ tests frontend (composants, services, guards, interceptors, pipes)
- [x] 12 fichiers composants .spec.ts (MetricCard, ConfirmDialog, StatusChip, SeverityBadge, etc.)
- [x] 5 fichiers services .spec.ts (ApiService, DashboardApi, AlertApi, etc.)
- [x] 4 fichiers interceptors .spec.ts (auth, tenant, correlation, error)
- [x] 3 fichiers guards .spec.ts (auth, role, unsaved-changes)
- [x] 2 fichiers pipes .spec.ts (risk-level, relative-time)
- [x] 6 fichiers E2E Cypress (dashboard, alerts, interventions, buildings, reports, settings)

#### Pipeline CI/CD — DONE
- [x] 7 jobs GitHub Actions: code-quality, build-and-test, integration-tests, coverage, security-scan, frontend-tests, docker-build
- [x] Concurrence: cancel-in-progress meme branche
- [x] Cache: Maven .m2 + npm node_modules
- [x] Artefacts: test results (7j), coverage (14j), OWASP reports (14j)
- [x] Frontend CI: ng test ChromeHeadless + code-coverage artifact

#### Regles Critiques (>95%) — DONE
- [x] Alert state machine (14 + 23 tests)
- [x] Intervention state machine + diagnostic required (15 + 6 tests)
- [x] CRITICAL alert → auto-intervention (5 workflow tests)
- [x] Concurrent deduplication 10 threads (1 ConcurrencyTest)
- [x] Channel routing policy severity → channels (6 tests)
- [x] Tenant isolation cross-tenant rejected (19 tests across 4 services)
- [x] Rate limiting enforcement (6 tests)
- [x] Download token single-use + expiry (7 tests)
- [x] Kafka idempotent consumer dedup (4 tests)
- [x] SLA breach detection (5 tests)
- [x] Quiet hours + critical override (15 tests)

#### Documentation — DONE
- [x] docs/mvp2-testing-strategy.md (strategie complete, inventaire, objectifs, CI, commandes)

### Phase 17: MVP 2 Kafka Event Integration (DONE)

#### Event Envelope Enrichment — DONE
- [x] IntegrationEvent enrichi: version, tenantId, correlationId, causationId, sourceService
- [x] Builder pattern pour construction fluide
- [x] Constructeur deprece 5-args pour backward compatibility
- [x] EventMetadata helper pour extraction metadata
- [x] IdempotentEventConsumer interface dans shared-kernel
- [x] Tests unitaires IntegrationEventTest (9 tests)

#### Topics Standardises — DONE
- [x] pyrosense.alerts.events (alerting-service output)
- [x] pyrosense.maintenance.events (maintenance-service output)
- [x] pyrosense.reports.events (reporting-service output)
- [x] pyrosense.notifications.events (notification-service output)
- [x] pyrosense.dead-letter.events (DLQ centralise)
- [x] Topics configures via application.yml avec variables d'environnement

#### Nouveaux Domain Events — DONE
- [x] NotificationSentEvent (notification.notification.sent)
- [x] NotificationFailedEvent (notification.notification.failed)
- [x] NotificationEventPublisherPort + KafkaNotificationEventPublisher

#### KafkaConfig Uniformise — DONE
- [x] Alerting: DLQ + ExponentialBackOff + idempotent producer + acks=all + observation
- [x] Maintenance: DLQ + ExponentialBackOff + idempotent producer + acks=all + observation
- [x] Reporting: idempotent producer + acks=all + observation
- [x] Notification: ExponentialBackOff (remplace FixedBackOff) + idempotent producer + observation

#### Publishers Enrichis — DONE
- [x] KafkaAlertEventPublisher: builder, eventId as key, correlationId/tenantId, structured logs
- [x] KafkaMaintenanceEventPublisher: builder, eventId as key, correlationId/tenantId, structured logs
- [x] KafkaReportEventPublisher: builder, eventId as key, correlationId/tenantId, structured logs
- [x] KafkaNotificationEventPublisher: nouveau, builder, output vers pyrosense.notifications.events

#### Consumers Idempotents — DONE
- [x] Maintenance KafkaAlertEventListener: dedup via recommendationRepository.findByAlertId
- [x] Notification service: DeduplicationPort avec window configurable (30 min)
- [x] IdempotentEventConsumer interface pour pattern reutilisable

#### Tests d'Integration Kafka — DONE
- [x] KafkaAlertEventPublisherIT (Testcontainers, verifie envelope enrichi + key)
- [x] KafkaMaintenanceEventPublisherIT (Testcontainers, verifie correlationId + defaults)
- [x] Dependances testcontainers ajoutees aux pom.xml alerting + maintenance

#### Documentation — DONE
- [x] docs/mvp2-events.md (topics, envelope, catalog, flows, config, observabilite)

### Phase 22: Audit Complet MVP 2 (DONE)

#### Vulnerabilites Corrigees — DONE
- [x] CRITICAL: Electricien voyait toutes les interventions du tenant (filtre par assignee ajoute dans list/kanban)
- [x] CRITICAL: Tenant spoofing via request body (tenantId retire des DTOs, TenantContext.require() partout)
- [x] HIGH: Notifications critiques desactivables par utilisateur (criticalOverrideEnabled force a true)
- [x] HIGH: RecommendationController accept/reject sans validation tenant (TenantId passe et verifie)

#### Verifications Strictes — PASS
- [x] Aucun controller ne depend d'un repository
- [x] Le domaine ne depend pas de Spring
- [x] Pas de logique metier dans Angular components
- [x] Pas de secret dans Git
- [x] Rapports proteges (download token single-use, 15min TTL)
- [x] Notifications critiques non desactivables (apres fix)
- [x] Interventions ne peuvent pas etre completees sans diagnostic
- [x] Electricien ne voit que ses interventions (apres fix)
- [x] Assureur ne voit que les rapports autorises

#### Tests — DONE (485 backend tests passent)
- [x] Maintenance: 99 tests, 0 failures
- [x] Reporting: 125 tests, 0 failures
- [x] Notification: 226 tests, 0 failures
- [x] Dashboard: 35 tests, 0 failures

#### Documentation — DONE
- [x] docs/audit-mvp2.md (findings, corrections, verifications, bilan)
- [x] docs/mvp2-production-readiness-checklist.md (criteres par etape)

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
| Dashboard | 8088 | local, docker, prod |
| Reporting | 8091 | local, docker, prod |
| Maintenance | 8089 | local, docker, prod |
| Frontend | 4200 | docker (nginx) |

---

## MVP 2 - Bilan

### Ce qui est termine (Phases 6-21)

| Domaine | Statut | Details |
|---------|:------:|---------|
| Dashboard Frontend (Angular 18) | FAIT | 12 ecrans, responsive, a11y, WebSocket temps reel |
| Dashboard Backend (query API) | FAIT | 6 endpoints, cache Redis, invalidation Kafka |
| Maintenance (interventions) | FAIT | Cycle de vie complet, workflow alerte→intervention, SLA, recommendations |
| Reporting (rapports PDF) | FAIT | 4 types, PDF professionnel, download securise, signature SHA-256 |
| Notifications (multi-canal) | FAIT | 5 canaux, 9 templates, preferences avancees, audit, anti-spam |
| Securite MVP 2 | FAIT | RBAC 8 roles, tenant isolation 4 services, rate limiting, anti mass-assignment |
| Kafka Integration | FAIT | 6 topics, envelope enrichi, DLQ, idempotence, Testcontainers IT |
| Tests | FAIT | 863+ backend, 132+ frontend, ArchUnit, security, E2E Cypress |
| Observabilite | FAIT | Metriques metier, tracing OTEL, logs JSON, 21 alertes Prometheus |
| Docker Compose | FAIT | Profil mvp2, Dockerfile par service, scripts, Grafana dashboard |
| Documentation | FAIT | 25+ documents, README enrichi, production readiness checklist |

### Ce qui reste (non bloquant pour le pilote terrain)

- [ ] Fix MEDIUM: Identity-service Flyway migrations (replace in-memory repos)
- [ ] Keycloak realm export + script init (bloquant avant pilote reel)
- [ ] ML-based anomaly detection service (NoOp adapter ready, prevu MVP 4 post-pilote)
- [ ] mTLS X.509 + ATECC608B (prevu MVP 4)
- [ ] OTA firmware securise (prevu MVP 4)
- [ ] Mobile push notifications (PWA, prevu MVP 4)
- [ ] Contract tests Spring Cloud Contract
- [ ] Load tests K6/Gatling (1000 devices, prevu MVP 4)
- [ ] Chaos testing

---

## Risques Techniques (MVP 3 — Pre-Pilote)

| # | Risque | Impact | Probabilite | Mitigation |
|---|--------|--------|:-----------:|-----------|
| 1 | Firmware instable sur hardware reel (ESP32-S3) | Pilote bloque | Moyenne | Test labo 24h obligatoire avant terrain (SC-04/SC-07) |
| 2 | Interferences WiFi en batiment technique | Perte donnees | Moyenne | Buffer offline 72h + LoRaWAN en backup (planifie) |
| 3 | Faux positifs detection > 5% | Perte confiance gestionnaire | Moyenne | Seuils ajustables + feedback loop + phase labo |
| 4 | Compromission physique device | Extraction credentials | Faible | Revocation immediate + rotation + mTLS planifie |
| 5 | Perte connectivite > 72h | Depassement buffer SPIFFS | Faible | Eviction FIFO + alerte offline Prometheus |
| 6 | Derive thermique capteur | Mesures incorrectes | Faible | Calibration terrain + compensation + self-diagnostic |
| 7 | Seuils detection calibres sur simulation uniquement | Detection inefficace terrain | Haute | Recalibration 3 mois donnees reelles obligatoire |
| 8 | PostgreSQL single-instance sans replicas | Perte donnees pilote | Moyenne | Migration managed DB avant extension 100 capteurs |
| 9 | Keycloak non configure (realm vide) | Pas d'auth en pilote | Haute | Script d'initialisation realm a creer (bloquant) |
| 10 | Pas de HA / pas de DR | Indisponibilite | Moyenne | Acceptable pour pilote 10 capteurs |
| 11 | MQTT TLS non valide end-to-end | Canal non chiffre | Moyenne | Valider TLS 8883 avant deploiement terrain |
| 12 | Absence OTA firmware | Impossibilite mise a jour terrain | Moyenne | Mise a jour manuelle USB pour pilote 10 devices |

### Items bloquants avant pilote terrain

- [ ] Firmware valide sur hardware ESP32-S3 pendant 24h (stabilite, memory, watchdog)
- [ ] Electricien habilite B2V minimum identifie et disponible
- [ ] MQTT TLS (port 8883) valide end-to-end avec certificat
- [ ] Keycloak realm 'pyrosense' configure (roles, client, utilisateurs pilote)

### Items recommandes (non bloquants)

- [ ] Backup LoRaWAN configure
- [ ] Assurance RC Pro a jour
- [ ] Load test 10 devices simultanes (validation charge)

---

## Proposition MVP 4

### Objectif : Industrialisation Post-Pilote + Intelligence

> Le MVP 3 est termine (documentation, firmware prototype, pipeline securise, CI/CD, observabilite).
> Le MVP 4 demarre apres validation terrain (3-6 mois de donnees pilote).

| Sprint | Duree | Livrable |
|--------|-------|---------|
| MVP 4.1 | 4 sem | Keycloak realm complet + script init + realm export |
| MVP 4.2 | 4 sem | ML v1: Isolation Forest + LSTM sur donnees terrain (MachineLearningInferencePort) |
| MVP 4.3 | 4 sem | mTLS X.509 + ATECC608B secure element + EMQX broker |
| MVP 4.4 | 4 sem | OTA firmware securise (A/B partitions, ECDSA, rollback auto) |
| MVP 4.5 | 4 sem | Application mobile PWA (notifications push, vue alertes, offline) |
| MVP 4.6 | 4 sem | Load testing K6 (100-1000 devices) + Infrastructure cloud (Terraform, K8s) |

### Pre-requis MVP 4

1. Pilote terrain 10 capteurs valide (3 mois minimum de donnees reelles)
2. Resultats labo 24h satisfaisants (firmware stable, no memory leak, no watchdog)
3. Faux positifs < 5% sur donnees terrain (seuils calibres)
4. Cloud provider selectionne (decision budgetaire)
5. Budget certification disponible (IEC 61439, CEM, thermique)

### Ce que MVP 4 couvre

- **Securite avancee** : mTLS, secure element, MQTT ACL per device, OTA signe
- **Intelligence** : ML sur 3+ mois de donnees reelles, feedback loop boucle fermee
- **Industrialisation** : PCB 4 couches, tests CEM/thermiques, dossier pre-certification
- **Scalabilite** : 100+ capteurs, Kubernetes multi-AZ, managed services
- **Mobile** : PWA notifications push, vue terrain electricien

### Ce que MVP 4 ne couvre PAS

- Certification electrique finale (IEC 61439, NF C 15-100) — organisme notifie
- Assurance RC Pro — processus juridique independant
- Commercialisation (tarification, support, SLA contractuels) — decision business
- Multi-region / DR (prevu production commerciale)

### Phase MVP 3.2: Real Device Ingestion Adaptation (pyrosense-ingestion-service) — DONE

#### Domain Model — DONE
- [x] IngestionRejectionReason enum (8 codes: INVALID_SIGNATURE, REPLAY_DETECTED, DEVICE_REVOKED, UNKNOWN_DEVICE, INVALID_SCHEMA_VERSION, PAYLOAD_TOO_OLD, INVALID_MEASUREMENT_RANGE, LOW_SIGNAL_QUALITY)
- [x] SignalQualityScore value object (5 levels: EXCELLENT, GOOD, DEGRADED, POOR, CRITICAL, with thresholds)
- [x] DeviceClockDrift value object (4 severities: NONE, MINOR, MODERATE, SEVERE)

#### Domain Events — DONE
- [x] RealDeviceTelemetryReceivedEvent (ingestion.real_device.telemetry.received)
- [x] TelemetryRejectedEvent (ingestion.telemetry.rejected)
- [x] DeviceClockDriftDetectedEvent (ingestion.device.clock_drift_detected)
- [x] LowSignalQualityDetectedEvent (ingestion.device.low_signal_quality)

#### Domain Validation — DONE
- [x] TelemetryQualityValidator (signal quality, noise floor, HF noise, sampling window)
- [x] DeviceClockDriftDetector (device vs server timestamp comparison)

#### Ports Out — DONE
- [x] DeviceAuthenticationPort (device status, HMAC key retrieval, firmware tracking)
- [x] DeviceSignatureVerificationPort (HMAC-SHA256 verification)
- [x] DeviceCapabilityLookupPort (device capabilities, real vs simulator detection)

#### Adapters Out — DONE
- [x] HttpDeviceAuthenticationAdapter (device-service REST client, cached, with metrics)
- [x] HttpDeviceCapabilityAdapter (device-service REST client, cached)
- [x] RedisAntiReplayStores (NonceStore, SequenceStore, MessageIdStore — Redis-backed)
- [x] DeviceStatusCheckerAdapter (bridges DeviceAuthenticationPort to ProtocolValidationPipeline)

#### MQTT Listener Upgrade — DONE
- [x] V1 topic subscription: pyrosense/v1/+/+/telemetry|heartbeat|events
- [x] Legacy topic subscription (simulator backward compat): pyrosense/+/+/telemetry|heartbeat|events
- [x] Full ProtocolValidationPipeline integration on v1 path
- [x] Quality assessment (TelemetryQualityValidator) on every v1 message
- [x] Clock drift detection (DeviceClockDriftDetector) on every v1 message
- [x] Rejection handling: store + event + specific metric counters

#### Metrics — DONE
- [x] real_device_telemetry_received_total (v1 ingestions)
- [x] telemetry_signature_invalid_total (HMAC failures)
- [x] telemetry_replay_detected_total (replay attacks)
- [x] low_signal_quality_total (poor/critical signal)
- [x] device_clock_drift_total (moderate/severe drift)

#### REST API — DONE
- [x] GET /api/v1/devices/quality/summary (aggregated quality metrics)
- [x] GET /api/v1/devices/quality/devices/{deviceId} (per-device quality)
- [x] @PreAuthorize: PLATFORM_ADMIN, TENANT_ADMIN, DEVICE_MANAGER

#### Configuration — DONE
- [x] ProtocolValidationConfig (wires pipeline beans)
- [x] RedisAntiReplayStores (nonce TTL 24h, messageId TTL 24h)

#### Tests — DONE
- [x] SignalQualityScoreTest (10 tests: levels, boundaries, alerts, minimums)
- [x] DeviceClockDriftTest (5 tests: none, minor, moderate, severe, future)
- [x] IngestionRejectionReasonTest (3 tests: mappings, unknown codes, descriptions)
- [x] TelemetryQualityValidatorTest (7 tests: good, low signal, noise, floor, window, multiple)
- [x] DeviceClockDriftDetectorTest (2 tests: explicit timestamp, current time)
- [x] RealDeviceIngestionTest (7 tests: valid accepted, schema rejected, mismatch, revoked, duplicate, sequence regression, reason mapping)

#### Documentation — DONE
- [x] docs/real-device-ingestion.md (architecture, topics, validation pipeline, rejection handling, quality, drift, events, metrics, REST, ports, security, DB, config)

### Phase MVP 3.3: Data Quality Strategy (pyrosense-ingestion-service) — DONE

#### Domain Model — DONE
- [x] DataQualityAssessment aggregate (QualityGrade A-F, composite scoring 0-100, 9 criteria, auto-issue detection)
- [x] DataQualityIssue entity (10 IssueTypes, 4 severities, OPEN/REVIEWED/DISMISSED lifecycle)
- [x] MissingDataRatio value object (compute, isAcceptable/isWarning/isCritical)
- [x] MeasurementRangeStatus value object (compute, hasIssues, isCritical)
- [x] SensorNoiseLevel value object (HF noise, current/voltage stddev, isExcessive)
- [x] DataCompleteness value object (heartbeat + telemetry completeness, sequence issues)
- [x] DeviceCalibrationStatus value object (unknown, calibrated, isExpired, isValid)
- [x] SignalQualityLevel enum (5 levels, fromScore, isTrustworthy, requiresReview)

#### Domain Events — DONE
- [x] DataQualityIssueDetectedEvent (published for HIGH/CRITICAL issues)
- [x] DeviceDataQualityScoreUpdatedEvent (published on every assessment)

#### Ports In (Use Cases) — DONE
- [x] AssessTelemetryQualityUseCase (AssessCommand → DataQualityAssessment)
- [x] ComputeDailyDeviceDataQualityUseCase (computeForDevice, computeForAllDevices)
- [x] ListDataQualityIssuesQuery (IssueFilter, IssuePageResult, list, getById)
- [x] MarkDataQualityIssueReviewedUseCase (ReviewCommand → DataQualityIssue)

#### Ports Out — DONE
- [x] DataQualityRepositoryPort (saveAssessment, findLatest, findHistory, CRUD issues)
- [x] DeviceTelemetryStatsPort (TelemetryStats, countExpected/Received messages/heartbeats)

#### Use Case Implementations — DONE
- [x] AssessTelemetryQualityService (orchestrates stats, builds assessment, persists, publishes events)
- [x] ComputeDailyDeviceDataQualityService (24h window computation)
- [x] ListDataQualityIssuesService (paginated query with tenant isolation)
- [x] MarkDataQualityIssueReviewedService (review/dismiss with tenant validation)

#### REST API — DONE
- [x] GET /api/v1/devices/{deviceId}/data-quality/latest
- [x] GET /api/v1/devices/{deviceId}/data-quality/history?from=&to=
- [x] GET /api/v1/data-quality/issues?deviceId=&status=&type=&page=&size=
- [x] PATCH /api/v1/data-quality/issues/{id}/review
- [x] @PreAuthorize: PLATFORM_ADMIN, TENANT_ADMIN, DEVICE_MANAGER, PROPERTY_MANAGER

#### Persistence — DONE
- [x] JdbcDataQualityRepository (full CRUD, row mappers)
- [x] JdbcDeviceTelemetryStatsAdapter (SQL stats queries on electrical_telemetry + device_heartbeats)
- [x] Flyway V007: data_quality_assessments + data_quality_issues tables with indexes

#### Business Rules — DONE
- [x] Grade F blocks critical alerts (allowsCriticalAlerts = false)
- [x] Grade D+ excludes ML training (allowsMlTraining = false for C/D/F)
- [x] Grade A-B allows full trust (isTrustworthy = true)
- [x] Issues HIGH/CRITICAL publish events for notification
- [x] Tenant isolation enforced on all queries

#### Configuration — DONE
- [x] UseCaseConfig: 4 new beans wired (assess, daily, list, review)

#### Tests — DONE
- [x] DataQualityAssessmentTest (27 tests: out of range, timestamps, sequences, missing data, signal, offline, grading, noise, firmware/calibration)
- [x] AssessTelemetryQualityServiceTest (10 tests: healthy device, out of range, timestamps, sequences, missing data, signal, offline, events, persistence)

#### Documentation — DONE
- [x] docs/data-quality.md (model, scoring algorithm, criteria, API, events, rules, tests, config)

### Phase MVP 3.4: Field Data Collection & ML Dataset (pyrosense-ingestion-service) — DONE

#### Domain Model — DONE
- [x] DatasetCandidate entity (status lifecycle: PENDING_LABEL → LABELED → VALIDATED → EXPORTED / REJECTED, quality tiers, exportability rules)
- [x] DataLabel record (9-value taxonomy, 4 sources: TECHNICIAN/LAB/SYSTEM/MANUAL_REVIEW, confidence scoring)
- [x] FieldObservation entity (pseudonymized, linked to intervention, with label)
- [x] TechnicianFeedback record (intervention-linked, defect/false-positive, measurement details, toLabel())
- [x] DefectConfirmation record (intervention+alert linked, risk reduction tracking, toLabel())
- [x] FalsePositiveFeedback record (alert-linked, threshold adjustment suggestions, toLabel())
- [x] DatasetExportJob entity (PENDING → RUNNING → COMPLETED/FAILED, format, filters)
- [x] FeatureSummary record (14 aggregated features: RMS, THD, HF noise, temperature, power factor, signal quality)
- [x] PseudonymizationService (HMAC-SHA256, deterministic, one-way, configurable key)

#### Label Taxonomy (9 values) — DONE
- [x] NORMAL, MICRO_ARC_SUSPECTED, MICRO_ARC_CONFIRMED, LOOSE_CONNECTION_CONFIRMED
- [x] INSULATION_DEGRADATION_CONFIRMED, OVERLOAD_CONFIRMED, SENSOR_NOISE, FALSE_POSITIVE, INCONCLUSIVE

#### Ports In (Use Cases) — DONE
- [x] SubmitFieldFeedbackUseCase (FieldFeedbackCommand → TechnicianFeedback)
- [x] QueryDatasetCandidatesUseCase (CandidateFilter → CandidatePageResult)
- [x] RequestDatasetExportUseCase (ExportCommand → DatasetExportJob, getExportJob)

#### Ports Out — DONE
- [x] DatasetRepositoryPort (full CRUD: candidates, feedbacks, observations, confirmations, false positives, export jobs)
- [x] DatasetAuditPort (logFeedbackSubmitted, logExportRequested, logExportCompleted, logCandidateLabeled)

#### Use Case Implementations — DONE
- [x] SubmitFieldFeedbackService (pseudonymizes device, persists, audits)
- [x] QueryDatasetCandidatesService (pseudonymizes tenant, paginated query)
- [x] RequestDatasetExportService (pseudonymizes tenant, creates job, audits)

#### REST API — DONE
- [x] POST /api/v1/field-feedback (PLATFORM_ADMIN, TENANT_ADMIN, ELECTRICIAN, DEVICE_MANAGER)
- [x] GET /api/v1/dataset/candidates (PLATFORM_ADMIN, TENANT_ADMIN)
- [x] POST /api/v1/dataset/export-jobs (PLATFORM_ADMIN, TENANT_ADMIN)
- [x] GET /api/v1/dataset/export-jobs/{id} (PLATFORM_ADMIN, TENANT_ADMIN)

#### Persistence — DONE
- [x] JdbcDatasetRepository (full implementation, 7 tables)
- [x] LoggingDatasetAuditAdapter (structured audit logging)
- [x] Flyway V008: dataset_candidates, technician_feedbacks, field_observations, defect_confirmations, false_positive_feedbacks, dataset_export_jobs, data_labels

#### Privacy & Security — DONE
- [x] Pseudonymization HMAC-SHA256 at write time (device + tenant)
- [x] No raw electrical data in dataset tables
- [x] All exports audited
- [x] Tenant isolation via pseudonymized tenant filter
- [x] Labels preserve source traceability
- [x] Feedback linked to intervention (mandatory)
- [x] False positives feed threshold improvement

#### Configuration — DONE
- [x] UseCaseConfig: PseudonymizationService + 3 new use case beans
- [x] pyrosense.dataset.pseudonymization-secret (configurable, default for dev)

#### Tests — DONE (24 tests, 0 failures)
- [x] DatasetCandidateTest (8 tests: lifecycle, quality tiers, exportability, labels, primary label, export, reject)
- [x] DataLabelTest (5 tests: sources, isConfirmed, isHighConfidence)
- [x] PseudonymizationServiceTest (7 tests: deterministic, different inputs, prefix, different keys, device/tenant methods, null)
- [x] SubmitFieldFeedbackServiceTest (4 tests: pseudonymization, audit, false positive label, defect label)

#### Documentation — DONE
- [x] docs/field-data-collection.md (architecture, pipeline, models, API, pseudonymisation, controles qualite, privacy)
- [x] docs/dataset-governance.md (principes, roles, cycle de vie, regles, controles, RGPD, environnement labo)
- [x] docs/dataset-format-v1.md (schema, colonnes, formats, exemples, versioning, securite, limitations)

### Phase MVP 3.5: Field Feedback Loop (pyrosense-risk-scoring-service) — DONE

#### Domain Model — DONE
- [x] FeedbackOutcome enum (CONFIRMED_DEFECT, FALSE_POSITIVE, INCONCLUSIVE, NO_DEFECT_FOUND)
- [x] ScoringFeedback entity (device, alert, intervention, outcome, anomaly type, risk score at alert, source, comment)
- [x] ScoringAdjustment record (AdjustmentType, AdjustmentMode, factory methods: confidenceBoost/confidenceReduction)
- [x] FeedbackConfidenceEngine (pure domain logic: conservative decay-based adjustments)

#### Confidence Adjustment Rules — DONE
- [x] CONFIRMED_DEFECT: +0.05 × 0.8^n, max cumulative +0.20
- [x] FALSE_POSITIVE: -0.03 × 0.8^n, max cumulative -0.15
- [x] NO_DEFECT_FOUND: -0.01 (fixed, if confidence > 0.2)
- [x] INCONCLUSIVE: no adjustment
- [x] All adjustments in SUGGESTION_ONLY mode (no auto-apply)
- [x] Bounds: confidence ∈ [0.0, 1.0], stop boost at 0.99, stop reduction at 0.1

#### Ports In (Use Cases) — DONE
- [x] ProcessFieldFeedbackUseCase (FieldFeedbackCommand → ScoringFeedback)
- [x] GetFeedbackHistoryQuery (FeedbackFilter, FeedbackPageResult, FeedbackStats, getHistory, getStats, getAdjustmentHistory)

#### Ports Out — DONE
- [x] ScoringFeedbackRepositoryPort (saveFeedback, saveAdjustment, findByTenantAndDevice, countByOutcome, findAdjustments, countByOutcomeForTenant)

#### Use Case Implementations — DONE
- [x] ProcessFieldFeedbackService (save feedback → count outcomes → compute adjustment → save adjustment → metrics)
- [x] GetFeedbackHistoryService (paginated history, stats aggregation, adjustment history)

#### Kafka Consumer — DONE
- [x] KafkaFeedbackEventListener (maintenance-events topic)
- [x] Maps: maintenance.intervention.completed, maintenance.defect.confirmed, maintenance.false_positive.confirmed
- [x] Result field mapping: DEFECT_CONFIRMED/ELECTRICAL_DEFECT → CONFIRMED_DEFECT, FALSE_POSITIVE → FALSE_POSITIVE, etc.

#### REST API — DONE
- [x] GET /api/v1/scoring/feedback/history (paginated, filters: tenantId, deviceId, anomalyType, from, to)
- [x] GET /api/v1/scoring/feedback/stats (confirmed, false positives, inconclusive, adjustments counts)
- [x] GET /api/v1/scoring/feedback/adjustments (per device, optional anomaly type filter)
- [x] @PreAuthorize: TENANT_ADMIN, PROPERTY_MANAGER, ELECTRICIAN, OPERATOR

#### Persistence — DONE
- [x] JdbcScoringFeedbackRepository (full implementation, dynamic queries)
- [x] Flyway V002: scoring_feedbacks + scoring_adjustments tables with indexes

#### Metrics — DONE
- [x] pyrosense.scoring.confirmed_defects_total
- [x] pyrosense.scoring.false_positives_total
- [x] pyrosense.scoring.inconclusive_feedback_total
- [x] pyrosense.scoring.scoring_adjustments_total

#### Configuration — DONE
- [x] UseCaseConfig: ProcessFieldFeedbackService + GetFeedbackHistoryService beans

#### Tests — DONE
- [x] FeedbackConfidenceEngineTest (14 tests: boost, decay, max cumulative, bounds, reduction, no-defect, inconclusive, mode, properties)
- [x] ProcessFieldFeedbackServiceTest (6 tests: save, adjustment computation, inconclusive no-adj, metrics, decay)

#### Documentation — DONE
- [x] docs/feedback-loop.md (architecture, events, rules, API, metrics, schema, labels ML, constraints)

### Phase MVP 3.6: Lab Test Protocol — DONE

#### Documentation — DONE
- [x] docs/lab-test-protocol.md (protocole complet : 13 sections, 14 scenarios, criteres acceptation, securite, roles, recommandations)
- [x] docs/lab-test-report-template.md (modele rapport : infos generales, resultats par scenario, mesures, anomalies, conclusion, approbation)
- [x] docs/lab-defect-report-template.md (modele fiche anomalie : identification, classification, reproduction, preuves, analyse, resolution, historique)

#### Scenarios de Test (14) — DONE
- [x] SC-01 : Demarrage device (cold boot, sequence init, 1er heartbeat)
- [x] SC-02 : Provisioning securise (claim token, single-use, rate limit)
- [x] SC-03 : Heartbeat periodique (regularite, contenu, RSSI)
- [x] SC-04 : Telemetrie normale (precision RMS ±5%, THD, temperature ±2°C)
- [x] SC-05 : Perte WiFi (detection < 10s, transition offline)
- [x] SC-06 : Reconnexion (< 30s, drain buffer, FIFO, pas de duplication)
- [x] SC-07 : Buffer offline stress (1h+ sans perte, eviction FIFO, integrite)
- [x] SC-08 : Donnees hors plage (saturation ADC, temp extremes, flags)
- [x] SC-09 : Signal bruite (SNR 30→6 dB, signalQuality degrade, events)
- [x] SC-10 : Temperature elevee simulee (seuils WARNING/HIGH, anomalie, retour)
- [x] SC-11 : Micro-arc simule (bursts HF basse tension, arcEnergy, detection > 80%)
- [x] SC-12 : Transitoires simules (impulsions carrees, transientCount, anomalie)
- [x] SC-13 : Firmware version mismatch (forward compat, tracking, rejet ancien)
- [x] SC-14 : Credential revoked (revocation effective, pas de boucle retry, re-provisioning)

#### Criteres Non-Passage Terrain (10) — DONE
- [x] NP-01 a NP-10 definis (crash, precision, buffer, provisioning, faux positifs, revocation, reconnexion, memory leak, watchdog, detection)

#### Recommandations — DONE
- [x] 10 recommandations firmware (watchdog HW, self-test, OTA, LZ4, degraded mode, etc.)
- [x] 10 recommandations backend (alertes, dashboard health, commandes device, etc.)
- [x] 5 recommandations integration (automatisation scenarios, CI firmware, E2E, signaux reference)

### Phase MVP 3.7: Field Pilot Plan (10 Capteurs) — DONE

#### Plan Pilote — DONE
- [x] docs/field-pilot-10-devices.md (plan complet : 25 sections, objectifs, perimetre, selection sites/tableaux, procedures, KPIs, risques, criteres succes/arret)

#### Sections couvertes (25) — DONE
- [x] Objectifs du pilote (principal + secondaires + disclaimers)
- [x] Perimetre (10 devices, 2-3 sites, 3-6 mois, fonctionnalites actives)
- [x] Hors perimetre (ML avance, certification, notifications push, HA, OTA auto, SLA)
- [x] Criteres selection sites (7 obligatoires, 6 souhaitables, 5 exclusions)
- [x] Criteres selection tableaux electriques (7 techniques, circuits a prioriser)
- [x] Pre-visite technique (10 points, livrable fiche site GO/NO-GO)
- [x] Checklist installation (resume, reference vers doc detaillee)
- [x] Checklist reseau (10 verifications, actions si NOK)
- [x] Checklist securite (12 verifications, consignation, EPI, habilitation)
- [x] Plan provisioning (workflow, registre devices, securite credentials)
- [x] Plan collecte donnees (types, volumes, features, labellisation, RGPD)
- [x] Plan monitoring quotidien (horaires, alertes auto, reference checklist)
- [x] Plan support (4 niveaux L0-L3, canaux, escalade, astreinte)
- [x] Procedure alerte WARNING (8 etapes, regles anti-fatigue alertes)
- [x] Procedure alerte CRITICAL (11 etapes, template communication gestionnaire)
- [x] Procedure capteur offline (paliers 3min→72h, causes probables, regles)
- [x] Procedure faux positif (7 etapes, analyse, objectifs FP mensuel)
- [x] Procedure retrait capteur (12 etapes, engagement retrait sans condition)
- [x] KPIs (7 techniques, 5 detection, 5 operationnels, reporting)
- [x] Duree recommandee (3 mois min, 6 mois recommande, phases)
- [x] Risques (12 risques identifies avec probabilite/impact)
- [x] Mitigations (12 mitigations associees)
- [x] Criteres de succes (8 techniques, 5 operationnels, 3 business)
- [x] Criteres d'arret (3 immediats securite, 6 programmes echec)
- [x] Rapport final pilote (14 sections, diffusion, reference template)

#### Documents Associes — DONE
- [x] docs/field-installation-checklist.md (8 phases, etiquette capteur, PV signature)
- [x] docs/pilot-daily-monitoring-checklist.md (7 sections, tableau 10 devices, metriques, bilan)
- [x] docs/pilot-final-report-template.md (14 sections, KPIs, detection, dataset ML, decision GO/NO-GO)

#### Securite et Conformite — DONE
- [x] Installation exclusivement par electricien habilite (B2V min.)
- [x] Consignation obligatoire (VAT, condamnation)
- [x] Disclaimer obligatoire (pas de securite incendie certifiee)
- [x] Retrait sans condition sur demande gestionnaire
- [x] RGPD : pseudonymisation, droit de retrait, pas de transmission tiers
- [x] Pas de promesse commerciale (pilote = experimentation controlee)

### Phase MVP 3.8: Pilot Backend (pyrosense-dashboard-service) — DONE

#### Domain Model — DONE
- [x] PilotProgram aggregate (state machine: PREPARING → ACTIVE → PAUSED → COMPLETED/CANCELLED)
- [x] PilotStatus enum (isTerminal, isRunning)
- [x] PilotDevice record (status lifecycle: PLANNED → INSTALLED → ACTIVE → OFFLINE → REMOVED)
- [x] PilotSite record (name, address, contact)
- [x] PilotObservation record (5 types: FIELD_NOTE, INSTALLATION_REPORT, DAILY_MONITORING, FEEDBACK, GENERAL)
- [x] PilotIncident record (4 severities, 8 categories, 4 statuses, report/resolve)
- [x] PilotKpiSnapshot record (16 metrics, computed falsePositiveRate)

#### Ports & Use Cases — DONE
- [x] ManagePilotUseCase (9 methods, 4 command records)
- [x] PilotRepositoryPort (15 methods: CRUD all 6 entities)
- [x] ManagePilotService (tenant isolation, KPI computation from devices+incidents)

#### REST API (8 endpoints) — DONE
- [x] POST /api/v1/pilots (create)
- [x] GET /api/v1/pilots (list by tenant)
- [x] GET /api/v1/pilots/{id} (detail with devices)
- [x] PATCH /api/v1/pilots/{id}/status (state transitions)
- [x] POST /api/v1/pilots/{id}/devices (add device)
- [x] POST /api/v1/pilots/{id}/observations (add observation)
- [x] POST /api/v1/pilots/{id}/incidents (report incident)
- [x] GET /api/v1/pilots/{id}/kpis (KPI history)
- [x] POST /api/v1/pilots/{id}/reports (compute current KPIs)
- [x] @PreAuthorize: TENANT_ADMIN, PROPERTY_MANAGER, PLATFORM_ADMIN

#### Persistence — DONE
- [x] JdbcPilotRepository (full JDBC, row mappers for all 6 entities)
- [x] Flyway V002: 6 tables (pilot_programs, pilot_sites, pilot_devices, pilot_observations, pilot_incidents, pilot_kpi_snapshots)

#### Configuration — DONE
- [x] UseCaseConfig: ManagePilotService bean wired

#### Tests — DONE
- [x] PilotProgramTest (13 tests: state machine transitions, validation, device count)
- [x] ManagePilotServiceTest (9 tests: create, get, tenant isolation, status, devices, incidents, KPIs)
- [x] PilotControllerTest (8 tests: REST endpoints, responses, 401)
- [x] PilotSecurityTest (9 tests: RBAC roles, 401/403 for unauthorized roles)

#### Documentation — DONE
- [x] docs/pilot-backend.md (architecture, domain, endpoints, security, schema, tests, rules)

### Phase MVP 3.9: Device Technical Dashboard — DONE

#### Backend (pyrosense-dashboard-service) — DONE
- [x] DeviceTechnicalHealth record (16 champs: identification, metriques sante, quality, clock drift, sequence gaps)
- [x] TelemetryQualityReport record (stats horaires, rejets, signal trend, offline periods)
- [x] DeviceSecurityStatus record (credential status, version, auth failures, replay blocks — jamais de secrets)
- [x] PilotDashboard record (vue consolidee: devices, incidents summary, KPI snapshot)
- [x] DeviceTechnicalQuery port in (4 methodes query)
- [x] DeviceTechnicalReadModelPort port out (4 methodes read model)
- [x] GetDeviceTechnicalService (delegation + tenant isolation)
- [x] DeviceTechnicalController (4 endpoints, @PreAuthorize PLATFORM_ADMIN/TENANT_ADMIN/PROPERTY_MANAGER/SUPPORT_READONLY)
- [x] StubDeviceTechnicalReadModel (donnees simulees MVP)
- [x] UseCaseConfig: GetDeviceTechnicalService bean wire

#### Backend Tests — DONE
- [x] DeviceTechnicalControllerTest (endpoints, responses, 401)
- [x] DeviceTechnicalSecurityTest (RBAC: roles autorises, 403 electrician/occupant)

#### Frontend (pyrosense-dashboard Angular) — DONE
- [x] device-technical.model.ts (interfaces TypeScript)
- [x] DeviceTechnicalApiService (HTTP client, 4 methodes)
- [x] DeviceTechnicalOverviewComponent (cartes KPI, identification, seuils couleur)
- [x] DeviceTelemetryQualityComponent (charts Chart.js, tables rejets/offline)
- [x] DeviceSecurityComponent (credential badge, compteurs, banniere revoked)
- [x] PilotMonitoringComponent (KPI cards, device table, incidents summary)
- [x] Routes: devices/:id/technical, telemetry-quality, security + pilots/:id/monitoring
- [x] roleGuard: PLATFORM_ADMIN, TENANT_ADMIN, PROPERTY_MANAGER, SUPPORT_READONLY

#### Frontend Tests — DONE
- [x] device-technical-api.service.spec.ts (appels HTTP)
- [x] device-technical-overview.component.spec.ts (creation, loading, affichage)
- [x] pilot-monitoring.component.spec.ts (creation, rendu)

#### Documentation — DONE
- [x] docs/device-technical-dashboard.md (architecture, endpoints, ecrans, securite, modeles, seuils, limitations)

### Phase MVP 3.10: Observability Real Device — DONE

#### Metrics (14) — DONE
- [x] Mvp3ObservabilityConfig: Counters (6): real_device_telemetry_received, signature_invalid, replay_detected, low_signal_quality, clock_drift, offline_queue_flush
- [x] Mvp3ObservabilityConfig: Gauges (7): device_offline_duration_seconds, signal_quality_score, data_quality_score, firmware_version_count, pilot_active_devices, pilot_data_quality_average, pilot_incidents_total
- [x] Mvp3ObservabilityConfig: Timer (1): validation_duration_seconds (P50, P95, P99)
- [x] TracingConfig: ObservedAspect for @Observed annotation support

#### Prometheus Alerts (10 rules, group pyrosense-mvp3) — DONE
- [x] Mvp3SignatureInvalidSpike (critical): rate > 0.5/s for 2m
- [x] Mvp3ReplayAttackDetected (critical): increase > 3 in 5m
- [x] Mvp3DeviceOfflineLong (warning): offline > 1h for 5m
- [x] Mvp3LowSignalQualityHigh (warning): rate > 0.3/s for 5m
- [x] Mvp3ClockDriftFrequent (warning): rate > 0.2/s for 10m
- [x] Mvp3NoRealDeviceData (critical): rate = 0 for 15m
- [x] Mvp3ValidationLatencyHigh (warning): P95 > 500ms for 3m
- [x] Mvp3DataQualityLow (warning): score < 60 for 10m
- [x] Mvp3PilotDevicesInactive (warning): active < 5 for 10m
- [x] Mvp3FirmwareVersionDrift (warning): versions > 3 for 30m

#### Structured Logging — DONE
- [x] Logback MDC fields: traceId, spanId, correlationId, tenantId, deviceId, firmwareVersion, schemaVersion, rejectionReason
- [x] No payload content in logs (security constraint)
- [x] No device secrets in logs (security constraint)
- [x] JSON structured output in docker/prod profiles (LogstashEncoder)

#### Distributed Tracing — DONE
- [x] OTLP HTTP export (localhost:4318/v1/traces)
- [x] W3C traceparent propagation (HTTP + Kafka)
- [x] Configurable sampling probability (TRACING_SAMPLING env var)
- [x] ObservedAspect for method-level span creation

#### Grafana Dashboards (4) — DONE
- [x] mvp3-device-fleet-health.json (telemetry rate, signal/data quality gauges, offline, firmware)
- [x] mvp3-pilot-monitoring.json (active devices, quality averages, incidents, security timeline)
- [x] mvp3-ingestion-security.json (signatures, replay, validation latency, rejection breakdown)
- [x] mvp3-data-quality.json (quality scores, rejections, offline events, drift)

#### Tests — DONE
- [x] Mvp3ObservabilityConfigTest (13 tests: registration, increment, gauges, timer, labels, no secrets)

#### Documentation — DONE
- [x] docs/mvp3-observability.md (metrics table, alerts, logging, tracing flow, dashboards, runbook)

### Phase MVP 3.11: IoT Security Hardening — DONE

#### Threat Model STRIDE — DONE
- [x] 13 menaces identifiees (Spoofing, Tampering, Repudiation, Info Disclosure, DoS, Elevation)
- [x] Matrice des risques (Impact × Likelihood → Risk Level)
- [x] Classement: 1 CRITICAL (#6 tenant spoofing), 8 HIGH, 4 MEDIUM

#### Security Controls Implementation — DONE
- [x] DeviceRateLimiter: per-device sliding window (configurable max-messages/window-seconds)
- [x] IoTSecurityAuditor: structured audit logging for 12 security event types
- [x] ProtocolValidationPipeline enhanced: rate limiting (step 0) + audit events on all rejections
- [x] ProtocolValidationConfig: wires DeviceRateLimiter + IoTSecurityAuditor beans
- [x] Configuration: pyrosense.security.device-rate-limit.max-messages (default 120)
- [x] Configuration: pyrosense.security.device-rate-limit.window-seconds (default 60)

#### Existing Controls Documented — DONE
- [x] HMAC-SHA256 per-device signature (SignatureVerifier, constant-time comparison)
- [x] Anti-replay 3 layers: nonce uniqueness, messageId idempotency, sequence monotonicity
- [x] Timestamp tolerance: ±300s normal, 72h drain mode
- [x] Device status check: ACTIVE/REVOKED/NOT_FOUND before signature verification
- [x] Topic-payload coherence: topicDeviceId == payload.deviceId, topicTenantId == payload.tenantId
- [x] Payload size limit: 8KB MQTT + PayloadSizeLimitFilter HTTP
- [x] Schema validation: version whitelist, feature range checks, nonce length, required fields
- [x] Provisioning: claim token single-use, 24h TTL, rate limited (10 failures/IP/15min)
- [x] Credential lifecycle: issue, rotate, revoke — hash-stored, never re-displayed
- [x] DLQ: failed messages routed to ingestion-dlq Kafka topic

#### IoT Security Tests (IoTSecurityTest.java — 24 tests) — DONE
- [x] Threat #1: Unknown device rejected, wrong tenant rejected
- [x] Threat #3: Nonce reuse rejected, duplicate messageId rejected, sequence regression rejected
- [x] Threat #4: Invalid HMAC rejected, missing signature rejected
- [x] Threat #5: Revoked device rejected immediately
- [x] Threat #6: Topic tenant mismatch rejected, topic device mismatch rejected
- [x] Threat #8: Firmware version tracked on successful validation
- [x] Threat #9: Future timestamp rejected, old timestamp rejected, drain mode accepted
- [x] Threat #11: Invalid schema version rejected, out-of-range values rejected, bad nonce rejected
- [x] Threat #12: Rate limit enforced, independent limits per device
- [x] Threat #10: Audit sanitizes secrets in detail messages
- [x] Signature: valid passes, tampered fails, null key disables, constant-time comparison

#### DeviceRateLimiterTest (6 tests) — DONE
- [x] First message allowed, within limit allowed, exceeding limit rejected
- [x] Independent devices, reset clears window, zero-limit rejects all

#### Documentation — DONE
- [x] docs/iot-security.md (STRIDE model, risk matrix, 13 controls, mTLS migration path, pre-pilot checklist)

#### Pre-Pilot Security Checklist — DONE
- [x] Device commissioning checklist (5 items)
- [x] Cloud infrastructure checklist (6 items)
- [x] Credential management checklist (5 items)
- [x] Monitoring & audit checklist (4 items)
- [x] Network security checklist (4 items)
- [x] Incident response procedures (4 scenarios)

#### Migration Future (mTLS) — Documented
- [x] Phase 1 (current): HMAC-SHA256
- [x] Phase 2: Dual validation HMAC + X.509
- [x] Phase 3: ATECC608B secure element (key non-extractable)
- [x] Phase 4: Pure mTLS with broker ACL

### Phase MVP 3.12: CI/CD Pipeline Complet — DONE

#### GitHub Actions Workflow (.github/workflows/mvp3-ci.yml) — DONE
- [x] Trigger: push main/develop/release/firmware, PR, workflow_dispatch
- [x] Concurrency: cancel-in-progress par branche
- [x] 7 jobs séparés avec dépendances

#### Job 1: backend-build-test — DONE
- [x] Compile (Maven, Java 21)
- [x] Unit tests (~500+ tests, 12 modules)
- [x] ArchUnit tests (règles hexagonales)
- [x] Integration tests (JDBC, Kafka embedded)
- [x] JaCoCo coverage (seuil 60% warning)
- [x] Artifacts: test reports + coverage reports

#### Job 2: frontend-build-test — DONE
- [x] Conditional: skip si pyrosense-dashboard/ non modifié (sauf main)
- [x] npm ci (dépendances lockfile exactes)
- [x] ESLint (ng lint)
- [x] Karma/Jasmine tests (ChromeHeadless)
- [x] Production build (AOT, tree-shaking)
- [x] Artifact: frontend-coverage

#### Job 3: firmware-build — DONE
- [x] Install cmake + g++ + cppcheck
- [x] Build host tests (CMake, C++17, -DPYRO_HOST_BUILD)
- [x] CTest execution (7 suites, 43 tests)
- [x] cppcheck static analysis (fail on error)
- [x] Firmware version generation (0.3.YYYYMMDD.sha7)
- [x] Artifacts: firmware-test-results, firmware-{version}

#### Job 4: security-scan — DONE
- [x] OWASP Dependency Check (fail on CVSS >= 8)
- [x] Hardcoded secrets grep scan
- [x] Artifact: owasp-report

#### Job 5: docker-build — DONE
- [x] Trigger: main ou release/* uniquement
- [x] Build JARs (skip tests)
- [x] Docker Buildx
- [x] Push to registry (credentials via secrets)
- [x] Version tag: 0.3.0-YYYYMMDD-sha7

#### Job 6: release-notes — DONE
- [x] Trigger: release/* branches
- [x] Changelog automatique (git log)
- [x] Matrice versions (backend, frontend, firmware, schema)
- [x] Statut sécurité
- [x] Checklist pré-pilote

#### Job 7: deploy-pilot — DONE
- [x] Trigger: workflow_dispatch UNIQUEMENT (jamais automatique)
- [x] GitHub Environment 'pilot' avec reviewers requis (manual approval)
- [x] Vérification checklist
- [x] Deploy backend + health check
- [x] Notification succès

#### Versioning — DONE
- [x] Backend: Maven 0.3.0-SNAPSHOT → Docker tag 0.3.0-YYYYMMDD-sha7
- [x] Firmware: 0.3.YYYYMMDD.sha7 (build) / 0.3.x (release)
- [x] Payload schema: v1.0 (MqttProtocolConstants.SUPPORTED_SCHEMA_VERSIONS)

#### Sécurité CI — DONE
- [x] Aucun secret dans code (grep enforced)
- [x] Secrets uniquement via GitHub Secrets (encrypted at rest)
- [x] OWASP fail CVSS >= 8
- [x] Pas de déploiement auto sur pilote
- [x] Manual approval obligatoire (Environment protection rules)

#### Checklist Release Firmware — DONE
- [x] Pré-release (6 items: tests, cppcheck, compile, version, changelog, schema)
- [x] Validation (6 items: hardware 24h, watchdog, memory, MQTT, offline, signal)
- [x] Déploiement (7 items: artifact signé, canary, progressif, rollback)
- [x] Post-déploiement (4 items: métriques, alertes, signatures, rejections)

#### Documentation — DONE
- [x] docs/ci-cd-mvp3.md (architecture pipeline, jobs, versioning, secrets, checklist firmware, environnements)

### Phase MVP 3.14: Audit Complet MVP 3 — DONE

#### Corrections Appliquees — DONE
- [x] SignatureVerifier.java: constantTimeEquals ne fuit plus la longueur (timing attack fix)
- [x] PayloadValidator.java: non-drain messages utilisent MAX_REALTIME_TIMESTAMP_AGE_SECONDS (600s, pas 300s)
- [x] MqttProtocolConstants.java: ajout MAX_REALTIME_TIMESTAMP_AGE_SECONDS = 600
- [x] DeviceRateLimiter.java: synchronized sur window reset (race condition fix)
- [x] ProtocolValidationPipeline.java: SIGNATURE_DISABLED rejeté (pas bypass) pour telemetry + events
- [x] app_main.cpp: WiFi credentials chargés depuis WifiConfig (plus hardcodés)
- [x] device_config.h: ajout struct WifiConfig
- [x] device_config.cpp: WiFi defaults vides (doit être provisionné)

#### Documents Créés — DONE
- [x] docs/audit-mvp3.md (audit complet 20 domaines, 35 findings, 9 corrections appliquées)
- [x] docs/mvp3-risk-register.md (registre risques: 8 CRITICAL, 9 HIGH, 12 MEDIUM, 6 LOW)
- [x] docs/mvp3-pilot-readiness-checklist.md (9 categories, GO/NO-GO, blocking items, sign-off)

#### Findings par Sévérité — DONE
- [x] 8 CRITICAL identifiés (5 corrigés, 3 ouverts: heartbeat bypass, tenant isolation x2)
- [x] 9 HIGH identifiés (3 corrigés, 6 ouverts: legacy handler, provisioning, CI)
- [x] 12 MEDIUM identifiés (acceptés ou recommandés pour sprint suivant)
- [x] 6 LOW identifiés (acceptés pour le pilote)

#### Corrections Bloquantes Appliquées — DONE
- [x] Keycloak realm configuré (realm-export.json + init-keycloak.sh + docker-compose import)
- [x] Firmware HMAC réel mbedtls (ESP32) + SHA-256 self-contained (host tests)
- [x] Heartbeat security bypass corrigé (rate limiting + revocation check)
- [x] Legacy handler désactivable en production (pyrosense.mqtt.legacy-handler.enabled=false)
- [x] Tenant isolation DataQuality corrigée (findLatestByDeviceAndTenant, findHistoryByDeviceAndTenant)
- [x] DeviceQualityController /summary restreint @PreAuthorize PLATFORM_ADMIN

#### Verdict Pilote — CONDITIONAL GO (2 items hardware restants)
- [x] Keycloak realm configuré — DONE
- [x] Firmware HMAC réel mbedtls — DONE
- [ ] Hardware 24h stabilité (bloquant pour terrain, nécessite ESP32-S3 DevKit)
- [ ] Electricien B2V+ identifié (bloquant opérationnel)

### Phase MVP 3.13: Documentation Finale & Production Readiness — DONE

#### Documents Crees — DONE
- [x] docs/mvp3-overview.md (objectifs, perimetre, prerequis MVP 1/2, architecture edge-cloud, phases, chiffres cles)
- [x] docs/mvp3-production-readiness-checklist.md (88 items, 10 categories, 77 Done / 4 Partial / 7 Not Started, GO/NO-GO)

#### Documents Existants Couvrant le Scope — DEJA FAIT
- [x] docs/edge-cloud-architecture.md (architecture detaillee)
- [x] docs/hardware-prototype-strategy.md (3 options, BOM, risques)
- [x] docs/firmware-architecture.md (ESP32-S3, modules, state machine)
- [x] docs/mqtt-protocol-v1.md (specification complete)
- [x] docs/device-provisioning.md (workflow, securite)
- [x] docs/real-device-ingestion.md (pipeline, rejections, metriques)
- [x] docs/data-quality.md (scoring, grades, regles)
- [x] docs/field-data-collection.md (dataset, pseudonymisation)
- [x] docs/lab-test-protocol.md (14 scenarios, criteres)
- [x] docs/field-pilot-10-devices.md (plan pilote complet)
- [x] docs/iot-security.md (STRIDE, 13 controles, mTLS migration)
- [x] docs/mvp3-observability.md (metriques, alertes, tracing)
- [x] docs/ci-cd-mvp3.md (pipeline 7 jobs, versioning, checklist)

#### README.md — DONE
- [x] Section MVP 3 ajoutee (demarrage rapide, provisioning, telemetrie, limites securite)
- [x] Table documentation MVP 3 & Pilot (17 documents references)
- [x] Avertissement installation par electricien habilite
- [x] Monitoring pilote (Grafana dashboards, Prometheus alerts)

#### TODO.md — DONE
- [x] Phases 3.10-3.13 marquees DONE
- [x] Risques residuels MVP 3 listes
- [x] Proposition MVP 4 ajoutee

### Documentation MVP 3

- [x] docs/mvp3-transition.md (plan complet : 15 sections, roadmap 6 sprints, risques, protocoles, feedback loop, budget)

---

## MVP 4 — Machine Learning & MLOps (PLANIFIE)

> **Document de reference** : docs/mvp4-ml-transition.md

### Phase 4.0 — Fondations ML (Mois 1-2)
- [ ] Feature store offline (vues SQL materialisees + export Parquet)
- [ ] Feature store online (Redis, lookup per device <10ms)
- [ ] MLflow deploye (Docker Compose, model registry)
- [ ] Pipeline evaluation offline (notebook → script automatise)
- [ ] Mesurer baseline statistique sur donnees pilote (metriques de reference)
- [ ] Interface labelling enrichie (technicien peut labeller en 3 clics)

### Phase 4.1 — Premier Modele (Mois 2-3)
- [ ] Entrainement Isolation Forest (scikit-learn, precision ≥0.80)
- [ ] Export ONNX + service inference (container, latence <50ms P99)
- [ ] Adapter MachineLearningInferencePort (remplacer NoOp)
- [ ] Shadow mode comparison engine (dashboard metriques)
- [ ] 4 semaines shadow mode (metriques ≥ stat sur 50+ events)

### Phase 4.2 — Scoring Ameliore (Mois 3-4)
- [ ] Poids adaptatifs par site (feedback terrain → recalibrage)
- [ ] Adapter RiskModelPort (score ensemble stat+ML)
- [ ] Canary deployment (10% fleet, ensemble 0.7 stat + 0.3 ML)
- [ ] SHAP explanations pour predictions ML
- [ ] Dashboard operateur enrichi (stat + ML side by side)

### Phase 4.3 — Modeles Avances (Mois 4-6)
- [ ] Autoencoder (Keras/PyTorch → ONNX, reconstruction error)
- [ ] Shadow mode autoencoder vs Isolation Forest
- [ ] LSTM/TCN si ≥200 sequences labellisees (lead time prediction)
- [ ] Survival analysis (Cox PH) si ≥100 events (maintenance predictive)
- [ ] Model ensemble (vote majoritaire ou stacking)

### Phase 4.4 — Production ML (Mois 5-6)
- [ ] Production deployment (ensemble stat+ML, 100% fleet)
- [ ] Monitoring drift (PSI, KS test, accuracy sliding window)
- [ ] Re-entrainement automatise (Airflow/Prefect, hebdomadaire)
- [ ] Circuit breaker + rollback automatique (<1s)
- [ ] Gouvernance formalisee (comite, registre decisions)
- [ ] Documentation scientifique du pipeline ML

### Contraintes MVP 4
- Scoring statistique reste actif comme filet de securite (JAMAIS retire)
- Shadow mode obligatoire avant tout deploiement utilisateur
- Humain dans la boucle pour alertes CRITICAL
- Confirmations terrain = labels de qualite (gold standard)
- Explicabilite sur chaque prediction (SHAP/feature importance)
- Pas de donnees electriques brutes au cloud
- Pseudonymisation pour tout entrainement
- Circuit breaker : fallback stat si ML unavailable >30s
