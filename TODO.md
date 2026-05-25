# PyroSense AI Platform - TODO & Progress Tracker

## Status: Skeleton Complete | Implementation In Progress

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

### Shared Kernel (FULLY IMPLEMENTED - 52 tests passing)
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

### Alerting Service (FULLY IMPLEMENTED - 78 tests passing)
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
- [x] API Gateway: SecurityConfig hardened (CSP, frame-options DENY, cache disabled, PyroSenseGrantedAuthoritiesConverter)
- [x] API Gateway: RateLimitingFilter (Redis-based, 60/10/120 req/min by endpoint type)
- [x] API Gateway: SecurityHeadersFilter (HSTS, X-Content-Type-Options, Referrer-Policy, Permissions-Policy)
- [x] Cross-service SecurityConfig: alerting, risk-scoring, device, ingestion — all with JWT + TenantContext + PyroSenseGrantedAuthoritiesConverter
- [x] application-secret.example.yml with Vault migration path
- [x] docs/security.md: STRIDE threat model, OWASP ASVS, role/permission matrix, IoT rules, attack protections

### Maintenance Service (FULLY IMPLEMENTED - 49 tests passing)
- [x] Domain model: Intervention aggregate (state machine), InterventionStatus (6 states), InterventionType, InterventionResult (5 outcomes), InterventionPriority, FieldDiagnostic, RiskImpact
- [x] State machine: CREATED → PLANNED → ASSIGNED → IN_PROGRESS → COMPLETED (+ CANCELLED from any non-terminal)
- [x] Business rules: CRITICAL/WARNING alerts auto-create interventions, one per alert (unique constraint), severity → type/priority mapping
- [x] Domain events: MaintenanceInterventionCreatedEvent, MaintenanceInterventionCompletedEvent, ElectricalDefectConfirmedEvent, FalsePositiveConfirmedEvent
- [x] Ports in: CreateInterventionUseCase, ManageInterventionUseCase (schedule/assign/start/diagnostic/complete/risk-impact/cancel), GetInterventionQuery (statistics)
- [x] Ports out: InterventionRepositoryPort, MaintenanceEventPublisherPort
- [x] Use case implementations: CreateInterventionService, ManageInterventionService, GetInterventionService
- [x] Kafka consumer: alerting-events → auto-creates interventions for CRITICAL/WARNING alerts
- [x] Kafka producer: maintenance-events (all domain events)
- [x] REST API: full CRUD + lifecycle operations + statistics + queries by tenant/electrician/device/alert
- [x] JDBC persistence: JdbcInterventionRepository with JSONB (diagnostic, risk_impact)
- [x] SecurityConfig: OAuth2 JWT + TenantContext + @PreAuthorize per role
- [x] GlobalExceptionHandler (RFC 7807 ApiErrorResponse)
- [x] Flyway V001: interventions table with indexes (tenant, status, device, electrician, alert unique)
- [x] Risk impact measurement: riskScoreBefore, riskScoreAfter, avoidedIncidentEstimate, riskReduction()
- [x] Feedback loop: FalsePositiveConfirmedEvent → signal-analysis threshold adjustment, ElectricalDefectConfirmedEvent → positive reinforcement
- [x] Unit tests: 15 Intervention, 6 InterventionStatus, 4 RiskImpact, 4 CreateInterventionService, 9 ManageInterventionService
- [x] ArchUnit tests: 10 rules (hexagonal enforcement)
- [x] Context load test (embedded Kafka)
- [x] Documentation: docs/maintenance.md

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

### Service Skeletons (remaining 1 module)
- [x] pyrosense-notification-service (port 8087) - Multi-channel, SendNotificationUseCase

### Infrastructure
- [x] Docker Compose (PostgreSQL, Redis, Kafka, Mosquitto, Keycloak, Prometheus, Grafana, Loki)
- [x] Multi-database init script (9 databases with TimescaleDB)
- [x] Mosquitto config
- [x] Prometheus scrape config (all 10 services)
- [x] Prometheus alert rules
- [x] Loki config
- [x] Grafana provisioning (datasources)

