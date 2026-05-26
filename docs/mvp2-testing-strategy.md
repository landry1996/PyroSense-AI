# MVP 2 - Strategie de Tests Complete

## Vue d'Ensemble

La strategie de tests du MVP 2 couvre l'ensemble de la plateforme PyroSense : backend Java (11 services), frontend Angular, et pipeline CI/CD. Elle etend la strategie de base (cf. `docs/testing-strategy.md`) avec des tests specifiques aux nouvelles fonctionnalites MVP 2 : event-driven Kafka, dashboard query service, preferences notifications avancees, templates PDF professionnels, audit transversal et securite renforcee.

## Objectifs de Couverture

| Couche | Objectif | Justification |
|--------|----------|---------------|
| Domain (aggregats, value objects, events) | **>95%** | Logique metier pure, critique, sans excuses |
| Application (use cases) | **>85%** | Orchestration avec ports mockes |
| Adapters (REST, Kafka, JDBC) | **>70%** | Verifies par tests d'integration |
| Config | Exclue | Cablage Spring, verifie par context load |
| Frontend (services, guards, pipes) | **>85%** | Logique critique cote client |
| Frontend (composants) | **>75%** | Rendu + interactions utilisateur |
| Regles critiques (securite, dedup, SLA) | **>95%** | Zero regression toleree |

---

## 1. Tests Backend

### 1.1 Tests Unitaires Domain

Verifient la logique metier pure sans Spring, sans I/O, sans infrastructure.

| Service | Classes testees | Tests |
|---------|-----------------|:-----:|
| shared-kernel | IntegrationEvent, RiskScore, Percentage, StronglyTypedId, exceptions, EventMetadata | 86 |
| alerting | Alert (state machine), AlertStatus, EscalationLevel, SlaPolicy, DeduplicationKey | 46 |
| maintenance | Intervention (state machine), InterventionRecommendation, RiskImpact, SlaPolicyTest, InterventionPriorityPolicy | 43 |
| reporting | Report (state machine), DownloadToken, ReportSignature, ReportPeriod, ReportExpiry, ReportViewModel | 43 |
| notification | Notification (retry), ChannelRoutingPolicy, Recipient, DeduplicationKey, NotificationPreferences, NotificationTemplate, TenantNotificationPolicy | 56 |
| dashboard | DashboardOverview, RiskyBuilding, RiskTrendPoint | 5 |
| device | Device (state machine), DeviceStatus | 20 |
| ingestion | TelemetryReading, TelemetryValidator | 19 |
| signal-analysis | SignalWindow, SignalFeature, detectors (5 algos), BaselineBuilder | 43 |
| risk-scoring | RiskScore, RiskScoringEngine, RiskFactor, RiskTrend | 37 |

**Pattern type :**

```java
class InterventionTest {
    @Test
    void shouldRejectCompletionWithoutDiagnostic() {
        var intervention = createAssignedIntervention();
        intervention.start();
        assertThatThrownBy(() -> intervention.complete(InterventionResult.DEFECT_CONFIRMED))
            .isInstanceOf(InvalidStateTransitionException.class);
    }
}
```

### 1.2 Tests Use Case (Application Layer)

Verifient l'orchestration avec ports mockes (Mockito). Pas de Spring context.

| Service | Classes testees | Tests |
|---------|-----------------|:-----:|
| alerting | CreateAlertService, ManageAlertService, EscalateAlertsService, GetAlertService | 13 |
| maintenance | CreateInterventionService, ManageInterventionService, ManageRecommendationService, AddInterventionCommentService | 18 |
| reporting | GenerateReportService, GetReportService, RequestReportService | 18 |
| notification | SendNotificationService, RetryNotificationService, ProcessNotificationEventService, ManagePreferencesService, ManageTenantPolicyService, GetAuditLogService | 43 |
| dashboard | GetDashboardOverviewService, GetRiskyBuildingsService | 5 |

