# PyroSense AI Platform - Audit Report

**Date:** 2026-05-25  
**Auditor:** Architecture Senior / Security Expert / DevOps Engineer  
**Scope:** Full platform audit (20 axes)  
**Status:** MVP  

## Executive Summary

Full audit of the PyroSense AI Platform covering 20 analysis axes. **136 findings** identified:

| Severity | Count | Description |
|----------|-------|-------------|
| CRITICAL | 4 | Must fix before any deployment |
| HIGH | 54 | Fix before pilot |
| MEDIUM | 56 | Fix before production |
| LOW | 22 | Technical debt, fix when convenient |

### Overall Assessment

The platform demonstrates solid architectural foundations: clean hexagonal architecture, no framework leaks into domain layers, proper bounded context isolation via Kafka events, and good security baseline (OAuth2/JWT, parameterized queries, header sanitization). However, the multi-tenancy enforcement has critical gaps, inter-service communication lacks resilience patterns, and the IoT ingestion pipeline needs performance optimization for production scale.

---

## CRITICAL Findings (Must Fix Immediately)

### C-1: Cross-Tenant Data Leak in Alert Queries
- **File:** `pyrosense-alerting-service/.../JdbcAlertRepository.java`
- **Problem:** `findById()` queries by ID without tenant_id filter. Combined with unprotected AlertController, any authenticated user can access any tenant's alerts.
- **Impact:** Complete tenant isolation bypass for alerting data.
- **Fix:** Add `AND tenant_id = ?` to findById query. Validate tenant in service layer.

### C-2: Cross-Tenant Data Leak in Alert List
- **File:** `pyrosense-alerting-service/.../AlertController.java:44-68`
- **Problem:** `list` endpoint accepts `tenantId` as request parameter. `findByStatus`/`findBySeverity` queries return ALL tenants' data.
- **Impact:** Full enumeration of all tenants' alerts.
- **Fix:** Derive tenantId from TenantContext.require(). Add tenant filter to all queries.

### C-3: Cross-Tenant Data Leak in Alert GetById
- **File:** `pyrosense-alerting-service/.../AlertController.java:37`
- **Problem:** `getById` endpoint retrieves alert without verifying it belongs to the caller's tenant.
- **Impact:** Any user can access any alert by ID.
- **Fix:** Validate alert.tenantId().equals(TenantContext.require()) after fetch.

### C-4: No Timeout on Inter-Service HTTP Calls
- **File:** `pyrosense-ingestion-service/.../HttpDeviceAuthorizationAdapter.java:21-24`
- **Problem:** RestClient created without connect/read timeout. If device-service is slow, all ingestion threads block indefinitely = cascading failure.
- **Impact:** Single slow service takes down entire ingestion pipeline.
- **Fix:** Configure connect timeout (2s) and read timeout (5s).

---

## HIGH Findings (Fix Before Pilot)

### Architecture (4)
| # | File | Problem |
|---|------|---------|
| H-1 | identity/.../UserController.java | Controller bypasses application layer, directly uses outbound port (UserRepository) |
| H-2 | signal-analysis/.../AnalysisController.java | Controller directly uses outbound ports (AnomalyRepositoryPort, BaselineProfileRepositoryPort) |
| H-3 | identity/.../Membership.java | Mutable value object - getRoles() returns mutable internal EnumSet |
| H-4 | identity/.../Membership.java:47 | Encapsulation violation - external mutation of aggregate state possible |

