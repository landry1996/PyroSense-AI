# MVP 3 — Overview

> **AVERTISSEMENT** : Ce projet est un prototype logiciel et matériel expérimental.
> Il n'est **pas certifié** pour la surveillance électrique (IEC 61439, NF C 15-100).
> Toute installation sur un tableau électrique réel doit être réalisée **exclusivement par un électricien qualifié habilité B2V minimum**.
> PyroSense ne remplace pas un système de sécurité incendie certifié.

## Objectifs

Le MVP 3 prépare la transition du simulateur logiciel vers un **pilote terrain avec capteurs réels**. Il couvre :

1. **Protocole IoT sécurisé** — Communication MQTT v1 entre capteurs ESP32-S3 et cloud
2. **Sécurité device-cloud** — HMAC-SHA256, anti-replay, provisioning, révocation
3. **Pipeline d'ingestion renforcé** — Validation, qualité des données, rate limiting
4. **Firmware prototype** — Architecture C++ modulaire pour ESP32-S3 (ESP-IDF 5.x)
5. **Observabilité capteur réel** — Métriques spécifiques, alertes, dashboards Grafana
6. **Programme pilote** — Backend de gestion pilote, monitoring terrain, KPIs
7. **Dashboard technique** — Vue santé capteur, qualité télémétrie, sécurité device
8. **CI/CD complet** — Pipeline qualité backend + frontend + firmware
9. **Documentation opérationnelle** — Procédures installation, monitoring quotidien, rapport final

## Périmètre

### Inclus dans MVP 3

| Domaine | Livrables |
|---------|-----------|
| Protocole | MQTT v1 topics, payloads JSON schéma v1.0, versioning |
| Sécurité IoT | HMAC signature, nonce, sequenceNumber, timestamp tolerance, rate limiting per device |
| Provisioning | Claim token (single-use, 24h), credential rotation, révocation immédiate |
| Ingestion | ProtocolValidationPipeline (5 étapes), PayloadValidator, AntiReplayGuard |
| Firmware | 7 modules C++ (sensors, signal_processing, telemetry, connectivity, security, storage, diagnostics) |
| Firmware tests | 43 tests host (CMake, C++17, sans ESP-IDF) |
| Backend pilote | PilotProgram domain (state machine), REST API (8 endpoints), KPI computation |
| Dashboard technique | 4 écrans Angular (overview, telemetry quality, security, pilot monitoring) |
| Observabilité | 14 métriques MVP3, 10 alertes Prometheus, 4 dashboards Grafana |
| CI/CD | 7 jobs GitHub Actions (backend, frontend, firmware, security, docker, release, deploy) |
| Qualité données | Score qualité, SignalQualityScore, TelemetryQualityValidator |
| Documentation | 15 documents techniques + opérationnels |

### Hors périmètre MVP 3

| Élément | Raison | Planifié pour |
|---------|--------|---------------|
| mTLS X.509 | Nécessite ATECC608B + broker EMQX configuré | MVP 4 |
| CBOR / LZ4 compression | Optimisation prématurée pour 10 capteurs | Edge-Cloud Phase |
| ML Isolation Forest / LSTM | Pas assez de données terrain | Post-pilote 6 mois |
| OTA firmware | Nécessite partitions A/B validées sur hardware | Sprint 3.3+ |
| Multi-region / HA | Hors budget pilote | Production |
| Certification IEC/NF C | Processus long, budget séparé | Post-pilote |
| Application mobile | Focus sur dashboard web pour pilote | MVP 4 |
| Facturation / SaaS | Prototype non commercial | Phase commercialisation |

## Prérequis MVP 1

Le MVP 3 s'appuie sur l'infrastructure MVP 1 complète :

| Service | Dépendance MVP 3 |
|---------|------------------|
| Shared Kernel | IDs typés, DomainEvent, ValueObjects, PayloadIntegrity |
| Device Service | Device aggregate, provisioning, credential management |
| Ingestion Service | MQTT listener, telemetry storage, event publishing |
| Signal Analysis | Anomaly detection, baseline profiles |
| Risk Scoring | Risk assessment, scoring weights |
| Alerting | Alert lifecycle, deduplication |
| Identity | JWT, roles, tenant isolation, device credentials |
| TimescaleDB | Hypertables, continuous aggregates, retention |
| Kafka | Event bus (telemetry-events, analysis-events, scoring-events) |

