# MVP 3 — Production Readiness Checklist (Pre-Pilot)

> **AVERTISSEMENT** : Cette checklist prépare un **pilote terrain contrôlé** (10 capteurs, 1 bâtiment).
> Ce n'est PAS une mise en production commerciale.
> Toute installation physique requiert un **électricien qualifié habilité B2V minimum**.

## Légende

- ✅ Done — Implémenté et testé
- ⚠️ Partial — Implémenté mais nécessite validation terrain
- ❌ Not started — Planifié pour phase ultérieure
- 🔒 Security — Item critique sécurité

## 1. Sécurité IoT

| # | Item | Status | Notes |
|---|------|--------|-------|
| 1.1 | 🔒 HMAC-SHA256 signature sur chaque message | ✅ | SignatureVerifier, constant-time comparison |
| 1.2 | 🔒 Anti-replay (nonce + sequence + messageId) | ✅ | AntiReplayGuard, 3 couches Redis |
| 1.3 | 🔒 Device provisioning (claim token single-use) | ✅ | DeviceProvisioningService, rate limited |
| 1.4 | 🔒 Credential rotation | ✅ | DeviceCredential.rotate(), version tracked |
| 1.5 | 🔒 Device revocation (immediate) | ✅ | Pipeline step 2, DEVICE_REVOKED rejection |
| 1.6 | 🔒 Topic-payload coherence | ✅ | PayloadValidator topicId == payloadId |
| 1.7 | 🔒 Per-device rate limiting | ✅ | DeviceRateLimiter 120 msg/60s |
| 1.8 | 🔒 No secrets in logs | ✅ | IoTSecurityAuditor masks IDs, sanitizes |
| 1.9 | 🔒 No plaintext credential storage | ✅ | SHA-256 hash only, key shown once |
| 1.10 | MQTT broker TLS (port 8883) | ⚠️ | Configuré, à valider en pilote |
| 1.11 | mTLS X.509 (secure element) | ❌ | Planifié Phase 4 |
| 1.12 | MQTT ACL per device | ❌ | Nécessite EMQX, planifié |

## 2. Pipeline Ingestion

| # | Item | Status | Notes |
|---|------|--------|-------|
| 2.1 | Schema validation (v1.0 whitelist) | ✅ | PayloadValidator, all ranges checked |
| 2.2 | Timestamp tolerance (±300s / 72h drain) | ✅ | Configurable via constants |
| 2.3 | Payload size limit (8KB) | ✅ | MQTT + HTTP filter |
| 2.4 | DLQ for failed messages | ✅ | Kafka ingestion-dlq topic |
| 2.5 | Idempotency (messageId dedup) | ✅ | Redis, 24h TTL |
| 2.6 | Backpressure handling | ✅ | PayloadSizeLimitFilter, processing pool |
| 2.7 | Quality assessment | ✅ | TelemetryQualityValidator, SignalQualityScore |
| 2.8 | Clock drift detection | ✅ | DeviceClockDriftDetector, event published |

## 3. Backend Services

| # | Item | Status | Notes |
|---|------|--------|-------|
| 3.1 | All 12 services compile (Java 21) | ✅ | Maven multi-module |
| 3.2 | Unit tests passing (~900+) | ✅ | Surefire |
| 3.3 | Integration tests passing | ✅ | Failsafe, H2/embedded Kafka |
| 3.4 | ArchUnit compliance (hexagonal rules) | ✅ | 10+ rules per service |
| 3.5 | Security tests (RBAC, tenant isolation) | ✅ | @WebMvcTest, @WithMockUser |
| 3.6 | Pilot management backend | ✅ | PilotProgram, 8 endpoints, KPIs |
| 3.7 | Device technical dashboard backend | ✅ | 4 query endpoints, StubReadModel |
| 3.8 | OWASP Dependency Check | ✅ | Fail on CVSS >= 8 |

## 4. Frontend Dashboard