### Documentation
- [x] README.md (project overview, quick start, tech stack)
- [x] docs/architecture.md (C4, bounded contexts, ADRs, hexagonal)
- [x] docs/security.md (auth, OWASP, secrets, network)
- [x] docs/devops.md (containers, CI/CD, observability, scaling)
- [x] docs/testing-strategy.md (pyramid, tools, conventions)
- [x] docs/timeseries-storage.md (schema, aggregates, retention, GDPR)
- [x] docs/signal-analysis.md (detection pipeline, algorithms, ML extension, thresholds)
- [x] docs/risk-scoring.md (formula, factors, explainability, ML extension, trend/prediction)
- [x] docs/alerting-service.md (lifecycle, deduplication, escalation, SLA, API, events)
- [x] docs/maintenance.md (lifecycle, feedback loop, risk impact, API, events)
- [x] docs/reporting.md (report types, PDF generation, secure download, signature, API, events)
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

### Phase 1: Core Implementation (Priority)
- [x] Flyway migrations: device-service, ingestion-service, signal-analysis-service
- [x] JPA entities + repositories: device-service, ingestion-service, signal-analysis-service
- [x] REST controllers: device-service, ingestion-service, signal-analysis-service
- [x] Kafka producers: device-service, ingestion-service, signal-analysis-service
- [x] Kafka consumers: signal-analysis-service (telemetry-events)
- [x] MQTT listener: ingestion-service (HiveMQ, topic subscriptions)
- [x] Redis idempotency adapter: ingestion-service
- [x] Signal analysis: hybrid detection (z-score, micro-arc, temperature, THD drift, exponential smoothing)
- [x] ML-ready port: MachineLearningInferencePort + NoOp stub
- [x] Risk scoring: weighted multi-factor formula, recency decay, repetition boost, ML-ready port
- [ ] Remaining services: Flyway, JPA, REST, Kafka (alerting, notification, reporting, maintenance)
- [ ] Redis cache adapter (scoring service)
- [ ] MapStruct mappers (entity <-> domain)
- [ ] Global exception handlers (RFC 7807)

### Phase 2: Integration
- [ ] Keycloak realm configuration (realm export JSON)
- [ ] Service-to-service Kafka event wiring
- [ ] API Gateway JWT validation end-to-end
- [ ] Full signal pipeline: MQTT -> Ingestion -> Kafka -> Analysis -> Scoring -> Alert -> Notification

### Phase 3: Quality
- [ ] Unit tests: domain models (all services)
- [ ] Unit tests: use cases (mocked ports)
- [ ] ArchUnit tests (hexagonal rules enforcement)
- [ ] Integration tests with Testcontainers
- [ ] Contract tests (Spring Cloud Contract)
- [ ] JaCoCo coverage >= 80%

### Phase 4: Production Readiness
- [ ] Dockerfiles per service (multi-stage)
- [ ] Docker Compose with all services (docker profile)
- [ ] GitHub Actions CI/CD pipeline
- [ ] Health checks and readiness probes
- [ ] Distributed tracing (OpenTelemetry)
- [ ] API documentation (SpringDoc OpenAPI)
- [x] Rate limiting (Redis-based via API Gateway)
- [x] Security hardening (CORS, CSP, HSTS, XSS, CSRF, rate limiting)
- [x] Multi-tenancy via JWT tenant_id claim + TenantContext ThreadLocal
- [x] RBAC with 8 roles and 34 permissions (method-level security)
- [x] Device authentication (HMAC-SHA256, rotation, expiry)
- [x] Audit logging (AOP-based, structured)
- [x] Anti mass-assignment (@AllowedFields)
- [x] Replay attack prevention (HMAC + timestamp window)
- [x] Payload integrity (HMAC signature on device telemetry)

### Phase 5: Advanced Features
- [ ] ML-based anomaly detection service
- [ ] Federated learning module
- [ ] WebSocket real-time dashboard
- [x] PDF report generation (OpenPDF) — implemented in pyrosense-reporting-service
- [ ] Mobile push notifications (Firebase)
- [ ] Keycloak realm export (users, roles, client configuration)

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
