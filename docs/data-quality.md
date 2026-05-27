# Data Quality Strategy - PyroSense AI MVP 3

## Objectif

Mesurer la fiabilite des donnees terrain avant de les utiliser pour le scoring ou le futur ML. Les donnees de mauvaise qualite ne doivent pas declencher d'alertes critiques sans confirmation, et les donnees insuffisantes doivent etre exclues de l'entrainement ML ou annotees.

## Architecture

```
electrical_telemetry (TimescaleDB)
        |
        v
DeviceTelemetryStatsPort ──> AssessTelemetryQualityService
        |                              |
        v                              v
TelemetryStats (record)         DataQualityAssessment (aggregate)
                                       |
                    ┌──────────────────┼──────────────────┐
                    v                  v                   v
          DataQualityIssue[]   QualityGrade (A-F)   Events (Kafka)
                    |                  |
                    v                  v
        data_quality_issues    data_quality_assessments
```

## Modele de Domaine

### DataQualityAssessment (Aggregate Root)

Entite principale. Calcule automatiquement le score (0-100), le grade (A-F) et detecte les issues lors de la construction.

| Champ | Type | Description |
|-------|------|-------------|
| id | UUID | Identifiant unique |
| deviceId | String | Device concerne |
| tenantId | String | Tenant proprietaire |
| assessedAt | Instant | Date d'evaluation |
| periodStart/periodEnd | Instant | Fenetre d'analyse |
| overallScore | int | Score composite 0-100 |
| grade | QualityGrade | A/B/C/D/F |
| missingDataRatio | MissingDataRatio | Taux de donnees manquantes |
| rangeStatus | MeasurementRangeStatus | Valeurs hors plage |
| noiseLevel | SensorNoiseLevel | Niveau de bruit capteur |
| completeness | DataCompleteness | Completude heartbeat/telemetry |
| calibrationStatus | DeviceCalibrationStatus | Etat calibration |
| averageSignalQuality | double | Qualite signal moyenne |
| clockDriftSeconds | double | Derive horloge maximale |
| offlineCount | int | Nombre de deconnexions |
| firmwareVersion | String | Version firmware |
| firmwareObsolete | boolean | Firmware obsolete |
| issues | List<DataQualityIssue> | Problemes detectes |

### QualityGrade

| Grade | Score | Description | Regles |
|-------|-------|-------------|--------|
| A | 90-100 | Fiable, exploitable ML + scoring | Trustworthy, alerts OK, ML OK |
| B | 75-89 | Acceptable avec precaution | Trustworthy, alerts OK, ML OK |
| C | 50-74 | Degradee, scoring pondere | Not trustworthy, alerts OK, ML excluded |
| D | 25-49 | Insuffisante, scoring inhibe | Not trustworthy, alerts OK, ML excluded |
| F | 0-24 | Non exploitable | Not trustworthy, alerts BLOCKED, ML excluded |

### Value Objects

| Value Object | Responsabilite |
|-------------|----------------|
| MissingDataRatio | Calcule le taux de messages manquants (expected vs received) |
| MeasurementRangeStatus | Detecte les valeurs hors plage physique |
| SensorNoiseLevel | Evalue le bruit HF, stddev courant/tension |
| DataCompleteness | Completude heartbeat/telemetry + gaps/doublons sequence |
| DeviceCalibrationStatus | Etat de calibration (connue, expiree, inconnue) |
| SignalQualityLevel | 5 niveaux de qualite signal (EXCELLENT → CRITICAL) |

### DataQualityIssue (Entity)

Probleme detecte lors de l'evaluation. Cycle de vie: OPEN → REVIEWED / DISMISSED.

10 types: MISSING_DATA, OUT_OF_RANGE, TIMESTAMP_INCONSISTENT, SEQUENCE_ANOMALY, EXCESSIVE_NOISE, LOW_SIGNAL_QUALITY, FREQUENT_OFFLINE, OBSOLETE_FIRMWARE, SENSOR_DRIFT, UNKNOWN_CALIBRATION.

4 severites: LOW, MEDIUM, HIGH, CRITICAL.

## Algorithme de Scoring

Score initial = 100, penalites cumulatives:

| Critere | Condition | Penalite | Issue Type | Severity |
|---------|-----------|----------|------------|----------|
| Donnees manquantes | >20% | -30 | MISSING_DATA | CRITICAL |
| Donnees manquantes | 5-20% | -15 | MISSING_DATA | MEDIUM |
| Hors plage | >10% lectures | -20 | OUT_OF_RANGE | HIGH |
| Hors plage | >0 lectures | -10 | OUT_OF_RANGE | LOW |
| Derive horloge | >300s | -15 | TIMESTAMP_INCONSISTENT | HIGH |
| Derive horloge | >30s | -5 | TIMESTAMP_INCONSISTENT | LOW |
| Sequence | gaps + duplicates | -min(10, count) | SEQUENCE_ANOMALY | MEDIUM/LOW |
| Bruit excessif | HF/current/voltage | -10 | EXCESSIVE_NOISE | MEDIUM |
| Signal faible | POOR/CRITICAL | -15 | LOW_SIGNAL_QUALITY | HIGH |
| Signal faible | DEGRADED | -5 | - | - |
| Offline frequent | >5 fois | -10 | FREQUENT_OFFLINE | MEDIUM |
| Offline frequent | 2-5 fois | -3 | - | - |
| Firmware obsolete | version < min | -5 | OBSOLETE_FIRMWARE | LOW |
| Calibration inconnue | non valide | -10 | UNKNOWN_CALIBRATION | HIGH/MEDIUM |

Score minimum: 0. Penalite maximale theorique: -145 (clamp a 0).

