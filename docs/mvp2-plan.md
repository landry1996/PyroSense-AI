# PyroSense AI — Plan Technique MVP 2

**Objectif:** Dashboard + Reporting + Intervention + Notifications  
**Stack:** Java 21, Spring Boot 3.4.x, Angular 18, Architecture Hexagonale, DDD, PostgreSQL, TimescaleDB, Kafka, Redis

---

## 1. Perimetre precis du MVP 2

### Inclus
- Dashboard web Angular (SPA) pour gestionnaires et operateurs
- Visualisation multi-batiments / multi-tableaux / multi-capteurs
- Affichage temps reel des scores de risque (WebSocket ou polling)
- Gestion des alertes (liste, detail, acknowledge, resolve, comment)
- Planification et suivi des interventions de maintenance
- Generation de rapports PDF (mensuel, compliance, intervention)
- Notifications multi-canal (email, SMS, push navigateur, dashboard)
- Historique complet (alertes, interventions, scores, telemetrie)
- Audit trail (qui a fait quoi, quand)
- RBAC complet (8 roles, methode-level security)
- Multi-tenancy strict (isolation donnees par tenant)

### Exclus (MVP 3+)
- Application mobile native
- ML/AI reelle (utilise les methodes statistiques)
- WebSocket temps reel (MVP 2 utilise polling 30s, WebSocket en V3)
- Integration calendrier externe (Google Calendar, Outlook)
- Facturation / paiement SaaS
- Marketplace de capteurs

## 2. Bounded Contexts concernes

| Bounded Context | Module Backend | Role dans MVP 2 |
|-----------------|---------------|-----------------|
| Identity & Access | pyrosense-identity-service | Auth, RBAC, tenants, users |
| Device Management | pyrosense-device-service | Liste capteurs, statut, localisation |
| Risk Scoring | pyrosense-risk-scoring-service | Scores affiches dans dashboard |
| Alerting | pyrosense-alerting-service | CRUD alertes, acknowledge, resolve |
| Maintenance | pyrosense-maintenance-service | Interventions, planification, resultats |
| Reporting | pyrosense-reporting-service | Generation PDF, telechargement |
| Notification | pyrosense-notification-service | Email/SMS/Push routing |
| Signal Analysis | pyrosense-signal-analysis-service | Historique anomalies, baselines |
| API Gateway | pyrosense-api-gateway | Routing, auth, rate limiting |

**Nouveau module:**
- `pyrosense-dashboard` — Angular SPA (frontend only, communicates via REST API through gateway)

## 3. User Stories

### Gestionnaire de batiment (PROPERTY_MANAGER)
- US-01: En tant que gestionnaire, je veux voir un tableau de bord avec tous mes batiments et leurs scores de risque
- US-02: En tant que gestionnaire, je veux cliquer sur un batiment pour voir le detail par tableau electrique
- US-03: En tant que gestionnaire, je veux voir la liste des alertes ouvertes triees par severite
- US-04: En tant que gestionnaire, je veux acquitter une alerte et ajouter un commentaire
- US-05: En tant que gestionnaire, je veux creer une intervention de maintenance a partir d'une alerte
- US-06: En tant que gestionnaire, je veux suivre l'avancement des interventions planifiees
- US-07: En tant que gestionnaire, je veux generer un rapport mensuel PDF de mon parc
- US-08: En tant que gestionnaire, je veux etre notifie par email quand une alerte CRITICAL arrive
- US-09: En tant que gestionnaire, je veux voir l'historique des scores de risque sur 30/90/365 jours
- US-10: En tant que gestionnaire, je veux filtrer les alertes par batiment, severite, statut

### Electricien (ELECTRICIAN)
- US-11: En tant qu'electricien, je veux voir les interventions qui me sont assignees
- US-12: En tant qu'electricien, je veux demarrer une intervention et saisir un diagnostic
- US-13: En tant qu'electricien, je veux completer une intervention avec un resultat et un impact risque
- US-14: En tant qu'electricien, je veux voir l'historique de telemetrie d'un capteur sur 7 jours
- US-15: En tant qu'electricien, je veux signaler un faux positif sur une alerte

### Administrateur tenant (TENANT_ADMIN)
- US-16: En tant qu'admin, je veux gerer les utilisateurs de mon organisation
- US-17: En tant qu'admin, je veux configurer les seuils d'alerte par batiment
- US-18: En tant qu'admin, je veux voir les statistiques globales (nombre alertes, interventions, cout evite)
- US-19: En tant qu'admin, je veux exporter les donnees en CSV
- US-20: En tant qu'admin, je veux voir l'audit trail (qui a fait quoi)