## Prérequis MVP 2

| Service | Dépendance MVP 3 |
|---------|------------------|
| Dashboard Service | Read models, ObservabilityConfig, Kafka cache invalidation |
| Maintenance Service | Intervention lifecycle, electrician assignment |
| Notification Service | Alert notifications, multi-channel delivery |
| Reporting Service | Compliance reports, PDF generation |
| Frontend Dashboard | Component library, routing, services, guards |
| Security Hardening | @PreAuthorize, tenant isolation, anti mass-assignment |
| Observability | Prometheus stack, Grafana dashboards, structured logging |

## Architecture Logique MVP 3

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CLOUD (Java 21 / Spring Boot 3.4)            │
│                                                                     │
│  ┌─────────────┐   ┌──────────────┐   ┌─────────────────────────┐  │
│  │ API Gateway │   │ Dashboard    │   │ Identity Service        │  │
│  │             │   │ Service      │   │ (credentials, tokens)   │  │
│  └─────────────┘   └──────────────┘   └─────────────────────────┘  │
│         │                  │                      │                  │
│  ┌──────▼──────────────────▼──────────────────────▼───────────────┐ │
│  │               Ingestion Service                                │ │
│  │  ┌──────────────────────────────────────────────────────────┐  │ │
│  │  │  ProtocolValidationPipeline                              │  │ │
│  │  │  [Rate Limit] → [Schema] → [Device Status] → [HMAC] →  │  │ │
│  │  │  [Anti-Replay] → [Quality] → [Ingest] → [Kafka]        │  │ │
│  │  └──────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────┘ │
│         │                                                           │
│  ┌──────▼──────────┐   ┌──────────────┐   ┌────────────────────┐   │
│  │ Signal Analysis │──►│ Risk Scoring │──►│ Alerting           │   │
│  └─────────────────┘   └──────────────┘   └────────────────────┘   │
│                                                                     │
└─────────────────────────────┬───────────────────────────────────────┘
                              │ MQTT v1 (TLS)
                              │
┌─────────────────────────────▼───────────────────────────────────────┐
│                        EDGE (ESP32-S3 / ESP-IDF 5.x)                │
│                                                                     │
│  ┌──────────┐  ┌────────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │ Sensors  │──►│ Signal    │──►│ Telemetry│──►│ MQTT Client     │  │
│  │ (ADC)   │  │ Processing │  │ Builder  │  │ (HMAC signed)   │  │
│  └──────────┘  └────────────┘  └──────────┘  └──────────────────┘  │
│                                                                     │
│  ┌──────────────────┐  ┌────────────────┐  ┌────────────────────┐  │
│  │ Offline Queue    │  │ State Machine  │  │ Security (HMAC,   │  │
│  │ (SPIFFS 72h)    │  │ (7 states)     │  │  nonce, certs*)   │  │
│  └──────────────────┘  └────────────────┘  └────────────────────┘  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
  * mTLS planifié Phase 4
```

## Distinction des Phases

| Phase | Nature | Certification | Risque électrique |
|-------|--------|---------------|-------------------|
| MVP 1-2 | **Prototype logiciel** (simulateur) | Non | Aucun (données fictives) |
| MVP 3 firmware | **Prototype matériel** (labo) | Non | Basse tension uniquement |
| MVP 3 pilote | **Pilote terrain contrôlé** (10 capteurs) | Non | Installation par électricien habilité |
| Post-MVP 3 | **Pré-industrialisation** | En cours (IEC 61439) | Tests CEM + thermiques |
| Production | **Produit commercial** | Certifié | Conforme NF C 15-100 |

## Chiffres Clés

| Métrique | Valeur |
|----------|--------|
| Services backend | 12 modules Maven |
| Tests backend | ~900+ (unit + integration + security) |
| Tests firmware | 43 (host, C++17) |
| Alertes Prometheus | 24 rules (7 groupes + mvp3) |
| Dashboards Grafana | 8 (platform + ingestion + mvp2 + 4 mvp3) |
| Endpoints REST | 80+ |
| Rôles RBAC | 8 (34 permissions) |
| Documents | 60+ markdown |