## Use Cases

### AssessTelemetryQualityUseCase

Evalue la qualite sur une periode donnee. Orchestre:
1. Collecte des statistiques (DeviceTelemetryStatsPort)
2. Verification des capacites device (DeviceCapabilityLookupPort)
3. Construction de l'assessment (auto-scoring + issue detection)
4. Persistance (assessment + issues)
5. Publication d'evenements (score updated + issues critiques)

### ComputeDailyDeviceDataQualityUseCase

Calcul quotidien automatique (fenetre 24h glissante).

### ListDataQualityIssuesQuery

Requete paginee des issues avec filtres: tenant, device, status, type.

### MarkDataQualityIssueReviewedUseCase

Marquer une issue comme revue ou rejetee par un operateur.

## Events Kafka

| Event | Topic | Quand |
|-------|-------|-------|
| DeviceDataQualityScoreUpdatedEvent | ingestion-events | A chaque assessment |
| DataQualityIssueDetectedEvent | ingestion-events | Pour chaque issue HIGH/CRITICAL |

## API REST

### GET /api/v1/devices/{deviceId}/data-quality/latest

Dernier assessment pour un device.

### GET /api/v1/devices/{deviceId}/data-quality/history

Historique des assessments (params: from, to, defaut 30 jours).

### GET /api/v1/data-quality/issues

Liste paginee des issues (params: deviceId, status, type, page, size).

### PATCH /api/v1/data-quality/issues/{id}/review

Marquer une issue comme revue/rejetee (body: comment, dismiss).

**Roles**: PLATFORM_ADMIN, TENANT_ADMIN, DEVICE_MANAGER, PROPERTY_MANAGER.

## Schema Base de Donnees

### data_quality_assessments

```sql
CREATE TABLE data_quality_assessments (
    id UUID PRIMARY KEY,
    device_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    assessed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    overall_score INTEGER NOT NULL,
    grade TEXT NOT NULL,
    trustworthy BOOLEAN NOT NULL DEFAULT false,
    signal_quality_avg DOUBLE PRECISION,
    clock_drift_seconds DOUBLE PRECISION,
    offline_count INTEGER DEFAULT 0,
    firmware_version TEXT,
    firmware_obsolete BOOLEAN DEFAULT false,
    issue_count INTEGER DEFAULT 0,
    completeness_percent DOUBLE PRECISION
);
```

Index: (device_id, assessed_at DESC), (tenant_id, assessed_at DESC), (grade) WHERE grade IN ('D', 'F').

### data_quality_issues

```sql
CREATE TABLE data_quality_issues (
    id UUID PRIMARY KEY,
    device_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    type TEXT NOT NULL,
    severity TEXT NOT NULL,
    details TEXT,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status TEXT NOT NULL DEFAULT 'OPEN',
    reviewed_by TEXT,
    reviewed_at TIMESTAMPTZ,
    review_comment TEXT
);
```

Index: (tenant_id, status, detected_at DESC), (device_id, detected_at DESC), partial index WHERE status = 'OPEN'.

## Regles Metier

1. **Grade F bloque les alertes critiques**: un device avec un score F ne doit pas declencher d'alertes critiques automatiques sans confirmation humaine.
2. **Grade D+ exclut du ML**: les donnees de qualite insuffisante (D/F) sont exclues de l'entrainement ML.
3. **Grade A-B autorise tout**: scoring complet, alertes normales, entrainement ML sans annotation.
4. **Grade C pondere le scoring**: le scoring de risque doit prendre en compte la fiabilite des donnees.
5. **Issues HIGH/CRITICAL publient des events**: notification automatique des operateurs.
6. **Tenant isolation**: chaque issue/assessment est isole par tenant_id.

## Tests

### Tests unitaires (DataQualityAssessmentTest)

- Valeurs hors plage: critical (>10%) penalise -20, minor penalise -10
- Timestamp futur: severe drift (>300s) penalise -15, TIMESTAMP_INCONSISTENT HIGH
- Timestamp trop ancien: moderate drift (30-300s) penalise -5, LOW severity
- Sequence doublon: gaps + duplicates generent SEQUENCE_ANOMALY
- Donnees manquantes: >20% critical (-30), 5-20% warning (-15)
- Signal quality faible: POOR/CRITICAL penalise -15
- Device offline: >5 fois penalise -10, FREQUENT_OFFLINE
- Bruit excessif: HF/stddev au-dessus seuils penalise -10
- Grade F bloque alertes, D exclut ML, A autorise tout
- Penalites cumulatives (multiple issues)

### Tests use case (AssessTelemetryQualityServiceTest)

- Device sain → Grade A
- Out of range → issue HIGH
- Future/old timestamp → TIMESTAMP_INCONSISTENT
- Sequence doublons → SEQUENCE_ANOMALY
- Donnees manquantes → MISSING_DATA CRITICAL
- Signal faible → LOW_SIGNAL_QUALITY HIGH
- Offline frequent → FREQUENT_OFFLINE
- Issues critiques → events publies
- Assessment + issues persistes

## Configuration

```yaml
# application.yml
pyrosense:
  data-quality:
    telemetry-interval-seconds: 10
    heartbeat-interval-seconds: 60
    minimum-firmware-version: "1.0.0"
```

## Integration avec le Scoring de Risque

Le service de risk scoring consulte le grade de qualite avant de calculer le score final:
- Grade A/B: scoring normal
- Grade C: scoring pondere (penalite fiabilite appliquee)
- Grade D/F: scoring inhibe ou annotation requise

## Migration Flyway

`V007__create_data_quality_tables.sql` - cree les tables et index.
