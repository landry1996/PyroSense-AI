# MVP 2 — Production Readiness Checklist

## Statut Actuel : PROTOTYPE

Ce document liste les criteres a satisfaire pour passer du prototype logiciel au pilote terrain, puis a la production.

---

## Legende

| Icone | Signification |
|:-----:|---------------|
| DONE | Fait et verifie |
| TODO | A faire |
| N/A | Non applicable a cette etape |

---

## 1. Securite

| # | Critere | Prototype | Pilote | Production |
|---|---------|:---------:|:------:|:----------:|
| 1.1 | RBAC applique sur tous les endpoints | DONE | DONE | DONE |
| 1.2 | Tenant isolation validee (tests d'isolation) | DONE | DONE | DONE |
| 1.3 | Pas de tenant spoofing possible (TenantContext.require) | DONE | DONE | DONE |
| 1.4 | Notifications critiques non desactivables | DONE | DONE | DONE |
| 1.5 | Electricien ne voit que ses interventions | DONE | DONE | DONE |
| 1.6 | Rapports proteges (download token single-use 15min) | DONE | DONE | DONE |
| 1.7 | Anti mass-assignment (@AllowedFields) | DONE | DONE | DONE |
| 1.8 | Audit trail (actions sensibles loguees) | DONE | DONE | DONE |
| 1.9 | Pas de secret dans le code source | DONE | DONE | DONE |
| 1.10 | PII masquees dans les logs | DONE | DONE | DONE |
| 1.11 | Pentest professionnel | N/A | TODO | DONE |
| 1.12 | Rotation des secrets (Vault/Secrets Manager) | N/A | TODO | DONE |
| 1.13 | mTLS entre services | N/A | N/A | TODO |
| 1.14 | WAF devant le gateway | N/A | TODO | DONE |
| 1.15 | RGPD (consentement, registre de traitement) | N/A | TODO | DONE |

---

## 2. Architecture & Code

| # | Critere | Prototype | Pilote | Production |
|---|---------|:---------:|:------:|:----------:|
| 2.1 | Architecture hexagonale respectee (ArchUnit) | DONE | DONE | DONE |
| 2.2 | Pas de logique metier dans les controllers | DONE | DONE | DONE |
| 2.3 | Domaine sans dependance Spring | DONE | DONE | DONE |
| 2.4 | Controllers n'injectent que des ports (pas de repositories) | DONE | DONE | DONE |
| 2.5 | Event-driven Kafka (DLQ, idempotence, retry) | DONE | DONE | DONE |
| 2.6 | Pagination SQL (LIMIT/OFFSET) | TODO | TODO | DONE |
| 2.7 | Circuit breaker Redis (Resilience4j) | TODO | TODO | DONE |
| 2.8 | Health checks Spring Boot Actuator | DONE | DONE | DONE |

---

## 3. Tests

| # | Critere | Prototype | Pilote | Production |
|---|---------|:---------:|:------:|:----------:|
| 3.1 | Tests unitaires domaine (>95%) | DONE | DONE | DONE |
| 3.2 | Tests integration (Testcontainers) | DONE | DONE | DONE |
| 3.3 | Tests securite (access matrix, tenant isolation) | DONE | DONE | DONE |
| 3.4 | Tests architecture (ArchUnit) | DONE | DONE | DONE |
| 3.5 | Tests de charge (>1000 devices) | N/A | TODO | DONE |
| 3.6 | Tests chaos (service down, network partition) | N/A | N/A | TODO |
| 3.7 | Tests E2E frontend (Cypress/Playwright) | TODO | TODO | DONE |
| 3.8 | Tests mutation (PIT) | N/A | N/A | TODO |

---

## 4. Observabilite

| # | Critere | Prototype | Pilote | Production |
|---|---------|:---------:|:------:|:----------:|
| 4.1 | Metriques metier Micrometer (Counter, Timer, Gauge) | DONE | DONE | DONE |
| 4.2 | Prometheus scraping configure | DONE | DONE | DONE |
| 4.3 | Alertes Prometheus (21 regles) | DONE | DONE | DONE |
| 4.4 | Tracing distribue OpenTelemetry | DONE | DONE | DONE |
| 4.5 | Logs structures JSON (profil docker/prod) | DONE | DONE | DONE |
| 4.6 | Correlation ID propage (X-Correlation-Id) | DONE | DONE | DONE |
| 4.7 | Grafana dashboards provisionnes | DONE | DONE | DONE |
| 4.8 | Alerting PagerDuty/OpsGenie integre | N/A | TODO | DONE |
| 4.9 | Log aggregation centralisee (ELK/Loki) | DONE | DONE | DONE |
| 4.10 | SLI/SLO definis et monitores | N/A | TODO | DONE |

---

## 5. Infrastructure

| # | Critere | Prototype | Pilote | Production |
|---|---------|:---------:|:------:|:----------:|
| 5.1 | Docker Compose fonctionnel | DONE | DONE | N/A |
| 5.2 | Kubernetes manifests (Helm/Kustomize) | N/A | TODO | DONE |
| 5.3 | PostgreSQL manage (RDS/Cloud SQL) | N/A | TODO | DONE |
| 5.4 | Kafka manage (MSK/Confluent/Redpanda) | N/A | TODO | DONE |
| 5.5 | Redis manage (ElastiCache/Memorystore) | N/A | TODO | DONE |
| 5.6 | Stockage objets pour PDFs (S3/GCS) | N/A | TODO | DONE |
| 5.7 | TLS/HTTPS (Let's Encrypt ou ACM) | N/A | TODO | DONE |
| 5.8 | DNS configure | N/A | TODO | DONE |
| 5.9 | CDN pour frontend | N/A | N/A | TODO |
| 5.10 | Haute disponibilite (multi-AZ) | N/A | N/A | DONE |
| 5.11 | Disaster recovery (RPO<1h, RTO<4h) | N/A | N/A | DONE |
| 5.12 | Backups automatises + test restore | N/A | TODO | DONE |

---

## 6. IoT / Hardware

| # | Critere | Prototype | Pilote | Production |
|---|---------|:---------:|:------:|:----------:|
| 6.1 | Capteurs selectionnes et testes | N/A | TODO | DONE |
| 6.2 | Gateway IoT (ESP32/RPi) | N/A | TODO | DONE |
| 6.3 | Firmware MQTT developpe | N/A | TODO | DONE |
| 6.4 | Provisioning securise (certificats device) | N/A | TODO | DONE |
| 6.5 | Calibration seuils sur donnees reelles | N/A | TODO | DONE |
| 6.6 | Tests sur banc electrique en labo | N/A | TODO | DONE |

---

## 7. Certification & Legal

| # | Critere | Prototype | Pilote | Production |
|---|---------|:---------:|:------:|:----------:|
| 7.1 | Dossier IEC 61439 | N/A | N/A | TODO |
| 7.2 | NF C 15-100 | N/A | N/A | TODO |
| 7.3 | Marquage CE (CEM + basse tension) | N/A | N/A | TODO |
| 7.4 | Assurance RC Pro | N/A | TODO | DONE |
| 7.5 | Conformite RGPD complete | N/A | TODO | DONE |

---

## 8. Operations

| # | Critere | Prototype | Pilote | Production |
|---|---------|:---------:|:------:|:----------:|
| 8.1 | Runbooks operationnels | N/A | TODO | DONE |
| 8.2 | On-call rotation | N/A | TODO | DONE |
| 8.3 | Incident management process | N/A | TODO | DONE |
| 8.4 | Change management | N/A | N/A | TODO |
| 8.5 | Capacity planning | N/A | N/A | TODO |

---

## Score par Etape

| Etape | Criteres requis | Criteres faits | Progression |
|-------|:--------------:|:--------------:|:-----------:|
| Prototype → Pilote | ~20 TODO | 0 faits | 0% |
| Pilote → Production | ~15 TODO supplementaires | 0 faits | 0% |

---

## Prochaine Action

**Immediate :** Planifier la phase laboratoire (mois 1-3) avec selection capteurs et validation banc electrique.

Voir `docs/pilot-transition-plan.md` pour le calendrier detaille et `docs/audit-mvp2.md` pour les problemes identifies et corriges.
