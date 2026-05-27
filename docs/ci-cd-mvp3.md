# CI/CD MVP 3 — Pipeline Qualité Backend + Frontend + Firmware

## Overview

Pipeline GitHub Actions complet pour la validation qualité avant pilote terrain. Couvre le backend Java, le dashboard Angular, et le firmware ESP32-S3.

## Architecture Pipeline

```
                    ┌──────────────────┐
                    │   Push / PR      │
                    └────────┬─────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
    ┌─────────▼──────┐ ┌────▼────────┐ ┌──▼──────────────┐
    │ backend-build  │ │ frontend-   │ │ firmware-build   │
    │ -test          │ │ build-test  │ │                  │
    │                │ │             │ │ • cmake build    │
    │ • compile      │ │ • npm ci    │ │ • unit tests     │
    │ • unit tests   │ │ • lint      │ │ • cppcheck       │
    │ • archunit     │ │ • tests     │ │ • version gen    │
    │ • integration  │ │ • build     │ │                  │
    │ • coverage     │ │             │ │                  │
    └───────┬────────┘ └──────┬──────┘ └────────┬─────────┘
            │                 │                  │
            ▼                 │                  │
    ┌───────────────┐         │                  │
    │ security-scan │         │                  │
    │               │         │                  │
    │ • OWASP       │         │                  │
    │ • secrets     │         │                  │
    └───────┬───────┘         │                  │
            │                 │                  │
            ├─────────────────┤                  │
            │                 │                  │
            ▼                 ▼                  │
    ┌───────────────────────────┐               │
    │ docker-build              │               │
    │ (main/release branches)   │               │
    │                           │               │
    │ • package JARs            │               │
    │ • build images            │               │
    │ • push to registry        │               │
    └───────────┬───────────────┘               │
                │                               │
                ├───────────────────────────────┤
                │                               │
                ▼                               ▼
    ┌───────────────────────────────────────────────┐
    │ release-notes                                 │
    │ (release/* branches)                          │
    │                                               │
    │ • changelog from git log                      │
    │ • version matrix                              │
    │ • pre-pilot checklist                         │
    └───────────────────┬───────────────────────────┘
                        │
                        ▼
    ┌───────────────────────────────────────────────┐
    │ deploy-pilot (workflow_dispatch ONLY)          │
    │                                               │
    │ ⚠️  MANUAL APPROVAL REQUIRED                   │
    │ (GitHub Environment protection rules)         │
    │                                               │
    │ • verify checklist                            │
    │ • deploy backend                              │
    │ • verify health                               │
    │ • notify team                                 │
    └───────────────────────────────────────────────┘
```

## Jobs

### 1. backend-build-test

**Trigger**: Tous push/PR
**Durée**: ~25 min
**Steps**:

| Step | Commande | Objectif |
|------|----------|----------|
| Compile | `mvn compile -B -q` | Vérification syntaxe Java 21 |
| Unit tests | `mvn test -B` | ~500+ tests unitaires (12 modules) |
| ArchUnit | `mvn test -B -Dtest="**/architecture/**"` | Règles hexagonales, isolation domaine |
| Integration | `mvn verify -Ptest -B -Dsurefire.skip=true` | Tests JDBC, Kafka embedded |
| Coverage | `mvn jacoco:report -B` | JaCoCo → seuil 60% minimum |

**Artifacts**: `backend-test-reports`, `backend-coverage`

### 2. frontend-build-test

**Trigger**: Si `pyrosense-dashboard/` modifié OU branche main
**Durée**: ~10 min
**Steps**:

| Step | Commande | Objectif |
|------|----------|----------|
| Install | `npm ci` | Dépendances exactes (lockfile) |
| Lint | `npx ng lint` | ESLint Angular rules |
| Tests | `npx ng test --watch=false --browsers=ChromeHeadless` | Karma/Jasmine |
| Build | `npx ng build --configuration production` | AOT, tree-shaking, minification |

**Artifacts**: `frontend-coverage`

### 3. firmware-build

**Trigger**: Tous push/PR
**Durée**: ~5 min
**Steps**:

| Step | Commande | Objectif |
|------|----------|----------|
| Host build | `cmake .. && make` | Compilation C++17 host (sans ESP-IDF) |
| Unit tests | `ctest --output-on-failure` | 7 suites de tests (43 tests) |
| Static analysis | `cppcheck --enable=all` | Détection bugs, memory leaks |
| Version | `0.3.YYYYMMDD.sha7` | Versioning automatique |

**Artifacts**: `firmware-test-results`, `firmware-{version}`

### 4. security-scan

**Trigger**: Après backend-build-test
**Durée**: ~15 min
**Steps**:

| Step | Commande | Objectif |
|------|----------|----------|
| OWASP | `mvn dependency-check:check -DfailBuildOnCVSS=8` | CVE critiques = build fail |
| Secrets scan | `grep -rn` patterns | Aucun secret hardcodé |

**Artifacts**: `owasp-report`

### 5. docker-build

