# Field Data Collection - PyroSense AI MVP 3

## Objectif

Transformer les telemetries et evenements du MVP 3 en donnees exploitables pour analyse, amelioration du scoring et futur entrainement ML. Le systeme collecte les observations terrain, les retours technicien et les confirmations de defaut pour creer un dataset labellise de haute qualite.

## Architecture

```
                                    ┌──────────────────┐
                                    │ Technician App   │
                                    └────────┬─────────┘
                                             │
                                POST /api/v1/field-feedback
                                             │
┌─────────────┐    ┌──────────────┐    ┌─────▼──────────────┐
│ Telemetry   │    │ Anomaly      │    │ FieldDataController │
│ Pipeline    │    │ Detection    │    └─────────────────────┘
└──────┬──────┘    └──────┬───────┘              │
       │                  │                      │
       ▼                  ▼                      ▼
┌──────────────────────────────────────────────────────┐
│            DatasetCandidate Pipeline                   │
│                                                       │
│  Features + Anomalies + Risk + Alerts + Interventions │
│  + Confirmations + Labels                             │
└──────────────────────────┬───────────────────────────┘
                           │
                           ▼
              ┌─────────────────────────┐
              │  PseudonymizationService │
              │  (HMAC-SHA256, one-way)  │
              └────────────┬────────────┘
                           │
                           ▼
              ┌─────────────────────────┐
              │  Dataset Storage         │
              │  (pseudonymized)         │
              └────────────┬────────────┘
                           │
              POST /api/v1/dataset/export-jobs
                           │
                           ▼
              ┌─────────────────────────┐
              │  DatasetExportJob        │
              │  (Parquet/CSV/JSONL)     │
              └─────────────────────────┘
```

## Pipeline de Collecte

Le pipeline aggrege 7 sources de donnees :

1. **Telemetry Features** — metriques calculees (RMS, THD, HF noise, temperature, power factor)
2. **Anomalies** — detections du signal-analysis-service (type, severite, confiance)
3. **Risk Scores** — score de risque au moment de l'observation
4. **Alerts** — alertes declenchees et leur type
5. **Interventions** — interventions associees et leur resultat
6. **Confirmations terrain** — feedback technicien post-intervention
7. **Labels** — etiquettes de qualite (taxonomy ci-dessous)

## Modeles de Domaine

### DatasetCandidate

Candidat pour inclusion dans un dataset ML. Represente une fenetre temporelle de donnees pour un device pseudonymise.

| Champ | Type | Description |
|-------|------|-------------|
| id | UUID | Identifiant unique |
| pseudonymizedDeviceId | String | Device pseudonymise (HMAC) |
| pseudonymizedTenantId | String | Tenant pseudonymise (HMAC) |
| windowStart/windowEnd | Instant | Fenetre temporelle |
| status | CandidateStatus | PENDING_LABEL → LABELED → VALIDATED → EXPORTED / REJECTED |
| qualityTier | DataQualityTier | HIGH/MEDIUM/LOW/INSUFFICIENT |
| dataQualityScore | int | Score qualite (0-100, de DataQualityAssessment) |
| riskScoreAtTime | double | Score de risque au moment |
| anomalyType | String | Type d'anomalie detectee |
| sourceAlertId | UUID | Alerte source (si applicable) |
| sourceInterventionId | UUID | Intervention source |
| labels | List<DataLabel> | Labels appliques |
| featureSummary | FeatureSummary | Resume des features extraites |

**Regles metier :**
- Un candidat est exportable seulement s'il est LABELED ou VALIDATED, avec un tier != INSUFFICIENT
- Le tier HIGH correspond a un data_quality_score >= 90
- Un label confirme + haute confiance (>0.9) valide automatiquement le candidat

### DataLabel

Etiquette de qualite appliquee a un candidat.

| Champ | Type | Description |
|-------|------|-------------|
| value | LabelValue | Valeur de l'etiquette (taxonomy) |
| source | LabelSource | TECHNICIAN / LAB / SYSTEM / MANUAL_REVIEW |
| confidence | double | Confiance 0.0-1.0 |
| labeledBy | String | Identite du labelleur |
| justification | String | Raison du label |

### TechnicianFeedback

Retour terrain d'un technicien apres intervention.

| Champ | Type | Description |
|-------|------|-------------|
| interventionId | UUID | Intervention liee (obligatoire) |
| technicianId | String | Technicien source |
| defectObserved | LabelValue | Defaut observe |
| confidenceLevel | double | Confiance du technicien |
| visualInspection | String | Description visuelle |
| measurementMethod | String | Methode de mesure (camera thermique, etc.) |
| measurementResult | String | Resultat de mesure |
| defectConfirmed | boolean | Defaut confirme |
| falsePositive | boolean | Faux positif confirme |

### DefectConfirmation

Confirmation officielle d'un defaut apres intervention corrective.