| # | Item | Status | Notes |
|---|------|--------|-------|
| 4.1 | Device technical overview | ✅ | KPI cards, seuils couleur |
| 4.2 | Telemetry quality view | ✅ | Charts, rejection table |
| 4.3 | Device security status | ✅ | Credential badge, compteurs |
| 4.4 | Pilot monitoring dashboard | ✅ | KPIs, device table, incidents |
| 4.5 | Role-based route guards | ✅ | PLATFORM_ADMIN, TENANT_ADMIN, PROPERTY_MANAGER, SUPPORT_READONLY |
| 4.6 | Loading/error/empty states | ✅ | Skeleton loaders, error messages |
| 4.7 | Angular production build | ✅ | AOT, tree-shaking |

## 5. Firmware

| # | Item | Status | Notes |
|---|------|--------|-------|
| 5.1 | Architecture modulaire (7 modules) | ✅ | C++17, CMake |
| 5.2 | State machine (7 états) | ✅ | BOOTING → ACTIVE flow |
| 5.3 | Host unit tests (43 tests) | ✅ | CTest, sans ESP-IDF |
| 5.4 | HMAC signing module | ✅ | hmac_signer.cpp |
| 5.5 | Nonce generation | ✅ | nonce_generator.cpp |
| 5.6 | Offline queue (SPIFFS 72h) | ✅ | offline_queue.cpp, drain algorithm |
| 5.7 | Signal quality self-diagnostic | ✅ | signal_quality.cpp (0-100) |
| 5.8 | Compilation ESP-IDF complète | ⚠️ | Nécessite IDF_PATH + hardware |
| 5.9 | Test hardware 24h (stabilité) | ❌ | Nécessite ESP32-S3 DevKit |
| 5.10 | OTA A/B partitions | ❌ | Planifié Sprint 3.3+ |

## 6. Observabilité

| # | Item | Status | Notes |
|---|------|--------|-------|
| 6.1 | 14 métriques MVP3 (Micrometer) | ✅ | Mvp3ObservabilityConfig |
| 6.2 | 10 alertes Prometheus (pyrosense-mvp3) | ✅ | alert-rules.yml |
| 6.3 | 4 dashboards Grafana | ✅ | fleet-health, pilot, security, quality |
| 6.4 | Structured logging (JSON, MDC) | ✅ | logback-spring.xml, 8 MDC fields |
| 6.5 | Distributed tracing (OTLP) | ✅ | TracingConfig, W3C propagation |
| 6.6 | Security audit logger | ✅ | IoTSecurityAuditor, 12 event types |
| 6.7 | Prometheus scraping configured | ✅ | prometheus.yml, 11 jobs |

## 7. CI/CD

| # | Item | Status | Notes |
|---|------|--------|-------|
| 7.1 | Backend build + test pipeline | ✅ | GitHub Actions mvp3-ci.yml |
| 7.2 | Frontend build + test pipeline | ✅ | Conditional, lint + test + build |
| 7.3 | Firmware build + test pipeline | ✅ | CMake + CTest + cppcheck |
| 7.4 | Security scan (OWASP + secrets) | ✅ | Fail CVSS >= 8, grep scan |
| 7.5 | Docker image build | ✅ | Main/release branches |
| 7.6 | Manual approval for pilot deploy | ✅ | GitHub Environment protection |
| 7.7 | Release notes generation | ✅ | Version matrix, changelog |

## 8. Documentation

| # | Item | Status | Notes |
|---|------|--------|-------|
| 8.1 | MVP3 overview | ✅ | docs/mvp3-overview.md |
| 8.2 | Edge-cloud architecture | ✅ | docs/edge-cloud-architecture.md |
| 8.3 | Hardware prototype strategy | ✅ | docs/hardware-prototype-strategy.md |
| 8.4 | Firmware architecture | ✅ | docs/firmware-architecture.md |
| 8.5 | MQTT protocol v1 | ✅ | docs/mqtt-protocol-v1.md |
| 8.6 | Device provisioning | ✅ | docs/device-provisioning.md |
| 8.7 | Real device ingestion | ✅ | docs/real-device-ingestion.md |
| 8.8 | Data quality | ✅ | docs/data-quality.md |
| 8.9 | Field data collection | ✅ | docs/field-data-collection.md |
| 8.10 | Lab test protocol | ✅ | docs/lab-test-protocol.md |
| 8.11 | Field pilot 10 devices | ✅ | docs/field-pilot-10-devices.md |
| 8.12 | IoT security | ✅ | docs/iot-security.md |
| 8.13 | Observability | ✅ | docs/mvp3-observability.md |
| 8.14 | CI/CD | ✅ | docs/ci-cd-mvp3.md |
| 8.15 | Production readiness | ✅ | Ce document |