### Occupant (OCCUPANT)
- US-21: En tant qu'occupant, je veux voir le score de risque de mon logement
- US-22: En tant qu'occupant, je veux etre notifie si une intervention est planifiee chez moi

## 4. Roles utilisateurs

| Role | Acces Dashboard | Alertes | Interventions | Rapports | Notifications | Admin |
|------|----------------|---------|---------------|----------|---------------|-------|
| PLATFORM_ADMIN | Tous tenants | CRUD | CRUD | Tous | Config | Full |
| TENANT_ADMIN | Son tenant | CRUD | CRUD | Tenant | Config | Users/Config |
| PROPERTY_MANAGER | Ses batiments | Read/Acknowledge | Create/Read | Ses batiments | Recevoir | - |
| ELECTRICIAN | Assignations | Read/Comment | Update/Complete | - | Recevoir | - |
| OPERATOR | Dashboard global | Read | Read | Read | Recevoir | - |
| OCCUPANT | Son logement | - | - | - | Recevoir | - |
| INSURER | Rapports | Read | - | Compliance | - | - |
| DEVICE_MANAGER | Capteurs | - | - | - | - | Devices |

## 5. Ecrans du Dashboard

### Layout
- Sidebar navigation (batiments, alertes, interventions, rapports, admin)
- Top bar (user, tenant, notifications bell, logout)
- Content area (responsive)

### Ecrans

| # | Ecran | Route | Description |
|---|-------|-------|-------------|
| 1 | Dashboard Overview | /dashboard | Cartes batiments avec score couleur, alertes actives, KPIs |
| 2 | Building Detail | /buildings/:id | Tableaux electriques, capteurs, scores par circuit |
| 3 | Device Detail | /devices/:id | Graphiques temps reel, historique, baseline, anomalies |
| 4 | Alert List | /alerts | Tableau filtrable (severity, status, building, date) |
| 5 | Alert Detail | /alerts/:id | Timeline, comments, actions (ack, resolve, false-positive) |
| 6 | Intervention List | /interventions | Kanban ou tableau (created, planned, in-progress, completed) |
| 7 | Intervention Detail | /interventions/:id | Infos, diagnostic, resultat, impact risque |
| 8 | Reports | /reports | Generation, liste, telechargement PDF |
| 9 | Notifications | /notifications | Historique des notifications recues |
| 10 | Users (Admin) | /admin/users | CRUD utilisateurs du tenant |
| 11 | Settings (Admin) | /admin/settings | Seuils, preferences notification, config tenant |
| 12 | Audit Log (Admin) | /admin/audit | Journal d'actions (who, what, when) |
| 13 | Login | /login | Keycloak redirect (OIDC) |

## 6. Modules backend necessaires

### Existants (a enrichir)
- `pyrosense-api-gateway` — ajouter routes frontend, CORS Angular
- `pyrosense-alerting-service` — endpoints enrichis (filtres, stats)
- `pyrosense-maintenance-service` — endpoints enrichis (kanban view)
- `pyrosense-reporting-service` — declencher depuis dashboard
- `pyrosense-notification-service` — WebPush + dashboard notifications
- `pyrosense-risk-scoring-service` — historique scores pour graphiques
- `pyrosense-device-service` — enrichir avec localisation batiment/etage
- `pyrosense-identity-service` — CRUD users, Flyway migrations

### Nouveau
- `pyrosense-dashboard` — Angular 18 SPA
  - Modules: auth, dashboard, buildings, devices, alerts, interventions, reports, notifications, admin
  - Libs: Angular Material, ng-charts (Chart.js), Angular CDK, ngx-translate
  - State: NgRx ou signals (Angular 18)
  - HTTP: HttpClient + interceptors (JWT, tenant, correlation-id)

## 7. Endpoints REST

### Alerting Service (enrichissements)
```
GET    /api/v1/alerts?status=&severity=&buildingId=&page=&size=    # Filtres avances
GET    /api/v1/alerts/statistics                                    # Counts par severity/status
GET    /api/v1/alerts/:id/timeline                                  # Historique d'actions sur l'alerte
```

