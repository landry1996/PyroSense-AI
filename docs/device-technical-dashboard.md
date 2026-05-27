# Device Technical Dashboard — Phase MVP 3.9

## Objectif

Permettre aux equipes techniques et gestionnaires autorises de surveiller la sante des capteurs reels MVP 3 depuis le dashboard PyroSense AI.

## Architecture

### Backend (pyrosense-dashboard-service)

```
domain/model/
  DeviceTechnicalHealth.java     (record: identification + metriques sante)
  TelemetryQualityReport.java    (record: stats horaires, rejets, signal, offline)
  DeviceSecurityStatus.java      (record: credentials, tentatives auth, replay)
  PilotDashboard.java            (record: vue consolidee pilote)

application/port/in/
  DeviceTechnicalQuery.java      (4 methodes query)

application/port/out/
  DeviceTechnicalReadModelPort.java  (read model projections)

application/usecase/
  GetDeviceTechnicalService.java (delegation + tenant isolation)

adapter/in/rest/
  DeviceTechnicalController.java (4 endpoints)

adapter/out/persistence/
  StubDeviceTechnicalReadModel.java (stub MVP, a remplacer par JDBC)
```

### Frontend (pyrosense-dashboard Angular)

```
core/models/
  device-technical.model.ts      (interfaces TypeScript)

core/services/
  device-technical-api.service.ts (HTTP client)

features/device-technical/
  device-technical-overview.component.ts   (vue sante device)
  device-telemetry-quality.component.ts    (qualite telemetrie)
  device-security.component.ts             (securite device)
  pilot-monitoring.component.ts            (vue pilote)
```

## Endpoints REST

| Methode | Path | Description |
|---------|------|-------------|
| GET | /api/v1/devices/{id}/technical-health | Sante technique du device |
| GET | /api/v1/devices/{id}/telemetry-quality | Qualite telemetrie (from/to) |
| GET | /api/v1/devices/{id}/security-status | Statut securite |
| GET | /api/v1/pilots/{id}/dashboard | Vue consolidee pilote |

## Ecrans Frontend

### 1. Device Technical Overview (`/devices/:id/technical`)

Cartes KPI :
- Signal Quality (0-100, colore par seuil)
- Data Quality Score + Grade (A-F)
- Uptime (secondes, formate en jours/heures)
- Clock Drift (ms, alerte si > 5000ms)
- Sequence Gaps (alerte si > 0)
- Rejected Telemetry Count

Identification :
- deviceId, serialNumber, firmwareVersion, hardwareRevision
- Connectivity type, last heartbeat, battery (si applicable)
- Device temperature

### 2. Device Telemetry Quality (`/devices/:id/telemetry-quality`)

- Graphique barres : telemetrie recue vs rejetee par heure
- Tableau : raisons de rejet + compteurs
- Graphique ligne : tendance qualite signal
- Tableau : periodes offline (debut, fin, duree)

### 3. Device Security (`/devices/:id/security`)

- Badge statut credentials (ACTIVE vert / REVOKED rouge)
- Version credential
- Date derniere rotation
- Compteur tentatives auth echouees
- Compteur replays bloques
- Banniere alerte si revoque

### 4. Pilot Monitoring (`/pilots/:id/monitoring`)

- Nom pilote + badge statut
- Cartes KPI : uptime, signal quality, incidents, FP rate
- Tableau devices : ID, serial, statut, signal, grade qualite, heartbeat, installation
- Resume incidents (open / investigating / resolved)

## Securite

### Roles autorises

| Role | Acces |
|------|-------|
| PLATFORM_ADMIN | Tous les endpoints, tous les devices |
| TENANT_ADMIN | Devices de son tenant |
| PROPERTY_MANAGER | Devices de son tenant |
| SUPPORT_READONLY | Lecture seule, devices de son tenant |
| ELECTRICIAN | Refuse (403) |
| OCCUPANT | Refuse (403) |

