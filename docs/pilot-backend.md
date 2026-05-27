# Pilot Backend — Phase MVP 3.8

## Objectif

Fournir les fonctionnalites backend pour le suivi du pilote terrain 10 capteurs PyroSense AI.

## Architecture

Le module pilot est integre dans `pyrosense-dashboard-service` (hexagonal architecture).

```
domain/model/
  PilotProgram.java        (aggregate, state machine)
  PilotStatus.java         (enum: PREPARING, ACTIVE, PAUSED, COMPLETED, CANCELLED)
  PilotDevice.java         (record, status lifecycle)
  PilotSite.java           (record)
  PilotObservation.java    (record, typed: FIELD_NOTE, INSTALLATION_REPORT, etc.)
  PilotIncident.java       (record, severity/category/status)
  PilotKpiSnapshot.java    (record, computed metrics)

application/port/in/
  ManagePilotUseCase.java  (9 methods, 4 command records)

application/port/out/
  PilotRepositoryPort.java (15 methods)

application/usecase/
  ManagePilotService.java  (impl, tenant isolation)

adapter/in/rest/
  PilotController.java     (8 endpoints)

adapter/out/persistence/
  JdbcPilotRepository.java (JDBC, row mappers)
```

## Modele de Domaine

### PilotProgram (Aggregate)

Machine a etats :

```
PREPARING → ACTIVE → PAUSED → COMPLETED
    ↓          ↓        ↓
 CANCELLED  CANCELLED  COMPLETED
```

- `activate()` : depuis PREPARING ou PAUSED
- `pause()` : depuis ACTIVE uniquement
- `complete()` : depuis ACTIVE ou PAUSED
- `cancel()` : depuis tout sauf COMPLETED ou CANCELLED

### PilotDevice

Statuts : PLANNED → INSTALLED → ACTIVE → OFFLINE → REMOVED

### PilotIncident

- Severites : LOW, MEDIUM, HIGH, CRITICAL
- Categories : HARDWARE_FAILURE, FIRMWARE_BUG, CONNECTIVITY, FALSE_POSITIVE, REAL_DEFECT_DETECTED, SECURITY, SITE_ACCESS, OTHER
- Statuts : OPEN, INVESTIGATING, RESOLVED, CLOSED

### PilotKpiSnapshot

Metriques calculees :
- totalDevices, activeDevices, offlineDevices
- uptimePercent, telemetryValidPercent, avgSignalQuality
- alertsGenerated, alertsConfirmed, falsePositives, falsePositiveRate
- incidentsOpen, incidentsResolved, avgLatencyMs

## Endpoints REST

| Methode | Path | Description |
|---------|------|-------------|
| POST | /api/v1/pilots | Creer un pilote |
| GET | /api/v1/pilots | Lister les pilotes du tenant |
| GET | /api/v1/pilots/{id} | Detail pilote (avec devices) |
| PATCH | /api/v1/pilots/{id}/status | Changer le statut |
| POST | /api/v1/pilots/{id}/devices | Ajouter un device |
| POST | /api/v1/pilots/{id}/observations | Ajouter une observation |
| POST | /api/v1/pilots/{id}/incidents | Reporter un incident |
| GET | /api/v1/pilots/{id}/kpis | Historique KPIs |
| POST | /api/v1/pilots/{id}/reports | Generer KPI rapport |

## Securite

- Roles autorises : `TENANT_ADMIN`, `PROPERTY_MANAGER`, `PLATFORM_ADMIN`
- Isolation tenant : `TenantContext.require()` + verification ownership
- Acces refuse : ELECTRICIAN, OCCUPANT, SUPPORT_READONLY

## Schema Base de Donnees

Migration Flyway `V002__create_pilot_tables.sql` :

- `pilot_programs` : id, tenant_id, name, description, status, timestamps
- `pilot_sites` : id, pilot_id (FK), name, address, contact
- `pilot_devices` : id, pilot_id (FK), device_id, serial_number, status
- `pilot_observations` : id, pilot_id (FK), author, type, content
- `pilot_incidents` : id, pilot_id (FK), severity, category, status, resolution
- `pilot_kpi_snapshots` : id, pilot_id (FK), date, 16 metriques

## Tests

| Fichier | Tests | Couverture |
|---------|:-----:|-----------|
| PilotProgramTest | 13 | State machine, validation, device count |
| ManagePilotServiceTest | 9 | Create, get, tenant isolation, status, devices, incidents, KPIs |
| PilotControllerTest | 8 | REST endpoints, responses |
| PilotSecurityTest | 9 | RBAC roles, 401/403 |

## Configuration

Bean wire dans `UseCaseConfig.java` :

```java
@Bean
public ManagePilotService managePilotService(PilotRepositoryPort pilotRepository) {
    return new ManagePilotService(pilotRepository);
}
```

## Regles Metier

1. Un pilote appartient a un seul tenant (isolation stricte)
2. Les devices pilotes sont marques PLANNED a la creation
3. Les incidents ont un cycle de vie OPEN → INVESTIGATING → RESOLVED → CLOSED
4. Les KPIs sont calcules a la demande depuis les devices et incidents actuels
5. Le `falsePositiveRate` est calcule automatiquement
6. Seuls les roles de gestion (TENANT_ADMIN, PROPERTY_MANAGER, PLATFORM_ADMIN) peuvent manipuler un pilote
