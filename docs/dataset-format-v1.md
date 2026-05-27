# Dataset Format v1 - PyroSense AI

## Vue d'Ensemble

Le format v1 definit la structure des datasets ML exportes par la plateforme PyroSense. Les exports sont pseudonymises, labellises et qualifies.

## Formats Supportes

| Format | Extension | Usage |
|--------|-----------|-------|
| Apache Parquet | .parquet | Entrainement ML (pandas, Spark, PyTorch) |
| CSV | .csv | Analyse exploratoire, outils tabulaires |
| JSON Lines | .jsonl | Streaming, pipelines evenementiels |

## Schema d'un Candidat Exporte

### Colonnes Principales

| Colonne | Type | Nullable | Description |
|---------|------|----------|-------------|
| candidate_id | string (UUID) | Non | Identifiant unique du candidat |
| pseudonymized_device_id | string | Non | Device pseudonymise (ps_xxxx) |
| pseudonymized_tenant_id | string | Non | Tenant pseudonymise (ps_xxxx) |
| window_start | timestamp | Non | Debut de la fenetre d'observation |
| window_end | timestamp | Non | Fin de la fenetre d'observation |
| quality_tier | string | Non | HIGH / MEDIUM / LOW |
| data_quality_score | int | Non | Score 0-100 |
| risk_score_at_time | float | Oui | Score de risque au moment de l'observation |
| anomaly_type | string | Oui | Type d'anomalie detectee |
| primary_label | string | Oui | Label principal (taxonomy v1) |
| label_source | string | Oui | Source du label (TECHNICIAN/LAB/SYSTEM/MANUAL_REVIEW) |
| label_confidence | float | Oui | Confiance du label (0.0-1.0) |
| label_count | int | Non | Nombre total de labels |

### Colonnes Features

| Colonne | Type | Unite | Description |
|---------|------|-------|-------------|
| rms_voltage_avg | float | V | Tension RMS moyenne sur la fenetre |
| rms_voltage_sd | float | V | Ecart-type tension RMS |
| rms_current_avg | float | A | Courant RMS moyen |
| rms_current_sd | float | A | Ecart-type courant RMS |
| temperature_avg | float | C | Temperature moyenne |
| temperature_sd | float | C | Ecart-type temperature |
| thd_avg | float | % | THD moyen |
| thd_max | float | % | THD maximum |
| hf_noise_avg | float | - | Bruit HF moyen (normalise 0-1) |
| hf_noise_max | float | - | Bruit HF maximum |
| micro_arc_count | int | - | Nombre de micro-arcs detectes |
| power_factor_avg | float | - | Facteur de puissance moyen |
| reading_count | int | - | Nombre de lectures dans la fenetre |
| signal_quality_avg | float | - | Qualite signal moyenne (0-1) |

### Colonnes Metadata Export

| Colonne | Type | Description |
|---------|------|-------------|
| export_job_id | string (UUID) | ID du job d'export |
| exported_at | timestamp | Date d'export |
| dataset_version | string | Version du format (v1) |

## Contraintes de Qualite

### Donnees Incluses

- Tier >= MEDIUM (data_quality_score >= 75)
- Au moins 100 lectures dans la fenetre (reading_count >= 100)
- Signal quality moyenne > 0.5

### Donnees Exclues

- Candidats REJECTED
- Candidats avec tier INSUFFICIENT (score < 50)
- Candidats sans label (sauf `includeUnlabeled=true`)
- Candidats deja EXPORTED (eviter les doublons)

## Label Taxonomy v1

```
NORMAL                            → Fonctionnement normal
MICRO_ARC_SUSPECTED               → Suspicion non confirmee
MICRO_ARC_CONFIRMED               → Confirme par technicien/labo
LOOSE_CONNECTION_CONFIRMED         → Connexion desserre confirmee
INSULATION_DEGRADATION_CONFIRMED   → Degradation isolation confirmee
OVERLOAD_CONFIRMED                 → Surcharge confirmee
SENSOR_NOISE                       → Bruit capteur (pas un defaut)
FALSE_POSITIVE                     → Faux positif confirme
INCONCLUSIVE                       → Resultat non conclusif
```

## Exemple Parquet

```python
import pandas as pd

df = pd.read_parquet("pyrosense_dataset_v1_2026Q1.parquet")

# Structure
print(df.columns.tolist())
# ['candidate_id', 'pseudonymized_device_id', 'pseudonymized_tenant_id',
#  'window_start', 'window_end', 'quality_tier', 'data_quality_score',
#  'risk_score_at_time', 'anomaly_type', 'primary_label', 'label_source',
#  'label_confidence', 'label_count', 'rms_voltage_avg', 'rms_voltage_sd',
#  'rms_current_avg', 'rms_current_sd', 'temperature_avg', 'temperature_sd',
#  'thd_avg', 'thd_max', 'hf_noise_avg', 'hf_noise_max', 'micro_arc_count',
#  'power_factor_avg', 'reading_count', 'signal_quality_avg',
#  'export_job_id', 'exported_at', 'dataset_version']

# Filtrer les defauts confirmes
defects = df[df['primary_label'].str.contains('CONFIRMED')]

# Distribution des labels
print(df['primary_label'].value_counts())
```

## Exemple CSV

```csv
candidate_id,pseudonymized_device_id,window_start,window_end,quality_tier,primary_label,rms_voltage_avg,thd_avg,micro_arc_count
a1b2c3d4,ps_8f3a2b1c4d5e,2026-03-15T10:00:00Z,2026-03-15T11:00:00Z,HIGH,MICRO_ARC_CONFIRMED,231.2,4.7,3
e5f6g7h8,ps_1a2b3c4d5e6f,2026-03-15T10:00:00Z,2026-03-15T11:00:00Z,MEDIUM,NORMAL,229.8,2.1,0
```

## Exemple JSON Lines

```json
{"candidate_id":"a1b2c3d4","pseudonymized_device_id":"ps_8f3a2b1c4d5e","window_start":"2026-03-15T10:00:00Z","quality_tier":"HIGH","primary_label":"MICRO_ARC_CONFIRMED","rms_voltage_avg":231.2,"thd_avg":4.7,"micro_arc_count":3}
{"candidate_id":"e5f6g7h8","pseudonymized_device_id":"ps_1a2b3c4d5e6f","window_start":"2026-03-15T10:00:00Z","quality_tier":"MEDIUM","primary_label":"NORMAL","rms_voltage_avg":229.8,"thd_avg":2.1,"micro_arc_count":0}
```

## Versioning

- **v1** : Format initial MVP 3 (features agrégées, 14 features, 9 labels)
- Les futures versions ajouteront des features (additif), jamais de suppression
- Le champ `dataset_version` permet de distinguer les formats
- Les consumers doivent tolerer les colonnes inconnues (forward-compatible)

## Securite Export

1. L'export est declenche par un ADMIN uniquement
2. Chaque export genere un audit log immutable
3. Les fichiers exportes ne contiennent aucun identifiant reel
4. Les fichiers sont stockes dans un repertoire protege (FileStoragePort)
5. Les exports sont lies a un tenant pseudonymise (pas de cross-tenant)
6. La cle de pseudonymisation n'est jamais incluse dans l'export

## Limitations v1

- Pas de donnees brutes (echantillons 16kHz) — uniquement features agrégées
- Pas de series temporelles intra-fenetre — une ligne = un resume de fenetre
- Pas de correlation multi-device (chaque candidat est independant)
- Pas de donnees audio/vibration (hors scope MVP 3)