### Security & Multi-tenancy (14)
| # | File | Problem |
|---|------|---------|
| H-5 | alerting/.../AlertController.java | No @PreAuthorize on any endpoint |
| H-6 | scoring/.../RiskScoreController.java | No @PreAuthorize, no tenant validation |
| H-7 | ingestion/.../IngestionController.java | No @PreAuthorize, no role enforcement |
| H-8 | alerting/.../JdbcAlertRepository.java:115 | findByStatus/findBySeverity query across ALL tenants |
| H-9 | device/.../DeviceController.java:114 | getById doesn't check device belongs to caller's tenant |
| H-10 | device/.../DeviceController.java:128 | listByTenant accepts any tenantId without validation |
| H-11 | device/.../SecurityConfig.java:46 | Heartbeat endpoint is permitAll() - fake heartbeats possible |
| H-12 | notification/.../NotificationController.java:38 | list accepts tenantId param without validation |
| H-13 | notification/.../NotificationController.java:53 | listByRecipient no tenant filtering |
| H-14 | maintenance/.../InterventionController.java:119 | getById no tenant check |
| H-15 | maintenance/.../InterventionController.java:138 | listByElectrician/listByDevice cross-tenant |
| H-16 | reporting/.../ReportController.java:80 | list accepts tenantId without validation |
| H-17 | scoring/.../JdbcRiskAssessmentRepository.java:72 | findLatestByBuilding ignores tenant completely |
| H-18 | shared/.../TenantContext.java | ThreadLocal not propagated to async/Kafka threads |

### Kafka Robustness (9)
| # | File | Problem |
|---|------|---------|
| H-19 | ingestion/.../KafkaTelemetryEventPublisher.java:37 | Fire-and-forget: Kafka send future never inspected |
| H-20 | device/.../KafkaDeviceEventPublisher.java:44 | Same fire-and-forget pattern |
| H-21 | alerting/.../KafkaAlertEventPublisher.java:43 | Same - alert events can be silently lost |
| H-22 | analysis/.../KafkaAnalysisEventPublisher.java:37 | Same - anomaly events lost |
| H-23 | scoring/.../KafkaScoringEventPublisher.java:37 | Same - critical risk events lost |
| H-24 | analysis/.../KafkaConfig.java:34 | No consumer error handler - poison pill blocks partition |
| H-25 | scoring/.../KafkaConfig.java:34 | Same - no error handler |
| H-26 | alerting/.../KafkaConfig.java:36 | Same - no error handler |
| H-27 | Multiple services | All event publishers swallow exceptions (catch + log only) |

### MQTT (4)
| # | File | Problem |
|---|------|---------|
| H-28 | ingestion/.../MqttTelemetryListener.java:84 | No automatic reconnection logic |
| H-29 | ingestion/.../MqttTelemetryListener.java:112 | No explicit message acknowledgment (auto-ack before processing) |
| H-30 | ingestion/.../MqttTelemetryListener.java:112 | Blocking I/O on Netty thread - starves MQTT client |
| H-31 | ingestion/.../MqttTelemetryListener.java | No backpressure mechanism |

### Performance (5)
| # | File | Problem |
|---|------|---------|
| H-32 | ingestion/.../IngestTelemetryService.java:44 | Sequential processing bottleneck (HTTP+Redis+DB+Kafka per msg) |
| H-33 | ingestion/.../IngestTelemetryService.java:97 | Single-row DB insert (batch method exists but unused) |
| H-34 | ingestion/.../HttpDeviceAuthorizationAdapter.java:27 | 2 HTTP calls per telemetry message, no caching |
| H-35 | ingestion/.../HttpDeviceAuthorizationAdapter.java | No connection pooling on RestClient |
| H-36 | ingestion/.../MqttTelemetryListener.java | Blocking calls in MQTT callback path |

### Error Handling (4)
| # | File | Problem |
|---|------|---------|
| H-37 | maintenance/.../JdbcInterventionRepository.java:215 | catch(Exception ignored){} - swallowed exception |
| H-38 | maintenance/.../JdbcInterventionRepository.java:228 | Same - diagnostic data silently lost |
| H-39 | alerting/.../KafkaAlertEventPublisher.java:45 | Swallowed Kafka publish failure |
| H-40 | Multiple Kafka publishers | All event publishers catch+swallow on failure |