### FalsePositiveFeedback

Signalement d'un faux positif avec suggestion d'ajustement de seuil.

### DatasetExportJob

Job d'export asynchrone avec filtres (format, fenetre, qualite, labels).

## Label Taxonomy

| Label | Description | Source typique |
|-------|-------------|----------------|
| NORMAL | Fonctionnement normal confirme | SYSTEM, TECHNICIAN |
| MICRO_ARC_SUSPECTED | Suspicion de micro-arc non confirmee | SYSTEM |
| MICRO_ARC_CONFIRMED | Micro-arc confirme par intervention | TECHNICIAN, LAB |
| LOOSE_CONNECTION_CONFIRMED | Connexion desserre confirmee | TECHNICIAN |
| INSULATION_DEGRADATION_CONFIRMED | Degradation d'isolation confirmee | TECHNICIAN, LAB |
| OVERLOAD_CONFIRMED | Surcharge confirmee | TECHNICIAN |
| SENSOR_NOISE | Bruit capteur (pas un defaut reel) | SYSTEM, TECHNICIAN |
| FALSE_POSITIVE | Faux positif confirme | TECHNICIAN |
| INCONCLUSIVE | Resultat non conclusif | TECHNICIAN, MANUAL_REVIEW |

## API REST

### POST /api/v1/field-feedback

Soumet un retour terrain de technicien. Doit etre lie a une intervention.

**Roles**: PLATFORM_ADMIN, TENANT_ADMIN, ELECTRICIAN, DEVICE_MANAGER

```json
{
  "interventionId": "uuid",
  "deviceId": "device-001",
  "defectObserved": "MICRO_ARC_CONFIRMED",
  "confidenceLevel": 0.95,
  "visualInspection": "Traces d'arc visibles sur borne",
  "measurementMethod": "Camera thermique FLIR",
  "measurementResult": "Delta T = 15K par rapport a phase adjacente",
  "defectConfirmed": true,
  "falsePositive": false,
  "additionalNotes": "Resserrage effectue"
}
```

### GET /api/v1/dataset/candidates

Liste les candidats dataset pour le tenant courant.

**Roles**: PLATFORM_ADMIN, TENANT_ADMIN

Parametres : status, minQuality, label, from, to, page, size.

### POST /api/v1/dataset/export-jobs

Demande un export de dataset pseudonymise.

**Roles**: PLATFORM_ADMIN, TENANT_ADMIN

```json
{
  "format": "PARQUET",
  "windowStart": "2026-01-01T00:00:00Z",
  "windowEnd": "2026-05-01T00:00:00Z",
  "labelFilter": null,
  "minQualityTier": "MEDIUM",
  "includeUnlabeled": false
}
```

### GET /api/v1/dataset/export-jobs/{id}

Consulte le statut d'un job d'export.

## Pseudonymisation

La pseudonymisation utilise HMAC-SHA256 avec une cle secrete configurable :
- Le device_id reel n'apparait jamais dans les tables dataset
- Le tenant_id reel n'apparait jamais dans les exports
- La cle de pseudonymisation est distincte par environnement
- La correspondance inverse n'est possible qu'avec la cle (stockee hors base)

```
pseudonymized_id = "ps_" + HMAC-SHA256(secret, "device:" + deviceId)[0:16]
```

## Controles Qualite Dataset

1. **Score qualite minimum** : seuls les candidats avec un tier HIGH ou MEDIUM sont exportables
2. **Label obligatoire** : un candidat non labellise ne peut etre exporte (sauf `includeUnlabeled=true`)
3. **Confiance minimum** : le label primaire est celui de plus haute confiance
4. **Source tracee** : chaque label conserve sa source (TECHNICIAN, LAB, SYSTEM, MANUAL_REVIEW)
5. **Lien intervention** : le feedback terrain doit etre lie a une intervention existante
6. **Donnees suffisantes** : FeatureSummary.readingCount >= 100

## Securite & Privacy

- **Privacy by design** : pseudonymisation HMAC a l'ecriture, jamais de donnees brutes en clair
- **Pas de donnees electriques sensibles** : seuls les features agrégées sont stockees (RMS moyens, stddev)
- **Separation tenant** : chaque requete est filtree par tenant pseudonymise
- **Audit complet** : chaque feedback, export, et labellisation est audite
- **Consentement** : l'export necessite une action explicite d'un ADMIN

## Schema Base de Donnees

Migration `V008__create_dataset_collection_tables.sql` :
- dataset_candidates (avec JSONB feature_summary)
- technician_feedbacks
- field_observations
- defect_confirmations
- false_positive_feedbacks
- dataset_export_jobs
- data_labels (lie a dataset_candidates)

## Configuration

```yaml
pyrosense:
  dataset:
    pseudonymization-secret: ${DATASET_PSEUDONYMIZATION_SECRET:change-me-in-prod}
```
