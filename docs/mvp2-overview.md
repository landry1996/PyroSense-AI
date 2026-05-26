# MVP 2 - Vue d'Ensemble

## Statut

**Prototype logiciel fonctionnel** — pas un produit certifie, pas un pilote terrain.

Ce MVP 2 est un logiciel de demonstration qui valide l'architecture, les flux metier et les interfaces utilisateur. Il ne peut pas etre deploye en production sans :
- Capteurs reels et firmware certifie
- Certification electrique (IEC 61439, NF C 15-100)
- Audit de securite professionnel
- Infrastructure cloud haute disponibilite
- Assurance RC Pro

## Objectifs

| # | Objectif | Statut |
|---|----------|--------|
| 1 | Dashboard temps reel pour gestionnaires immobiliers | FAIT |
| 2 | Cycle de vie complet des interventions de maintenance | FAIT |
| 3 | Generation de rapports PDF professionnels | FAIT |
| 4 | Notifications multi-canal avec preferences | FAIT |
| 5 | Integration event-driven entre services (Kafka) | FAIT |
| 6 | Securite renforcee (RBAC, tenant isolation, audit) | FAIT |
| 7 | Observabilite complete (metriques, tracing, alertes) | FAIT |
| 8 | Infrastructure Docker locale complete | FAIT |

## Perimetre

### Inclus dans le MVP 2

- **Dashboard** : 12 ecrans Angular (overview, batiments, equipements, alertes, interventions, rapports, notifications, parametres, admin)
- **Maintenance** : workflow alerte → intervention (auto CRITICAL, recommandation WARNING), SLA, diagnostic, feedback loop
- **Reporting** : 4 types de rapports PDF (bilan mensuel, attestation, alerte critique, intervention), telechargement securise
- **Notifications** : 5 canaux (EMAIL, SMS, PUSH, WEBHOOK, DASHBOARD), 9 templates, deduplication, quiet hours, retry
- **Evenements Kafka** : 6 topics, envelope IntegrationEvent enrichi (version, correlation, causation, tenant), DLQ, idempotence
- **Securite** : 8 roles, tenant isolation, rate limiting, anti mass-assignment, audit transversal, masquage PII
- **Observabilite** : metriques metier Micrometer, tracing OpenTelemetry, logs JSON, alertes Prometheus
- **DevOps** : Docker Compose (profil mvp2), scripts demarrage, Grafana dashboards

### Hors perimetre MVP 2

| Element | Raison | Prevu pour |
|---------|--------|-----------|
| Capteurs physiques (hardware IoT) | Necessite selection composants + firmware | Pilote terrain |
| Certification electrique | Dossier IEC 61439 / NF C 15-100 | Pre-production |
| Machine Learning (LSTM, Isolation Forest) | Necessite donnees terrain reelles (>6 mois) | MVP 3 |
| Haute disponibilite (multi-AZ, failover) | Cloud managed, pas necessaire en dev | Production |
| Application mobile native | Push via PWA ou Firebase en MVP 3 | MVP 3 |
| Facturation / abonnement | Module commercial a definir | Commercialisation |
| Consentement RGPD (popup, registre) | Integration juridique | Pilote terrain |
| Tests de charge (>1000 devices) | Requiert infra cloud | Pre-production |

## Dependances MVP 1

Le MVP 2 s'appuie sur les composants MVP 1 suivants (deja livres) :

| Composant MVP 1 | Usage dans MVP 2 |
|-----------------|------------------|
| Ingestion Service (MQTT + REST) | Source de donnees telemetrie |
| Signal Analysis Service | Detection anomalies → evenements scoring |
| Risk Scoring Service | Calcul score risque → evenements alerting |
| Alerting Service | Cycle de vie alertes, consomme par maintenance + notification |
| Device Service | Registre equipements, statuts |
| Shared Kernel | IntegrationEvent, IDs fortement types, exceptions, value objects |
| API Gateway | Routage, JWT, rate limiting, headers securite |
| Identity Service | Roles, permissions, tenants |
| TimescaleDB schema | Hypertables telemetrie, aggregats continus |
| Docker Compose infra | PostgreSQL, Redis, Kafka, Keycloak, monitoring |

## Architecture MVP 2