### Maintenance Service (enrichissements)
```
GET    /api/v1/interventions/kanban                                 # Groupe par status
GET    /api/v1/interventions/statistics                             # KPIs interventions
PATCH  /api/v1/interventions/:id/assign                             # Assigner electricien
```

### Risk Scoring Service (enrichissements)
```
GET    /api/v1/risk/devices/:id/history?from=&to=&granularity=     # Historique scores pour graphs
GET    /api/v1/risk/buildings/:id/heatmap                          # Score par circuit (heatmap)
GET    /api/v1/risk/summary                                        # KPIs globaux tenant
```

### Device Service (enrichissements)
```
GET    /api/v1/devices/buildings/:buildingId                       # Devices par batiment
GET    /api/v1/devices/:id/telemetry?from=&to=                    # Historique telemetrie
GET    /api/v1/devices/status/summary                             # Online/offline counts
```

### Reporting Service (enrichissements)
```
POST   /api/v1/reports/on-demand                                   # Generation a la demande
GET    /api/v1/reports/templates                                   # Types disponibles
```

### Identity Service (nouveaux - necessite Flyway migrations)
```
GET    /api/v1/users                                               # Liste users du tenant
POST   /api/v1/users                                               # Creer user
PUT    /api/v1/users/:id                                           # Modifier user
DELETE /api/v1/users/:id                                           # Desactiver user
GET    /api/v1/audit-log?from=&to=&userId=&action=                # Audit trail
```

### Notification Service (enrichissements)
```
GET    /api/v1/notifications/unread-count                          # Badge counter
POST   /api/v1/notifications/:id/mark-read                        # Marquer comme lu
POST   /api/v1/notifications/preferences                          # Preferences canal
```

## 8. Evenements Kafka

### Existants (deja implementes)
| Topic | Producteur | Consommateur | Event |
|-------|-----------|--------------|-------|
| telemetry-events | ingestion | signal-analysis | TelemetryReceivedEvent |
| analysis-events | signal-analysis | risk-scoring | AnomalyDetectedEvent |
| scoring-events | risk-scoring | alerting | RiskThresholdExceededEvent |
| alerting-events | alerting | notification, maintenance | AlertCreatedEvent |
| maintenance-events | maintenance | signal-analysis (feedback) | InterventionCompletedEvent |

### Nouveaux pour MVP 2
| Topic | Producteur | Consommateur | Event |
|-------|-----------|--------------|-------|
| alerting-events | alerting | notification | AlertAcknowledgedEvent |
| alerting-events | alerting | notification | AlertResolvedEvent |
| reporting-events | reporting | notification | ReportGeneratedEvent |
| notification-events | notification | dashboard (SSE/polling) | NotificationCreatedEvent |
| audit-events | all services | identity (audit log) | AuditActionEvent |

## 9. Tables PostgreSQL

### Nouvelles tables (Identity Service — Flyway migrations)
```sql
-- V001__create_users.sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    keycloak_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    UNIQUE(tenant_id, email)
);
CREATE INDEX idx_users_tenant ON users(tenant_id);

-- V002__create_audit_log.sql
CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(255),
    details JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_tenant_time ON audit_log(tenant_id, created_at DESC);
CREATE INDEX idx_audit_user ON audit_log(user_id, created_at DESC);
```

### Tables enrichies (ajouts)
```sql
-- Device Service: V002__add_building_location.sql
ALTER TABLE devices ADD COLUMN building_id UUID;
ALTER TABLE devices ADD COLUMN floor VARCHAR(50);
ALTER TABLE devices ADD COLUMN panel_name VARCHAR(255);
ALTER TABLE devices ADD COLUMN circuit_label VARCHAR(255);
CREATE INDEX idx_devices_building ON devices(building_id);

-- Notification Service: V002__add_read_status.sql
ALTER TABLE notifications ADD COLUMN read_at TIMESTAMPTZ;
ALTER TABLE notifications ADD COLUMN dashboard_visible BOOLEAN DEFAULT true;

-- Risk Scoring Service: V002__add_tenant_id.sql
ALTER TABLE risk_assessments ADD COLUMN tenant_id UUID;
CREATE INDEX idx_ra_tenant_time ON risk_assessments(tenant_id, computed_at DESC);
```

## 10. Regles metier

### Alertes
- Une alerte CRITICAL non acquittee apres 15 min → escalade automatique
- Une alerte CRITICAL non resolue apres 4h → notification relance
- Deduplication : meme device + meme type + meme fenetre → increment occurrence
- Un acquittement necessite un commentaire obligatoire