### Donnees sensibles

- **JAMAIS** afficher les valeurs de credentials (HMAC keys, tokens)
- **JAMAIS** exposer les secrets device
- `credentialStatus` : uniquement ACTIVE/REVOKED + version + dates
- `failedAuthAttempts` et `replayAttemptsBlocked` : compteurs uniquement
- Masquer les informations pouvant identifier des vulnerabilites specifiques

### Isolation tenant

- TenantContext.require() sur chaque endpoint backend
- Verification ownership device/pilot avant retour des donnees
- NotFoundException si device n'appartient pas au tenant

## Modeles de donnees

### DeviceTechnicalHealth

| Champ | Type | Description |
|-------|------|-------------|
| deviceId | String | Identifiant unique |
| serialNumber | String | Numero de serie |
| firmwareVersion | String | Version firmware |
| hardwareRevision | String | Revision hardware |
| connectivity | String | Type connexion (WIFI, MQTT) |
| lastHeartbeat | Instant | Dernier heartbeat recu |
| uptimeSeconds | long | Temps fonctionnement |
| signalQuality | double | Qualite signal 0-100 |
| dataQualityScore | double | Score qualite donnees 0-100 |
| dataQualityGrade | String | Grade A-F |
| batteryPercent | Double | Batterie (null si secteur) |
| deviceTemperature | Double | Temperature boitier |
| clockDriftMs | long | Derive horloge en ms |
| sequenceGaps | int | Trous de sequence |
| rejectedTelemetryCount | int | Telemetries rejetees |
| status | String | Statut device |

### TelemetryQualityReport

- receivedPerHour : compteurs horaires de telemetrie recue
- rejectedPerHour : compteurs horaires de telemetrie rejetee
- rejectionReasons : ventilation par cause (INVALID_SIGNATURE, REPLAY_DETECTED, etc.)
- signalQualityTrend : evolution qualite signal dans le temps
- offlinePeriods : periodes offline avec duree

### DeviceSecurityStatus

- credentialStatus : ACTIVE ou REVOKED
- credentialVersion : numero de version
- lastRotation : derniere rotation credential
- failedAuthAttempts : tentatives auth echouees
- replayAttemptsBlocked : replays detectes et bloques
- isRevoked : flag boolean

## Tests

### Backend
- DeviceTechnicalControllerTest : endpoints + responses + 401
- DeviceTechnicalSecurityTest : RBAC roles, 403 pour roles non autorises
- GetDeviceTechnicalServiceTest : delegation, tenant isolation

### Frontend
- device-technical-api.service.spec.ts : appels HTTP corrects
- device-technical-overview.component.spec.ts : creation, loading, affichage
- pilot-monitoring.component.spec.ts : creation, rendu donnees

## Configuration

Bean dans UseCaseConfig.java :

```java
@Bean
public GetDeviceTechnicalService getDeviceTechnicalService(
        DeviceTechnicalReadModelPort readModel, PilotRepositoryPort pilotRepository) {
    return new GetDeviceTechnicalService(readModel, pilotRepository);
}
```

## Seuils d'alerte (frontend)

| Metrique | Bon (vert) | Attention (orange) | Critique (rouge) |
|----------|:----------:|:------------------:|:----------------:|
| Signal Quality | > 70 | 40-70 | < 40 |
| Data Quality Score | > 80 | 50-80 | < 50 |
| Clock Drift | < 1000ms | 1000-5000ms | > 5000ms |
| Sequence Gaps | 0 | 1-5 | > 5 |
| Failed Auth | 0 | 1-3 | > 3 |

## Limitations MVP

- Le stub adapter retourne des donnees simulees (pas de vraie aggreation cross-service)
- En production, le read model sera alimente par des projections Kafka depuis ingestion-service, device-service
- La vue securite depend du device-service pour les vrais compteurs
- Le clock drift est une estimation basee sur le dernier heartbeat