```
┌─────────────────────────────────────────────────────────────────┐
│  Frontend Angular (port 4200)                                    │
│  Dashboard SPA → nginx → proxy /api/ vers gateway               │
└──────────────────────────────┬──────────────────────────────────┘
                               │ REST
┌──────────────────────────────▼──────────────────────────────────┐
│  API Gateway (port 8080)                                         │
│  JWT, RBAC, tenant isolation, rate limiting, CORS, headers       │
└───┬──────────┬──────────┬──────────┬──────────┬────────────────┘
    │          │          │          │          │
┌───▼───┐  ┌──▼───┐  ┌───▼───┐  ┌──▼────┐  ┌─▼──────────┐
│Dashbrd│  │Alert-│  │Maint- │  │Report-│  │Notification│
│:8088  │  │ing   │  │enance │  │ing    │  │:8087       │
│       │  │:8086 │  │:8089  │  │:8091  │  │            │
└───┬───┘  └──┬───┘  └───┬───┘  └──┬────┘  └─┬──────────┘
    │         │           │          │          │
    │    ┌────▼───────────▼──────────▼──────────▼────┐
    │    │  Apache Kafka                              │
    │    │  pyrosense.alerts.events                   │
    │    │  pyrosense.maintenance.events              │
    │    │  pyrosense.reports.events                  │
    │    │  pyrosense.notifications.events            │
    │    │  pyrosense.dead-letter.events              │
    │    └────────────────────────────────────────────┘
    │
┌───▼────────────────────────────────────────────────┐
│  PostgreSQL + Redis                                 │
│  9 databases + cache invalidation                   │
└────────────────────────────────────────────────────┘
```

## Roles et Acces

| Role | Dashboard | Alertes | Interventions | Rapports | Notifications | Admin |
|------|:---------:|:-------:|:-------------:|:--------:|:-------------:|:-----:|
| PLATFORM_ADMIN | Tout | Tout | Tout | Tout | Tout | Tout |
| TENANT_ADMIN | Tout | Tout | Tout | Tout | Tout | Tenant |
| PROPERTY_MANAGER | Tout | Tout | Tout | Tout | Tout | Non |
| ELECTRICIAN | Partiel | Lecture | Assignees | Non | Non | Non |
| INSURANCE_PARTNER | Non | Non | Non | Accessibles | Non | Non |
| OCCUPANT | Partiel | Lecture | Non | Non | Non | Non |
| SUPPORT_READONLY | Partiel | Lecture | Lecture | Lecture | Lecture | Non |
| DEVICE_MANAGER | Non | Non | Non | Non | Non | Devices |

## Documentation Detaillee

| Document | Contenu |
|----------|---------|
| [Dashboard](dashboard-api.md) + [Frontend](frontend-dashboard.md) | Ecrans, composants, API, roles, UX |
| [Maintenance](maintenance-service.md) + [Workflow](alert-to-intervention-workflow.md) | Cycle de vie, regles metier, SLA |
| [Reporting](reporting-service.md) + [PDF Templates](pdf-templates.md) | Types rapports, contenu, securite, limites legales |
| [Notifications](notification-service.md) + [Templates](notification-templates.md) + [Regles](notification-rules.md) | Canaux, templates, preferences, anti-spam |
| [Securite MVP 2](mvp2-security.md) + [Securite globale](security.md) + [Audit](audit.md) | RBAC, tenant isolation, audit, PII |
| [Evenements Kafka](mvp2-events.md) | Topics, events, versioning, idempotence |
| [Tests](mvp2-testing-strategy.md) + [Tests globaux](testing-strategy.md) | Strategie, lancement, couverture |
| [DevOps Local](mvp2-local-run.md) + [DevOps global](devops.md) | Docker, variables, monitoring, demarrage |
| [Observabilite](mvp2-observability.md) | Metriques, tracing, logs, alertes |
| [Production Readiness](production-readiness-mvp2.md) | Checklist avant pilote reel |

## Chiffres Cles

| Indicateur | Valeur |
|-----------|--------|
| Services backend | 11 microservices Java 21 |
| Frontend | Angular 18 SPA (12 ecrans) |
| Tests backend | 863+ (unit + integration + architecture + security) |
| Tests frontend | 132+ (components + services + guards + E2E) |
| Topics Kafka | 6 (dont 1 DLQ) |
| Alertes Prometheus | 21 regles (14 existantes + 7 MVP 2) |
| Dashboards Grafana | 4 provisionnes |
| Roles RBAC | 8 roles, 34 permissions |
| Couverture cible | Domain >95%, Use cases >85% |

## Ce que ce MVP 2 N'EST PAS

1. **Pas un produit certifie** — Aucune certification electrique. Ne peut pas remplacer un diagnostic professionnel.
2. **Pas un pilote terrain** — Utilise des donnees simulees, pas de capteurs reels.
3. **Pas pret pour la production** — Pas de HA, pas de DR, secrets en dur pour le dev local.
4. **Pas une garantie anti-incendie** — Le systeme detecte des anomalies statistiques, il ne predit pas les incendies avec certitude.
5. **Pas conforme RGPD** — Le mecanisme de consentement et le registre de traitement ne sont pas implementes.

## Prochaines Etapes

Voir [Production Readiness MVP 2](production-readiness-mvp2.md) pour la checklist avant pilote terrain, et le TODO.md racine pour la roadmap complete.