**Verifications type :**
- Port appele avec les bons arguments
- Event publie apres chaque transition
- Idempotence respectee (dedup check)
- Erreur propagee correctement (NotFoundException, ValidationException)
- Tenant isolation dans le query

### 1.3 Tests d'Integration (Testcontainers)

Verifient les adapters avec de vraies infrastructures.

#### PostgreSQL / TimescaleDB

| Service | Test | Infrastructure |
|---------|------|----------------|
| dashboard | JdbcDashboardReadModelIntegrationTest (9 tests) | PostgreSQL 16 |
| ingestion | JdbcTelemetryRepositoryIT (3 tests) | TimescaleDB |
| device | DevicePersistenceIT (5 tests) | PostgreSQL 16 |
| alerting | JdbcAlertRepositoryIT (8 tests) | H2 (in-process) |

#### Kafka

| Service | Test | Verifications |
|---------|------|---------------|
| ingestion | KafkaEventPublisherIT | IntegrationEvent envelope, topic, key, DLQ |
| alerting | KafkaAlertEventPublisherIT | Enriched envelope (version, correlationId, tenantId, sourceService), key=eventId |
| maintenance | KafkaMaintenanceEventPublisherIT | Correlation metadata, default correlationId |

#### Redis

| Service | Test | Verifications |
|---------|------|---------------|
| ingestion | RedisIdempotencyIT | TTL, dedup key, concurrent access |
| notification | RedisDeduplicationAdapter | Window TTL, fingerprint, concurrent calls |

#### PDF

| Service | Test | Verifications |
|---------|------|---------------|
| reporting | ProfessionalPdfRendererTest (17 tests) | PDF valide, sections, disclaimer, signature, footer |
| reporting | OpenPdfReportRendererTest (4 tests) | Rendu legacy compatible |

### 1.4 Tests REST (@WebMvcTest)

Verifient la couche controller avec MockMvc et SecurityConfig importee.

| Service | Test | Assertions |
|---------|------|------------|
| dashboard | DashboardControllerTest (6) | Endpoints, DTO mapping, pagination |
| dashboard | DashboardSecurityTest (6) | Roles, 401/403 |
| maintenance | InterventionControllerTest (3) | CRUD, lifecycle |
| maintenance | InterventionSecurityTest (8) | RBAC, 401 unauthenticated, 403 forbidden |
| reporting | ReportControllerTest (3) | Generation, download |
| reporting | ReportControllerSecurityTest (7) | Roles, insurer restrictions |
| notification | NotificationControllerTest (4) | Query, retry, preferences |
| notification | NotificationSecurityTest (4) | Role restrictions |
| notification | AuditLogSecurityTest (8) | PLATFORM_ADMIN/TENANT_ADMIN only |

**Pattern type :**

```java
@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardSecurityTest {
    @Test
    void electricianShouldNotAccessOverview() {
        mockMvc.perform(get("/api/v1/dashboard/overview")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ELECTRICIAN"))))
            .andExpect(status().isForbidden());
    }
}
```

### 1.5 Tests de Securite

#### Tenant Isolation

| Service | Test | Scenarios |
|---------|------|-----------|
| dashboard | DashboardTenantIsolationTest (5) | Overview, risky-buildings, risk-trend, alerts per tenant, no-context fail |
| reporting | ReportTenantIsolationTest (5) | List per tenant, cross-tenant reject, insurer/electrician blocked |
| maintenance | InterventionTenantIsolationTest (4) | List, kanban, statistics, no-context fail |
| notification | NotificationTenantIsolationTest (5) | List, no-context, role restrictions |

#### Rate Limiting