### Interventions
- Une alerte CRITICAL/HIGH cree automatiquement une intervention
- Une intervention ne peut etre completee sans diagnostic renseigne
- Le resultat d'intervention (defaut confirme / faux positif) → feedback loop scoring
- Priority mapping : CRITICAL alert → URGENT intervention, HIGH → HIGH, MEDIUM → NORMAL

### Scores
- Score recalcule a chaque anomalie detectee (pas de polling periodique)
- Historique conserve 2 ans (TimescaleDB continuous aggregate)
- Trend calculation : comparaison score J-7, J-30, J-90

### Rapports
- Rapport mensuel auto-genere le 1er de chaque mois pour chaque tenant
- Rapport d'intervention genere a la completion de l'intervention
- Rapport compliance annuel (resume + recommandations)
- Signature SHA-256 sur chaque PDF (integrite)

## 11. Regles de securite

- Toute requete API authentifiee via JWT (sauf health check)
- Tenant isolation : chaque requete filtree par tenant_id du JWT
- RBAC method-level : @PreAuthorize sur chaque endpoint
- Pas d'acces cross-tenant meme pour PLATFORM_ADMIN sans switch explicite
- Rate limiting : 60 req/min (standard), 120 req/min (ingestion), 10 req/min (auth)
- Audit logging : toute action de mutation (POST/PUT/DELETE) loggee
- Session : JWT access token 15 min, refresh token 8h
- CORS : domaines autorises configurables par environnement
- CSP : script-src 'self', pas de inline scripts
- File upload : max 10MB, types autorises (PDF, CSV, images)

## 12. Regles de notification

### Routing par severite
| Severite | Canaux | Delai |
|----------|--------|-------|
| CRITICAL | SMS + Email + Push + Dashboard | Immediat |
| HIGH | Email + Push + Dashboard | < 5 min |
| MEDIUM | Email + Dashboard | < 15 min |
| LOW/INFO | Dashboard uniquement | Batch horaire |

### Anti-spam
- Deduplication : meme alerte+device+canal → 1 notification par 30 min max
- Quiet hours : configurable par utilisateur (ex: pas de SMS 22h-7h sauf CRITICAL)
- Unsubscribe : l'utilisateur peut desactiver un canal (sauf CRITICAL)
- Retry : 3 tentatives (30s, 2min, 10min) puis marque FAILED

### Preferences utilisateur
- Chaque utilisateur configure ses canaux preferes
- Respect du consentement RGPD (consentEmail, consentSms, consentPush)
- Fallback : si canal prefere echoue, essayer canal suivant

## 13. Regles de reporting

### Types de rapports
| Type | Frequence | Contenu |
|------|-----------|---------|
| MONTHLY_SUMMARY | Mensuel auto | KPIs, alertes, interventions, tendance risque |
| INTERVENTION_REPORT | A la completion | Diagnostic, resultat, photos, recommandations |
| COMPLIANCE_CERTIFICATE | Annuel ou on-demand | Etat du parc, conformite, historique |
| RISK_ASSESSMENT | On-demand | Score detaille par batiment/tableau |
| INSURANCE_REPORT | Trimestriel | Resume risques, interventions, pour assureur |
| CUSTOM | On-demand | Periode custom, filtres custom |

### Contenu PDF
- En-tete : logo tenant, numero rapport, date, periode
- Corps : tableaux, graphiques (risk trend chart), details
- Pied : signature SHA-256, mention legale, page numbering
- Format : A4, max 20 pages, taille < 5MB

### Retention
- Rapports conserves 5 ans
- Download token : usage unique, expire en 15 min
- Acces : tenant owner + INSURER (pour rapports assurance)

## 14. Strategie de tests

### Frontend (Angular)
- Unit tests : Jasmine/Karma — composants, services, pipes (80% coverage)
- E2E tests : Cypress ou Playwright — parcours critiques
- Visual regression : Chromatic (optionnel)

### Backend (enrichissements)
- Unit tests : nouveaux endpoints, nouvelles regles metier
- Integration tests : Testcontainers (nouveaux repos, migrations)
- Contract tests : Spring Cloud Contract (Angular <-> API)
- Security tests : @WithMockUser, tenant isolation assertions
- Performance : k6 ou Gatling pour endpoints dashboard (100 users concurrent)