### DevOps & Production (11)
| # | File | Problem |
|---|------|---------|
| H-41 | docker-compose.yml:108 | Single Kafka broker, replication factor 1 |
| H-42 | docker-compose.yml:52 | Single PostgreSQL for 9 databases (SPOF) |
| H-43 | gateway/.../GatewayConfig.java:40 | No response/connect timeout on gateway routes |
| H-44 | alerting/.../AlertController.java:44 | Unbounded queries - no pagination, no LIMIT |
| H-45 | notification/.../NotificationController.java:35 | Unbounded queries + in-memory filtering |
| H-46 | Multiple services | No graceful shutdown configured (server.shutdown: graceful) |
| H-47 | 5 services | Default HikariCP pool settings (no tuning) |
| H-48 | ingestion/.../IngestionController.java:56 | No authentication on ingestion endpoint |
| H-49 | .github/workflows/ci.yml:183 | Docker build never pushes/tags, missing env file |
| H-50 | All Kafka consumers | No trace propagation (broken distributed tracing) |
| H-51 | ingestion/.../HttpDeviceAuthorizationAdapter.java | No circuit breaker on inter-service calls |

### Tests & Flyway (5)
| # | File | Problem |
|---|------|---------|
| H-52 | identity-service (missing) | No Flyway migrations - in-memory only persistence |
| H-53 | Multiple services | No controller integration tests (@WebMvcTest) |
| H-54 | ingestion/.../IngestionPerformanceSmokeTest.java | Flaky timing-based assertion |
| H-55 | scoring/.../RiskScoringPerformanceSmokeTest.java | Same flaky pattern |
| H-56 | analysis/.../AnalysisController.java:65 | Domain model (SignalAnomaly) exposed in REST API |

---

## MEDIUM Findings (56)

### Architecture & DDD (9)
- Intervention aggregate events raised externally (not from aggregate methods)
- CreateInterventionService publishes events instead of aggregate
- MqttTelemetryListener uses outbound port directly (DLQ publishing)
- Tenant entity not extending AggregateRoot, no domain events
- User entity not extending AggregateRoot, no domain events for security actions
- DeviceCredential rotation has no domain events for audit
- Duplicate RiskScore value objects (shared-kernel vs scoring-service domain)
- Report domain model uses static AtomicLong for sequence (not cluster-safe)
- Notification entity not extending AggregateRoot

### Security (7)
- .env.docker committed with dev passwords
- DB passwords have fallback defaults in application.yml
- Gateway actuator exposes route info
- Swagger/OpenAPI enabled without access restriction
- Report download endpoint has no rate limiting
- CORS config string not resolved by Spring property resolver
- Service-to-service call has no authentication token

### Kafka & MQTT (11)
- No idempotent producer configuration
- Device-service producer missing retries/idempotence
- Alerting producer missing acks=all
- No max.poll.records on signal-analysis consumer
- KafkaScoringEventListener uses hardcoded null UUID for missing tenantId
- MQTT subscription without explicit QoS
- MQTT no circuit breaker on broker failure
- Docker Compose Kafka at /tmp path
- Signal-analysis consumer may exceed max.poll.interval.ms
- No consumer auto.offset.reset documentation
- RestClient no connection pooling config

### Error Handling (6)
- RuntimeException thrown instead of domain exceptions (3 services)
- 4 services missing GlobalExceptionHandler
- Reporting metadata serialization swallows errors
- Reporting metadata deserialization returns null silently

### Naming (3)
- Identity service ports use `*Repository` instead of `*RepositoryPort`
- Inconsistent Query/UseCase naming
- Inconsistent DTO naming (inner records vs separate classes)

### Flyway (4)
- Ingestion migrations require TimescaleDB (no H2 fallback)
- Risk assessments table missing tenant_id column
- JSONB/CLOB mismatch between Postgres and H2 test schemas
- Missing TIMESTAMPTZ in reporting migrations

### DevOps & Observability (10)
- No resource limits in docker-compose
- Grafana dashboards reference wrong metric names
- Prometheus alert rules reference wrong metric names
- Notification/maintenance Kafka listeners have no metrics
- No SAST or container image scanning in CI
- Keycloak in start-dev mode with default creds
- Redis without password
- Analysis/scoring services missing security config for actuator
- No SBOM generation
- Rate limit key uses spoofable X-Forwarded-For