## 9. Opérationnel Pilote

| # | Item | Status | Notes |
|---|------|--------|-------|
| 9.1 | Field installation checklist | ✅ | docs/field-installation-checklist.md |
| 9.2 | Daily monitoring checklist | ✅ | docs/pilot-daily-monitoring-checklist.md |
| 9.3 | Final report template | ✅ | docs/pilot-final-report-template.md |
| 9.4 | Pilot transition plan | ✅ | docs/pilot-transition-plan.md |
| 9.5 | Incident response procedures | ✅ | docs/iot-security.md §Runbook |
| 9.6 | Firmware release checklist | ✅ | docs/ci-cd-mvp3.md §Checklist |

## 10. Conformité & Sécurité Physique

| # | Item | Status | Notes |
|---|------|--------|-------|
| 10.1 | Installation par électricien habilité (B2V min.) | ⚠️ | Documenté, à respecter sur terrain |
| 10.2 | Consignation obligatoire (VAT, condamnation) | ⚠️ | Procédure documentée |
| 10.3 | Disclaimer "pas de sécurité incendie certifiée" | ✅ | Dans README + docs |
| 10.4 | Retrait sans condition sur demande gestionnaire | ✅ | Procédure documentée |
| 10.5 | RGPD : pseudonymisation, pas de transmission tiers | ✅ | Logs masqués, isolation tenant |
| 10.6 | Certification IEC 61439 | ❌ | Post-pilote, budget séparé |
| 10.7 | Tests CEM (chambre anéchoïque) | ❌ | Sprint 3.6+ si budget |
| 10.8 | Tests thermiques (-10°C / +60°C) | ❌ | Sprint 3.6+ si budget |

## Résumé

| Catégorie | Done | Partial | Not Started | Total |
|-----------|------|---------|-------------|-------|
| Sécurité IoT | 9 | 1 | 2 | 12 |
| Pipeline Ingestion | 8 | 0 | 0 | 8 |
| Backend Services | 8 | 0 | 0 | 8 |
| Frontend | 7 | 0 | 0 | 7 |
| Firmware | 7 | 1 | 2 | 10 |
| Observabilité | 7 | 0 | 0 | 7 |
| CI/CD | 7 | 0 | 0 | 7 |
| Documentation | 15 | 0 | 0 | 15 |
| Opérationnel | 6 | 0 | 0 | 6 |
| Conformité | 3 | 2 | 3 | 8 |
| **TOTAL** | **77** | **4** | **7** | **88** |

## Risques Résiduels Avant Pilote

| Risque | Probabilité | Impact | Mitigation |
|--------|-------------|--------|------------|
| Firmware instable sur hardware réel | Moyenne | Haut | Test labo 24h obligatoire avant terrain |
| Interférences WiFi en bâtiment | Moyenne | Moyen | LoRaWAN en backup |
| Faux positifs détection > 5% | Moyenne | Moyen | Seuils ajustables, feedback loop |
| Compromission device physique | Faible | Haut | Revocation + rotation, mTLS planifié |
| Perte connectivité > 72h | Faible | Moyen | Buffer SPIFFS, drain mode |
| Dérive thermique capteur | Faible | Moyen | Calibration terrain, compensation |

## Décision GO / NO-GO Pilote

**Critères obligatoires (GO):**
- [x] Pipeline sécurité complet (HMAC + anti-replay + revocation)
- [x] Tests sécurité passent (24+ tests IoT)
- [x] Firmware tests host OK (43 tests)
- [x] CI/CD opérationnel avec manual approval
- [x] Documentation opérationnelle complète
- [ ] Firmware validé sur hardware 24h (à faire)
- [ ] Électricien identifié et disponible (à confirmer)

**Critères recommandés (non bloquants):**
- [ ] MQTT TLS validé end-to-end
- [ ] Backup LoRaWAN configuré
- [ ] Assurance RC Pro à jour