| Service | Endpoint | Limite |
|---------|----------|--------|
| reporting | POST /reports/* | 5 req/min/tenant |
| notification | POST /retry | 10 req/min/tenant |
| notification | PUT /preferences | 20 req/min/tenant |

#### Anti Mass-Assignment

Services avec `@AllowedFields` + `AllowedFieldsInterceptor` :
- notification-service (preferences, policy)
- maintenance-service (interventions)
- reporting-service (report generation)

### 1.6 Tests ArchUnit

Chaque service possede `HexagonalArchitectureTest.java` avec **10 regles** :

| # | Regle | Description |
|---|-------|-------------|
| 1 | Domain ne depend pas de Spring | Aucun import `org.springframework` |
| 2 | Domain ne depend pas des adapters | Isolation complete |
| 3 | Domain est auto-contenu | Pas de dependance application |
| 4 | Application ne depend pas des adapters | Uniquement via ports |
| 5 | Application ne connait pas Config | Separation cablage |
| 6 | Ports sont des interfaces | port.in + port.out |
| 7 | Architecture en couches | Acces layer respecte |
| 8 | Events implementent DomainEvent | Contrat applique |
| 9 | Pas de @Service sur use cases | POJOs cables via Config |
| 10 | Pas d'injection par champ | Constructeur only |

**Tests plateforme** (`PlatformArchitectureRulesTest`) :
- Pas de cycles entre packages
- Controllers ne referencent pas les repositories
- Pas d'imports cross-contexte (alerting ↛ ingestion.domain)
- Value objects immutables (pas de setters)
- Conventions de nommage (*UseCase, *Port, *Adapter, *Event)

### 1.7 Tests de Performance (Smoke)

| Service | Test | Seuil |
|---------|------|-------|
| ingestion | 1000 messages ingest | < 5s total, < 10ms avg |
| risk-scoring | 500 calculs risk | < 3s total, < 5ms avg |
| signal-analysis | 200 fenetres analysees | < 4s total, < 20ms avg |

---

## 2. Tests Frontend Angular

### 2.1 Tests de Composants

| Composant | Fichier | Verifications |
|-----------|---------|---------------|
| MetricCardComponent | metric-card.component.spec.ts (4) | Rendu icon/value/label, variantes, lien routerLink |
| ConfirmDialogComponent | confirm-dialog.component.spec.ts (4) | Affichage message, boutons, result confirm/cancel |
| StatusChipComponent | status-chip.component.spec.ts (4) | Couleur par statut, texte, classe CSS |
| SeverityBadgeComponent | severity-badge.component.spec.ts (4) | Badge couleur, icone severity |
| DashboardComponent | dashboard.component.spec.ts | Cards KPI, chart risque, raccourcis |
| AlertListComponent | alert-list.component.spec.ts | Filtres, pagination, actions |
| InterventionListComponent | intervention-list.component.spec.ts | Kanban 5 colonnes, drag |
| BuildingListComponent | building-list.component.spec.ts | Cards, badge severity, search |
| DeviceListComponent | device-list.component.spec.ts | Table, filtre statut |
| NotificationListComponent | notification-list.component.spec.ts | Stats, filtre canal |
| ReportListComponent | report-list.component.spec.ts | Filtre type, download |
| SettingsComponent | settings.component.spec.ts | Onglets, sauvegarde |

### 2.2 Tests de Services

| Service | Fichier | Verifications |
|---------|---------|---------------|
| ApiService | api.service.spec.ts (10) | GET/POST/PUT/DELETE, error handling, retry |
| AuthService | auth.service.spec.ts | Login, logout, token refresh, roles |
| DashboardApiService | dashboard-api.service.spec.ts | Overview, risky-buildings, risk-trend |
| AlertApiService | alert-api.service.spec.ts | List, acknowledge, resolve, create-intervention |
| InterventionApiService | intervention-api.service.spec.ts | Kanban, lifecycle actions |
| ReportApiService | report-api.service.spec.ts | Generate, download-token, download |
| NotificationApiService | notification-api.service.spec.ts | List, preferences, retry |
| BuildingApiService | building-api.service.spec.ts | List, detail, devices |
| WebSocketService | websocket.service.spec.ts | Connect, subscribe, reconnect |

### 2.3 Tests de Guards

| Guard | Fichier | Verifications |
|-------|---------|---------------|
| AuthGuard | auth.guard.spec.ts | Redirige si non authentifie |
| RoleGuard | role.guard.spec.ts (3) | Autorise role correct, refuse mauvais role, gestion multi-roles |
| UnsavedChangesGuard | unsaved-changes.guard.spec.ts (4) | Dialog confirmation, annulation, pas de dialog si pas de changes |

### 2.4 Tests d'Interceptors

| Interceptor | Fichier | Verifications |
|-------------|---------|---------------|
| AuthInterceptor | auth.interceptor.spec.ts (2) | Ajoute Bearer token, skip si pas de token |
| TenantInterceptor | tenant.interceptor.spec.ts (2) | Ajoute X-Tenant-Id header |
| CorrelationInterceptor | correlation.interceptor.spec.ts (2) | Genere UUID, propage existant |
| ErrorInterceptor | error.interceptor.spec.ts (4) | 401 → logout, 403 → toast, 500 → toast, retry sur timeout |

### 2.5 Tests de Pipes

| Pipe | Fichier | Verifications |
|------|---------|---------------|
| RiskLevelPipe | risk-level.pipe.spec.ts (6) | LOW/MODERATE/HIGH/CRITICAL, edge cases |
| RelativeTimePipe | relative-time.pipe.spec.ts (5) | Secondes, minutes, heures, jours, semaines |

### 2.6 Tests E2E (Cypress)

| Fichier | Scenarios |
|---------|-----------|
| dashboard.cy.ts | Navigation, stat cards, raccourcis |
| alerts.cy.ts | Filtres, actions, detail timeline |
| interventions.cy.ts | Kanban, drag, lifecycle |
| buildings.cy.ts | Liste, detail, devices |
| reports.cy.ts | Generation, download |
| settings.cy.ts | Preferences, sauvegarde |

---

## 3. Pipeline CI/CD (GitHub Actions)

### 3.1 Jobs Pipeline

```
┌─────────────────┐
│  code-quality   │ (Spotless check)
└────────┬────────┘
         │
┌────────▼────────┐
│ build-and-test  │ (mvn compile + mvn test)
└────────┬────────┘
         │
    ┌────┼────┬────────────────┐
    │    │    │                 │
┌───▼──┐ ┌──▼───┐ ┌──────▼──────┐ ┌────▼─────┐
│ IT   │ │ Cov  │ │ Security    │ │ Frontend │
│ tests│ │ erage│ │ Scan (OWASP)│ │ Tests    │
└───┬──┘ └──┬───┘ └──────┬──────┘ └────┬─────┘
    │        │             │              │
    └────────┴──────┬──────┴──────────────┘
                    │
            ┌───────▼────────┐
            │  Docker Build  │ (main branch only)
            └────────────────┘
```

### 3.2 Detail des Jobs

| Job | Commande | Declencheur | Timeout |
|-----|----------|-------------|---------|
| code-quality | `mvn spotless:check -B` | Push + PR | 30 min |
| build-and-test | `mvn test -B` | Push + PR | 30 min |
| integration-tests | `mvn verify -Ptest -B -Dsurefire.skip=true` | Apres build-and-test | 30 min |
| coverage | `mvn verify -B -Djacoco.skip=false` | Apres build-and-test | 30 min |
| security-scan | `mvn dependency-check:check -B` | Apres build-and-test | 30 min |
| frontend-tests | `ng test --watch=false --browsers=ChromeHeadless` | Apres build-and-test | 15 min |
| docker-build | `docker compose --profile services build` | Main branch push | 30 min |

### 3.3 Frontend CI

```yaml
frontend-tests:
  name: Frontend Tests
  runs-on: ubuntu-latest
  needs: build-and-test
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-node@v4
      with:
        node-version: '20'
        cache: 'npm'
        cache-dependency-path: pyrosense-dashboard/package-lock.json
    - run: npm ci
      working-directory: pyrosense-dashboard
    - run: npx ng test --watch=false --browsers=ChromeHeadless --code-coverage
      working-directory: pyrosense-dashboard
    - uses: actions/upload-artifact@v4
      with:
        name: frontend-coverage
        path: pyrosense-dashboard/coverage/
        retention-days: 14
```

### 3.4 Artefacts

| Artefact | Retention | Contenu |
|----------|-----------|---------|
| unit-test-results | 7 jours | Surefire XML reports |
| integration-test-results | 7 jours | Failsafe XML reports |
| jacoco-coverage-reports | 14 jours | HTML + CSV coverage |
| frontend-coverage | 14 jours | Karma LCOV coverage |
| owasp-dependency-check-report | 14 jours | Vulnerabilites |

### 3.5 Concurrence

- Cancel-in-progress pour meme branche
- Matrix Java 21 (extensible 23)
- Cache Maven `.m2/repository` (hashFiles pom.xml)
- Cache npm `node_modules`

---

## 4. Regles Critiques (>95% coverage obligatoire)

| Regle | Service | Tests |
|-------|---------|-------|
| Alert state machine (transitions, invalid states) | alerting | 14 AlertStatus + 23 Alert |
| Intervention state machine + diagnostic required | maintenance | 15 Intervention + 6 InterventionStatus |
| CRITICAL alert → auto-intervention | maintenance | 5 AlertToInterventionWorkflow |
| Concurrent deduplication (10 threads) | maintenance | 1 ConcurrencyTest |
| Notification channel routing (severity → channels) | notification | 6 ChannelRoutingPolicy |
| Critical override bypass (preferences) | notification | 15 PreferencesRules |
| Tenant isolation (cross-tenant rejected) | all MVP 2 | 19 TenantIsolation tests |
| Rate limiting enforcement | notification, reporting | 6 RateLimit tests |
| Download token single-use + expiry | reporting | 7 DownloadToken |
| Kafka idempotent consumer (dedup) | maintenance, notification | 4 dedup tests |
| SLA breach detection | maintenance | 5 SlaPolicy |
| Quiet hours + critical override | notification | 15 PreferencesRules |

---

## 5. Infrastructure de Test Partagee

### 5.1 Testcontainers (Singleton Pattern)

```java
public class PostgresContainerConfig {
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("timescale/timescaledb:latest-pg16")
            .withDatabaseName("pyrosense_test");
    static { POSTGRES.start(); }
}

public class KafkaContainerConfig {
    static final KafkaContainer KAFKA =
        new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));
    static { KAFKA.start(); }
}

public class RedisContainerConfig {
    static final GenericContainer<?> REDIS =
        new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    static { REDIS.start(); }
}
```

### 5.2 Profil Test

Chaque service possede `application-test.yml` :
- H2 ou PostgreSQL Testcontainers selon le test
- Kafka embedded ou Testcontainers
- Redis embedded ou Testcontainers
- Flyway active (vraies migrations)
- JaCoCo instrumente

### 5.3 Test Data Builders

Convention : `src/test/java/.../support/TestDataBuilder.java`

```java
public class TestDataBuilder {
    public static Alert anAlert() {
        return Alert.create(
            new AlertId(UUID.randomUUID()),
            new TenantId(UUID.randomUUID()),
            new DeviceId(UUID.randomUUID()),
            AlertSeverity.CRITICAL,
            AlertType.OVERHEATING
        );
    }
}
```

---

## 6. Inventaire des Tests Existants (MVP 2)

### 6.1 Backend - Totaux par Service

| Service | Unit | Use Case | REST | Security | Arch | Integration | Total |
|---------|:----:|:--------:|:----:|:--------:|:----:|:-----------:|:-----:|
| shared-kernel | 77 | — | — | — | 10+ | — | 86 |
| alerting | 46 | 13 | 3 | 8 | 10 | 18 | 98 |
| maintenance | 43 | 18 | 3 | 12 | 10 | 13 | 99 |
| reporting | 43 | 18 | 3 | 7 | 10 | 44 | 125 |
| notification | 56 | 43 | 4 | 17 | 10 | 96 | 226 |
| dashboard | 5 | 5 | 6 | 6 | 6 | 9 | 35* |
| device | 20 | 4 | — | — | 7 | 5 | 32* |
| ingestion | 19 | 10 | — | — | 6 | 3 | 38 |
| signal-analysis | 43 | 9 | — | — | 10 | — | 66* |
| risk-scoring | 37 | 5 | — | — | 8 | — | 58* |
| **Total** | | | | | | | **863+** |

*Certains services n'ont pas encore tous les types de tests (opportunite d'amelioration).

### 6.2 Frontend Angular

| Type | Fichiers | Tests |
|------|:--------:|:-----:|
| Composants | 12 .spec.ts | 52 |
| Services | 5 .spec.ts | 26 |
| Guards | 3 .spec.ts | 9 |
| Interceptors | 4 .spec.ts | 10 |
| Pipes | 2 .spec.ts | 11 |
| E2E (Cypress) | 6 .cy.ts | ~24 |
| **Total** | **32** | **132+** |

---

## 7. Plan d'Amelioration Continue

### Court terme (MVP 2 scope)
- [x] Tests Kafka IT pour alerting, maintenance (Testcontainers)
- [x] Tests tenant isolation pour dashboard, reporting, maintenance, notification
- [x] Tests security (RBAC, rate limiting, anti mass-assignment)
- [x] Tests templates PDF (17 tests ProfessionalPdfRenderer)
- [x] Tests preferences notification avancees (15 regles)
- [x] Tests audit log (pagination, filtres, PII masking)
- [x] Tests frontend (composants, services, guards, interceptors, pipes)

### Moyen terme (post-MVP 2)
- [ ] Contract tests Spring Cloud Contract (API compatibility)
- [ ] Mutation testing (PIT) pour valider qualite des assertions
- [ ] Load tests K6/Gatling (1000 devices, 10 buildings)
- [ ] Chaos testing (reseau, Kafka down, PostgreSQL failover)
- [ ] E2E cross-service Kafka workflows (alert → intervention → notification)

### Long terme (production)
- [ ] Canary testing (traffic mirroring)
- [ ] A/B testing framework (feature flags)
- [ ] Regression visuelle (Percy/Chromatic)
- [ ] Security penetration testing (OWASP ZAP automatise)

---

## 8. Commandes de Reference

```bash
# Backend - tous les tests unitaires
mvn test -B

# Backend - tests d'integration (Docker requis)
mvn verify -Ptest -B -Dsurefire.skip=true

# Backend - couverture JaCoCo
mvn verify -B -Djacoco.skip=false

# Backend - service specifique
mvn test -pl pyrosense-notification-service

# Backend - tests ArchUnit uniquement
mvn test -Dtest="*ArchitectureTest*,*NamingConventionTest*"

# Backend - scan securite OWASP
mvn dependency-check:check -B

# Backend - format code
mvn spotless:check -B
mvn spotless:apply -B

# Frontend - tests unitaires
cd pyrosense-dashboard && npx ng test --watch=false --browsers=ChromeHeadless

# Frontend - tests avec couverture
cd pyrosense-dashboard && npx ng test --watch=false --code-coverage

# Frontend - E2E Cypress
cd pyrosense-dashboard && npx cypress run

# Frontend - lint
cd pyrosense-dashboard && npx ng lint

# CI complet (simule pipeline)
mvn verify -Ptest -Djacoco.skip=false && cd pyrosense-dashboard && npx ng test --watch=false
```