**Trigger**: Push main ou release/* uniquement
**Durée**: ~20 min
**Steps**:

- Build JARs sans tests
- Docker Buildx multi-arch
- Push vers registry (credentials via secrets)
- Tag: `0.3.0-YYYYMMDD-sha7`

### 6. release-notes

**Trigger**: Push sur release/* après tous tests OK
**Contenu**:

- Changelog (git log depuis dernier tag)
- Matrice de versions (backend, frontend, firmware, schema)
- Statut sécurité
- Checklist pré-pilote

### 7. deploy-pilot

**Trigger**: `workflow_dispatch` uniquement (bouton manuel)
**Protection**: GitHub Environment `pilot` avec reviewers requis

```yaml
environment:
  name: pilot
  url: ${{ vars.PILOT_URL }}
```

**Jamais de déploiement automatique sur pilote.**

## Versioning

### Backend

| Composant | Format | Exemple |
|-----------|--------|---------|
| Maven version | `{major}.{minor}.{patch}-SNAPSHOT` | `0.3.0-SNAPSHOT` |
| Docker tag | `0.3.0-YYYYMMDD-sha7` | `0.3.0-20260527-a3f2bc0` |
| Release | `0.3.0` | SemVer strict |

### Firmware

| Composant | Format | Exemple |
|-----------|--------|---------|
| Build version | `0.3.YYYYMMDD.sha7` | `0.3.20260527.a3f2bc0` |
| Release version | `{major}.{minor}.{patch}` | `0.3.1` |
| OTA compatible | >= version minimale cloud | Tracked par `recordFirmwareVersion()` |

### Payload Schema

| Version | Status | Compatibilité |
|---------|--------|---------------|
| 1.0 | Active (MVP3) | Seule version supportée |
| 2.0 | Planifiée (Edge-Cloud) | CBOR + features additionnelles |

Règle: le cloud supporte **toutes les versions déclarées** dans `MqttProtocolConstants.SUPPORTED_SCHEMA_VERSIONS`.

## Secrets & Variables

### GitHub Secrets (jamais en clair)

| Secret | Usage |
|--------|-------|
| `REGISTRY_USERNAME` | Docker registry login |
| `REGISTRY_PASSWORD` | Docker registry password |
| `PILOT_KUBECONFIG` | Kubernetes config pilote (base64) |

### GitHub Variables (non-sensibles)

| Variable | Usage |
|----------|-------|
| `REGISTRY_URL` | URL du container registry |
| `PILOT_URL` | URL environnement pilote |
| `PILOT_CLUSTER` | Nom du cluster Kubernetes pilote |

### Contraintes sécurité

- **Aucun secret dans le code source** (grep vérifié à chaque build)
- **Secrets uniquement via GitHub Secrets** (encrypted at rest)
- **Pas de `--no-verify`** sur git hooks
- **OWASP fail sur CVSS >= 8** (vulnérabilités critiques bloquent le build)

## Checklist Release Firmware

Avant toute mise à jour OTA sur les capteurs pilote:

### Pré-release

- [ ] Tous tests host passent (`ctest` vert)
- [ ] cppcheck sans erreurs
- [ ] Compilation ESP-IDF réussie (sur machine locale ou CI ESP-IDF)
- [ ] Version firmware incrémentée
- [ ] Changelog firmware documenté
- [ ] Schema version compatible (pas de breaking change sans migration cloud)

### Validation

- [ ] Test sur hardware labo (ESP32-S3 DevKit) pendant 24h sans crash
- [ ] Watchdog: pas de reset pendant test
- [ ] Memory: pas de leak (heap free stable)
- [ ] MQTT: messages reçus côté cloud avec signature valide
- [ ] Offline mode: buffer fonctionne, drain correct après reconnexion
- [ ] Signal quality: score stable et cohérent

### Déploiement

- [ ] Artifact firmware signé (SHA-256)
- [ ] Version enregistrée dans matrice de compatibilité
- [ ] OTA A/B partitions: rollback possible
- [ ] Déploiement 1 device d'abord (canary)
- [ ] Monitoring 2h post-déploiement canary
- [ ] Si OK: déploiement progressif (25%, 50%, 100%)
- [ ] Si KO: rollback automatique (3 boot failures)

### Post-déploiement

- [ ] `firmwareVersion` mis à jour dans métriques Prometheus
- [ ] Alerte `Mvp3FirmwareVersionDrift` ne se déclenche pas
- [ ] Pas de spike `signatureInvalid` (clé HMAC compatible)
- [ ] Pas de spike rejections (schema compatible)

## Concurrency & Optimisations

- **Cancel-in-progress**: un nouveau push annule le build précédent sur la même branche
- **Cache Maven**: `~/.m2/repository` par hash des POM
- **Cache npm**: `package-lock.json` fingerprint
- **Conditional frontend**: skip si aucun fichier `pyrosense-dashboard/` modifié
- **Parallel jobs**: backend, frontend, firmware s'exécutent en parallèle

## Environnements

| Env | Déploiement | Protection |
|-----|-------------|------------|
| CI | Automatique (tests) | Aucune |
| Docker Registry | Auto sur main/release | Credentials secrets |
| Pilote | Manuel uniquement | Environment reviewers + checklist |
| Production | Non configuré (MVP) | — |

## Évolutions futures

- **ESP-IDF CI**: Ajouter action `espressif/esp-idf-ci-action` pour build complet firmware
- **E2E tests**: Testcontainers full-stack (MQTT broker + services + DB)
- **Canary deploy**: Déploiement progressif automatisé avec rollback metrics-based
- **Artifact signing**: Cosign pour images Docker, ECDSA pour firmware
- **SonarQube**: Qualité code + security hotspots