### Architecture
- ArchUnit : nouvelles regles si nouveaux packages
- API compatibility : OpenAPI diff (breaking change detection)

## 15. Roadmap d'implementation etape par etape

### Sprint 1 (Semaine 1-2) : Fondations Angular
- [ ] Creer le projet Angular 18 (`pyrosense-dashboard`)
- [ ] Setup : Angular Material, routing, auth (Keycloak OIDC)
- [ ] Layout : sidebar, topbar, responsive shell
- [ ] Ecran login (redirect Keycloak)
- [ ] Service HTTP avec interceptors (JWT, tenant, correlation-id)
- [ ] Guard de route (auth + role-based)
- [ ] Ecran Dashboard Overview (mocked data)

### Sprint 2 (Semaine 3-4) : Batiments + Devices
- [ ] Backend : ajouter building_id aux devices (migration Flyway)
- [ ] Backend : endpoint GET /devices/buildings/:buildingId
- [ ] Backend : endpoint GET /devices/:id/telemetry?from&to
- [ ] Frontend : ecran Building Detail (liste tableaux, scores par circuit)
- [ ] Frontend : ecran Device Detail (graphiques Chart.js, historique)
- [ ] Frontend : composant RiskScoreGauge (indicateur couleur)

### Sprint 3 (Semaine 5-6) : Alertes
- [ ] Backend : enrichir AlertController (filtres avances, timeline)
- [ ] Frontend : ecran Alert List (tableau Angular Material, filtres, pagination)
- [ ] Frontend : ecran Alert Detail (timeline, comments, actions)
- [ ] Frontend : action acknowledge (modale commentaire obligatoire)
- [ ] Frontend : action resolve / false-positive
- [ ] Tests : WebMvcTest AlertController, Cypress E2E alert flow

### Sprint 4 (Semaine 7-8) : Interventions
- [ ] Backend : enrichir InterventionController (kanban endpoint, assign)
- [ ] Frontend : ecran Intervention List (vue Kanban drag-drop ou tableau)
- [ ] Frontend : ecran Intervention Detail (diagnostic form, resultat)
- [ ] Frontend : creation intervention depuis alerte
- [ ] Frontend : completer intervention (formulaire resultat + impact risque)
- [ ] Tests : E2E intervention lifecycle

### Sprint 5 (Semaine 9-10) : Rapports + Notifications
- [ ] Backend : endpoint rapport on-demand, templates
- [ ] Frontend : ecran Reports (liste, generation, telechargement)
- [ ] Backend : notification mark-read, unread-count, preferences
- [ ] Frontend : notification bell (badge unread count)
- [ ] Frontend : ecran Notifications (historique)
- [ ] Frontend : ecran preferences notification
- [ ] Tests : PDF generation E2E, notification delivery

### Sprint 6 (Semaine 11-12) : Admin + Polish
- [ ] Backend : Identity service Flyway migrations + JDBC repos
- [ ] Backend : audit-log endpoint
- [ ] Frontend : ecran Users (CRUD, invite)
- [ ] Frontend : ecran Settings (seuils, tenant config)
- [ ] Frontend : ecran Audit Log (tableau filtrable)
- [ ] Security hardening : test penetration OWASP
- [ ] Performance : load test k6 (100 users, 1000 devices)
- [ ] Documentation : update API docs, user guide
- [ ] Tests : full regression suite

---

## Architecture Frontend

```
pyrosense-dashboard/
├── src/
│   ├── app/
│   │   ├── core/              # Auth, interceptors, guards, services globaux
│   │   ├── shared/            # Composants reutilisables (risk-gauge, severity-badge, etc.)
│   │   ├── features/
│   │   │   ├── dashboard/     # Overview page
│   │   │   ├── buildings/     # Building list + detail
│   │   │   ├── devices/       # Device detail + telemetry charts
│   │   │   ├── alerts/        # Alert list + detail
│   │   │   ├── interventions/ # Intervention list + detail + kanban
│   │   │   ├── reports/       # Report generation + list
│   │   │   ├── notifications/ # Notification list + preferences
│   │   │   └── admin/         # Users, settings, audit
│   │   └── app.routes.ts
│   ├── environments/
│   └── styles/
├── angular.json
├── package.json
└── Dockerfile
```

---

*Ce plan ne constitue pas un engagement de livraison. Les estimations dependent de la taille de l'equipe et de la disponibilite des dependances (Keycloak configure, services backend stables).*