### Tests (6)
- Missing DTO validation on ingestion TelemetryPayload
- Missing validation on maintenance RiskImpactRequest
- Conditional test assertions (if/then verify)
- Integration test doesn't use actual Flyway migrations
- No Kafka listener tests
- KafkaScoringEventListener null-tenant fallback untested

---

## LOW Findings (22)

Architecture: TelemetryReading validation in separate class, RiskAssessment not AggregateRoot, BaselineProfile not AggregateRoot, IntegrationEvent uses Instant.now() not ClockProvider, TelemetryValidator uses Instant.now()

Naming: Controller uses outbound port for reads, inconsistent read port naming, DTO naming conventions

Flyway: Risk assessments missing tenant_id (duplicate), H2/Postgres timestamp mismatch, migration numbering

Tests: RuntimeException for SHA-256, performance test threshold sensitivity

DevOps: LWT not configured on MQTT, Kafka log dir at /tmp, alert rules may not fire, rate limit header spoofing

---

## Positive Observations

1. **No CRITICAL architecture violations** - Domain models are framework-free (no JPA annotations)
2. **No cross-service Java imports** - Services communicate only via Kafka IntegrationEvent
3. **No SQL injection** - All queries use parameterized statements
4. **Proper .gitignore** - Secret files excluded
5. **Good CSRF handling** - Disabled for stateless JWT APIs
6. **Header sanitization** - Gateway strips spoofed tenant/user/role headers
7. **Rich domain models** - Device, Alert, Intervention have proper state machines
8. **Clean use-case wiring** - @Bean factory pattern, no @Service on use cases
9. **Shared kernel is minimal** - Only IDs, value objects, exceptions, domain primitives
10. **Comprehensive test coverage** - 500+ tests, ArchUnit rules per service

---

## Priority Remediation Plan

### Phase 1: Critical (Before any deployment)
1. Fix all cross-tenant data leaks (C-1 through C-3)
2. Add HTTP timeouts and circuit breaker (C-4)
3. Add tenant validation to all endpoints accepting tenantId parameter
4. Add tenant_id filter to all repository findById queries

### Phase 2: High Priority (Before pilot)
1. Add Kafka send callbacks with DLQ routing (5 publishers)
2. Add consumer error handlers (3 services)
3. Add MQTT auto-reconnect and thread pool offloading
4. Cache device authorization (eliminate per-message HTTP calls)
5. Use batch DB inserts for telemetry
6. Add pagination to all list endpoints
7. Configure gateway timeouts
8. Add @PreAuthorize to all controllers
9. Configure graceful shutdown
10. Tune HikariCP pools

### Phase 3: Medium (Before production)
1. Fix error handling (remove swallowed exceptions)
2. Align metric names (code vs dashboards)
3. Enable Kafka observation (distributed tracing)
4. Add GlobalExceptionHandler to remaining services
5. Add controller integration tests
6. Create identity-service Flyway migrations
7. Fix Flyway H2/Postgres inconsistencies
8. Add resource limits to Docker Compose

---

## Production Readiness Checklist (Post-Audit)

| Category | Score | Notes |
|----------|-------|-------|
| Hexagonal Architecture | 85% | Minor violations in 2 controllers |
| DDD Compliance | 75% | Some aggregates lack events, a few anemic models |
| Module Coupling | 95% | Excellent - no cross-service imports |
| Security | 55% | Critical tenant isolation gaps |
| Multi-tenancy | 40% | Most endpoints lack tenant validation |
| Error Handling | 60% | Swallowed exceptions, missing handlers |
| Kafka Robustness | 45% | Fire-and-forget everywhere, no DLQ |
| MQTT Robustness | 40% | No reconnect, no backpressure |
| IoT Performance | 50% | No batching, no caching, blocking calls |
| Observability | 65% | Metrics registered but dashboard mismatch |
| DevOps/CI | 70% | Good CI but missing image push, scanning |
| Tests | 75% | Good unit coverage, weak integration |
| Naming | 85% | Minor inconsistencies |
| Flyway | 70% | Identity service missing, H2 mismatches |

**Overall Score: 65% (MVP adequate, not pilot-ready)**

---

*End of audit report.*
