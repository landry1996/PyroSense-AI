# MVP 4 — Transition vers le Machine Learning

> **AVERTISSEMENT** : Le scoring explicable (MVP 1-3) reste la reference de production.
> Aucun modele ML ne sera deploye en production sans avoir **depasse** le scoring statistique
> en evaluation shadow mode pendant **minimum 4 semaines**.
> L'humain reste dans la boucle : toute alerte CRITICAL requiert validation manuelle.

---

## 1. Ce qui est pret apres MVP 3

### Infrastructure ML deja en place

| Composant | Fichier | Statut |
|-----------|---------|--------|
| `MachineLearningInferencePort` | signal-analysis-service/.../port/out/ | Interface prete, NoOp adapter actif |
| `RiskModelPort` | risk-scoring-service/.../port/out/ | Interface prete, NoOp adapter actif |
| `FeedbackConfidenceEngine` | risk-scoring-service/.../domain/scoring/ | Ajustements ±0.05/0.03 par retour terrain |
| `KafkaFeedbackEventListener` | risk-scoring-service/.../adapter/in/messaging/ | Consomme maintenance-events |
| `DatasetCandidate` pipeline | ingestion-service/.../domain/model/dataset/ | Collecte, labels, export Parquet/CSV/JSONL |
| `TechnicianFeedback` → `DataLabel` | ingestion-service/.../domain/model/dataset/ | Conversion terrain → label ML |
| `PseudonymizationService` | ingestion-service | HMAC-SHA256, respect RGPD |
| `FeatureSummary` | ingestion-service/.../domain/model/dataset/ | 12 features agregees par fenetre |
| Baseline profiles (Welford) | signal-analysis-service | Moyenne/variance/percentiles par device |
| 7 scenarios simulateur | tools/pyrosense-iot-simulator | Normal + 6 degradations progressives |
| TimescaleDB aggregats continus | 1min, 15min, 1h, daily | Retention: raw 90j → daily indefini |

### Scoring statistique : baseline a battre

| Algorithme | Metrique cible | Etat |
|------------|----------------|------|
| Z-Score deviation | Deviation depuis baseline apprise | Production |
| Micro-arc pattern detection | Recurrence arcs dans fenetre | Production |
| Temperature trend analysis | Taux de montee °C/h | Production |
| THD drift detection | Evolution harmoniques vs baseline | Production |
| Exponential smoothing | Lissage bruit pour signal reel | Production |
| Risk scoring multi-facteurs | Score 0-100, 6 facteurs ponderes | Production |

### Feedback loop : deja fonctionnel

```
Intervention completee
    │
    ├── DefectConfirmedEvent → boost confiance +0.05 (decay 0.8^n)
    │                          → label MICRO_ARC_CONFIRMED / LOOSE_CONNECTION / etc.
    │
    ├── FalsePositiveEvent  → reduction confiance -0.03
    │                          → label FALSE_POSITIVE
    │                          → suggestion ajustement seuil
    │
    └── InconcllusiveEvent  → reduction legere -0.01
                               → label INCONCLUSIVE
```

---

## 2. Ce qui manque pour ML reel

### Donnees

| Manque | Impact | Resolution |
|--------|--------|------------|
| Volume insuffisant (<6 mois terrain) | Modele sous-entraine, sur-apprentissage | Pilote 10 devices pendant 6+ mois |
| Diversity electrique limitee | Modele biaise vers un type d'installation | 3+ sites differents (residentiel, tertiaire, industriel) |
| Labels rares (events electriques vrais) | Classes desequilibrees, recall faible | Enrichissement via injection defauts labo |
| Saisonnalite non observee | Drift saisonnier non modelise | Minimum 12 mois pour un cycle complet |
| Pas de donnees "pre-incident reel" | Pas de lead time mesurable | Collaboration avec experts forensiques |

### Infrastructure

| Manque | Impact | Resolution |
|--------|--------|------------|
| Feature store centralise | Features recalculees a chaque entrainement | Deployer offline + online feature store |
| Model registry | Pas de versioning des modeles | MLflow ou equivalent |
| Pipeline d'entrainement automatise | Re-entrainement manuel, non reproductible | Orchestration (Airflow/Prefect) |
| Evaluation continue | Drift non detecte post-deploiement | Monitoring predictions vs outcomes |
| GPU/compute pour entrainement | Temps d'entrainement excessif | Instance GPU ponctuelle (spot) |
| Service d'inference separe | Couplage modele/backend | Container inference dedie (TorchServe/Triton) |

### Processus

| Manque | Impact | Resolution |
|--------|--------|------------|
| Protocole d'evaluation scientifique | Pas de comparaison rigoureuse stat vs ML | Definition metriques + seuils GO/NO-GO |
| Gouvernance modele | Responsabilite floue si erreur ML | Comite validation + audit trail |
| Politique de rollback | Pas de retour arriere en cas de regression | Circuit-breaker automatique |
| Annotation a grande echelle | Labels limitees aux interventions | Interface de labelling pour experts |

---

## 3. Datasets necessaires

### Dataset 1 : Telemetry Features (non supervise)

| Champ | Source | Granularite |
|-------|--------|-------------|
| pseudonymized_device_id | PseudonymizationService | Par fenetre |
| timestamp_window | TimescaleDB | 1min / 15min |
| rms_voltage_avg, rms_voltage_std | continuous_aggregate | Moyennne/ecart-type |
| rms_current_avg, rms_current_std | continuous_aggregate | |
| thd_avg, thd_max | continuous_aggregate | |
| temperature_avg, temperature_max, temperature_rate | continuous_aggregate | °C et °C/h |
| hf_noise_avg, hf_noise_max | continuous_aggregate | |
| micro_arc_count | continuous_aggregate | Nombre dans fenetre |
| transient_count | continuous_aggregate | |
| power_factor_avg | continuous_aggregate | |
| signal_quality_avg | continuous_aggregate | 0-100 |
| data_quality_score | DataQualityAssessment | 0-100 |
| hour_of_day, day_of_week, month | timestamp | Features temporelles |
| device_age_days | device registry | Anciennete |
| baseline_mean_*, baseline_std_* | BaselineProfile | Contexte normalite |

**Volume cible** : 6 mois × 10 devices × 1440 fenetres/jour (1min) = ~26M lignes

### Dataset 2 : Events labellises (supervise)

| Champ | Source | Description |
|-------|--------|-------------|
| event_id | DatasetCandidate.id | Identifiant unique |
| feature_window_24h | Telemetry Features | 24h avant l'alerte |
| feature_window_7d | Telemetry Features | 7 jours avant l'alerte |
| anomaly_type | SignalAnomaly | Type detecte |
| risk_score_at_time | RiskAssessment | Score au moment |
| alert_severity | Alert | INFO/WARNING/CRITICAL |
| label | DataLabel.value | Label de verite terrain |
| label_source | DataLabel.source | TECHNICIAN/LAB/SYSTEM |
| label_confidence | DataLabel.confidence | 0.0-1.0 |
| intervention_result | InterventionResult | DEFECT_CONFIRMED/FALSE_POSITIVE/... |
| time_to_intervention | derive | Delai alerte → intervention |
| defect_confirmed | boolean | Verite terrain binaire |

**Volume cible** : 200+ events labellises (minimum pour supervised learning sur classes rares)

### Dataset 3 : Baseline profiles (reference)

| Champ | Source | Description |
|-------|--------|-------------|
| device_id | pseudonymise | |
| baseline_built_at | BaselineProfile | Date de construction |
| learning_days | BaselineProfile | Duree d'apprentissage |
| mean_rms_voltage | BaselineProfile | |
| std_rms_voltage | BaselineProfile | |
| percentile_95_* | BaselineProfile | Seuils par metric |
| installation_type | metadata | Residentiel/tertiaire/industriel |
| circuit_type | metadata | Eclairage/prises/chauffage/... |

### Dataset 4 : Sequences temporelles (pour LSTM/TCN)

| Champ | Source | Format |
|-------|--------|--------|
| device_id | pseudonymise | |
| sequence | TimescaleDB raw | 60 readings (1min) = 1h fenetre glissante |
| features per step | 11 features | rms_v, rms_i, thd, temp, hf, arc, transient, pf, crest, quality, noise |
| label | post-hoc | NORMAL/ANOMALY/PRE-FAULT |
| time_to_event | derive | Minutes avant evenement (si applicable) |

**Format** : tenseur (N_samples, 60_steps, 11_features) — stockage HDF5 ou TFRecord

---

## 4. Labels necessaires

### Taxonomie complete (extension du MVP 3)

| Label | Definition | Source principale | Frequence attendue |
|-------|-----------|-------------------|-------------------|
| NORMAL | Fonctionnement sain confirme | SYSTEM (auto) | ~95% |
| MICRO_ARC_CONFIRMED | Arc electrique confirme par technicien | TECHNICIAN | Rare (<1%) |
| LOOSE_CONNECTION_CONFIRMED | Connexion desserree | TECHNICIAN | 1-2% |
| INSULATION_DEGRADATION_CONFIRMED | Degradation isolation | TECHNICIAN/LAB | Rare (<0.5%) |
| OVERLOAD_CONFIRMED | Surcharge electrique | TECHNICIAN | 1-3% |
| TEMPERATURE_ANOMALY_CONFIRMED | Echauffement anormal | TECHNICIAN | 1-2% |
| HARMONIC_DISTORTION_CONFIRMED | THD excessif confirme | LAB | Rare |
| FALSE_POSITIVE | Alerte sans defaut reel | TECHNICIAN | 5-15% (cible <5%) |
| SENSOR_NOISE | Artefact capteur, pas defaut | SYSTEM/TECHNICIAN | 2-5% |
| INCONCLUSIVE | Impossible a determiner | TECHNICIAN | 3-5% |
| PRE_FAULT | Fenetre temporelle precedant un defaut confirme | MANUAL_REVIEW (post-hoc) | Derive |

### Strategie de labelling

```
┌─────────────────────────────────────────────────────────────────┐
│                    Sources de labels                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. AUTOMATIQUE (SYSTEM)                                          │
│     - Fenetres sans alerte depuis 30j → NORMAL (confiance 0.7)   │
│     - Signal quality < 30 → SENSOR_NOISE (confiance 0.8)         │
│                                                                   │
│  2. TECHNICIEN (TECHNICIAN)                                       │
│     - Post-intervention → defaut ou faux positif (confiance 0.9+) │
│     - Feedback structure via API /field-feedback                  │
│                                                                   │
│  3. LABORATOIRE (LAB)                                             │
│     - Injection defauts controles (confiance 1.0)                 │
│     - Reference absolue pour calibration                          │
│                                                                   │
│  4. REVUE MANUELLE (MANUAL_REVIEW)                                │
│     - Cas INCONCLUSIVE re-examines par expert                     │
│     - Labels PRE_FAULT appliques post-hoc                         │
│     - Enrichissement par contexte temporel                        │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### Label PRE_FAULT (specifique ML)

Ce label n'existe pas en temps reel. Il est applique **retroactivement** :

1. Un defaut est confirme a T=0
2. On remonte dans le temps : T-1h, T-6h, T-24h, T-7j
3. Chaque fenetre avant le defaut recoit le label PRE_FAULT avec un `time_to_event`
4. Le modele apprend a reconnaitre les patterns qui precedent un defaut

C'est la base du **lead time prediction**.

---

## 5. Metriques ML

### Metriques de detection (classification)

| Metrique | Definition | Seuil GO | Seuil Ideal |
|----------|-----------|----------|-------------|
| **Precision** | TP / (TP + FP) | ≥ 0.80 | ≥ 0.90 |
| **Recall** | TP / (TP + FN) | ≥ 0.85 | ≥ 0.95 |
| **False Positive Rate** | FP / (FP + TN) | ≤ 0.10 | ≤ 0.05 |
| **False Negative Rate** | FN / (FN + TP) | ≤ 0.15 | ≤ 0.05 |
| **F1-Score** | 2 × P×R / (P+R) | ≥ 0.82 | ≥ 0.92 |
| **AUROC** | Area under ROC curve | ≥ 0.85 | ≥ 0.95 |
| **PR-AUC** | Area under precision-recall curve | ≥ 0.60 | ≥ 0.80 |

> **Note** : PR-AUC est plus pertinente qu'AUROC pour les classes desequilibrees (defauts rares).

### Metriques temporelles (prediction)

| Metrique | Definition | Seuil GO | Seuil Ideal |
|----------|-----------|----------|-------------|
| **Lead time before defect** | Temps entre premiere alerte ML et defaut confirme | ≥ 24h | ≥ 7j |
| **Lead time accuracy** | |predicted - actual| / actual | ≤ 50% | ≤ 25% |
| **Time-to-alert reduction** | Lead time ML - Lead time stat | > 0 (positif) | > 48h |

### Metriques de calibration

| Metrique | Definition | Seuil GO | Seuil Ideal |
|----------|-----------|----------|-------------|
| **Calibration score (ECE)** | Expected Calibration Error | ≤ 0.10 | ≤ 0.05 |
| **Brier score** | Mean squared error of probabilities | ≤ 0.15 | ≤ 0.08 |
| **Reliability diagram** | Predicted vs observed probability | Monotone | Linear |

> Un modele calibre signifie : quand il dit "70% de risque", il y a effectivement ~70% de chance de defaut.

### Metriques operationnelles

| Metrique | Definition | Seuil |
|----------|-----------|-------|
| Inference latency P99 | Temps de prediction | ≤ 100ms |
| Model staleness | Temps depuis dernier re-entrainement | ≤ 30j |
| Prediction drift (PSI) | Population Stability Index | ≤ 0.10 |
| Feature drift (KS test) | Kolmogorov-Smirnov par feature | p ≥ 0.05 |
| Alert fatigue ratio | Alertes ML ignorees par operateur | ≤ 20% |
| Coverage | % devices couverts par ML (vs stat fallback) | ≥ 80% |

### Comparaison obligatoire : ML vs Statistique

| Dimension | Statistique (baseline) | ML (challenger) | GO si |
|-----------|----------------------|-----------------|-------|
| Precision | Mesurer sur pilote | Mesurer en shadow | ML ≥ stat |
| Recall | Mesurer sur pilote | Mesurer en shadow | ML ≥ stat |
| FP rate | Mesurer sur pilote | Mesurer en shadow | ML ≤ stat |
| Lead time | Mesurer sur pilote | Mesurer en shadow | ML > stat |
| Explicabilite | Toujours (par design) | SHAP/LIME | Comparable |

---

## 6. Modeles candidats

### 6.1 Baseline statistique amelioree (priorite 1)

**Quoi** : Amelioration du scoring existant avec les retours terrain, sans ML.

| Amelioration | Mecanisme | Impact attendu |
|--------------|-----------|----------------|
| Poids adaptatifs par site | Les facteurs se recalibrent selon FP/TP du site | FP -30% |
| Seuils dynamiques saisonniers | Baseline saisonniere (ete/hiver) | FP -20% |
| Correlation inter-circuits | Alerter si 2+ circuits degradent ensemble | Recall +10% |
| Pattern matching temporel | Reconnaitre la "signature" d'un serrage desserré | Precision +15% |

**Avantage** : Reste explicable, deploiement immediat, pas besoin de GPU.
**Prerequis** : 3+ mois de donnees terrain, 50+ retours technicien.

### 6.2 Isolation Forest (priorite 2)

**Quoi** : Detection d'anomalies non supervisee sur les features agregees.

```
Input:  FeatureSummary (12 dimensions)
Output: anomaly_score ∈ [-1, 1] (plus negatif = plus anomal)
```

| Aspect | Detail |
|--------|--------|
| Type | Non supervise (pas besoin de labels) |
| Entrainement | 10K+ samples "normaux" par installation |
| Inference | ~1ms par prediction |
| Interpretabilite | Feature importance via isolation depth |
| Avantage principal | Detecte anomalies inconnues (au-dela des regles) |
| Risque principal | FP eleve sur installations atypiques |

**Configuration** :
- `n_estimators`: 200
- `contamination`: 0.05 (5% d'anomalies attendues)
- `max_features`: 0.8 (sous-echantillonnage features)

**Deploiement** : scikit-learn model → ONNX → inference Java (ONNX Runtime)

### 6.3 Autoencoder (priorite 3)

**Quoi** : Apprend une representation compacte du "normal" ; la reconstruction error detecte les anomalies.

```
Input:  fenetre 15min (15 steps × 11 features) = vecteur 165
Encoder: 165 → 64 → 16 (latent space)
Decoder: 16 → 64 → 165 (reconstruction)
Output: reconstruction_error = MSE(input, output)
        si error > threshold → anomalie
```

| Aspect | Detail |
|--------|--------|
| Type | Semi-supervise (entraine sur normal uniquement) |
| Avantage | Apprend patterns complexes non lineaires |
| Risque | Threshold reconstruction error difficile a calibrer |
| Entrainement | ~100K fenetres normales, GPU recommande |
| Inference | ~5ms (modele compact) |
| Interpretabilite | Feature avec max reconstruction error = contributeur |

**Architecture** :
```
Input(165) → Dense(64, ReLU) → Dense(16, ReLU) → Dense(64, ReLU) → Dense(165, Sigmoid)
Loss: MSE + regularization L2(1e-4)
Optimizer: Adam(lr=1e-3, decay cosine)
Epochs: 100, batch_size: 256, early_stopping patience=10
```

### 6.4 LSTM / TCN (priorite 4)

**Quoi** : Modeles sequentiels qui apprennent les dependencies temporelles pour prediction.

#### LSTM (Long Short-Term Memory)

```
Input:  sequence (60 steps × 11 features) = 1h a 1 lecture/min
Architecture:
  LSTM(64, return_sequences=True)
  → Dropout(0.3)
  → LSTM(32)
  → Dropout(0.3)
  → Dense(16, ReLU)
  → Dense(1, Sigmoid)  [probabilite de defaut dans les 24h]
```

#### TCN (Temporal Convolutional Network)

```
Input:  sequence (60 steps × 11 features)
Architecture:
  TCN(filters=32, kernel=3, dilations=[1,2,4,8,16])
  → GlobalAveragePooling1D
  → Dense(32, ReLU)
  → Dense(1, Sigmoid)
```

| Aspect | LSTM | TCN |
|--------|------|-----|
| Avantage | Capture dependencies longues | Parallelisable, plus rapide |
| Risque | Gradient vanishing, lent | Receptive field fixe |
| Entrainement | GPU requis, ~10K sequences | GPU requis, ~10K sequences |
| Inference | ~10ms | ~5ms |
| Interpretabilite | Attention weights | Grad-CAM sur convolutions |

**Prerequis** : 200+ sequences labellisees (dont 50+ avec defaut confirme).

### 6.5 CNN spectrogramme (conditionnel)

**Quoi** : Si les capteurs echantillonnent a haute frequence (>1kHz), transformer le signal en spectrogramme et appliquer un CNN de classification d'image.

```
Signal temporel (16kHz, 1s) → STFT → Spectrogramme (image 128×128)
                                        │
                                        ▼
                              CNN (ResNet-18 pre-entraine)
                                        │
                                        ▼
                              Classes: NORMAL / ARC / OVERLOAD / NOISE
```

| Aspect | Detail |
|--------|--------|
| Prerequis | ADC 16kHz+ sur ESP32-S3 (DMA), donnees brutes haute frequence |
| Avantage | Excellente detection micro-arcs (signature spectrale connue) |
| Risque | Necessite envoi donnees brutes (contraire a edge-first) |
| Alternative | Calculer spectrogramme sur edge, envoyer features spectrales |
| Interpretabilite | Grad-CAM localise la region spectrale anomale |

> **Decision** : Ce modele ne sera envisage que si l'Option B/C hardware est deployee
> avec acquisition haute frequence. Pour le MVP 4 avec Option A lab, on reste sur features
> pre-calculees. Le spectrogramme peut etre calcule sur l'edge et les bandes d'interet
> envoyees sous forme de features (pas de donnees brutes au cloud).

### 6.6 Survival Analysis (priorite 5)

**Quoi** : Modelise le "temps avant defaut" comme une variable de survie. Predit QUAND (pas juste SI) un defaut surviendra.

```
Input:  covariates = features electriques + age_installation + historique_alertes
Output: survival_function S(t) = P(T > t | X)
        hazard_rate h(t) = probabilite instantanee de defaut
```

| Modele | Type | Avantage |
|--------|------|----------|
| Cox Proportional Hazards | Semi-parametrique | Interpretable, coefficients = facteurs de risque |
| Random Survival Forest | Non-parametrique | Gere interactions complexes |
| DeepSurv (neural) | Deep learning | Flexibilite maximale |

**Prerequis** : 
- Donnees censurees (devices toujours en fonctionnement sans defaut)
- 100+ evenements "defaut" avec timestamp exact
- Historique long (12+ mois par device)

**Application** :
- Planification maintenance preventive (intervenir a S(t) < seuil)
- Optimisation stock pieces de rechange
- Estimation ROI par installation

---

## 7. Strategie MLOps

### Architecture cible

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        MLOps Pipeline                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌──────────┐    ┌──────────────┐    ┌──────────────┐    ┌───────────┐  │
│  │ Feature  │    │ Training     │    │ Evaluation   │    │ Registry  │  │
│  │ Store    │───▶│ Pipeline     │───▶│ Pipeline     │───▶│ (MLflow)  │  │
│  └──────────┘    └──────────────┘    └──────────────┘    └─────┬─────┘  │
│       ▲                                                        │         │
│       │                                                        ▼         │
│  ┌────┴───────┐                                     ┌──────────────────┐ │
│  │ TimescaleDB│                                     │ Deployment Gate  │ │
│  │ + Dataset  │                                     │ (manual approval)│ │
│  │ Export     │                                     └────────┬─────────┘ │
│  └────────────┘                                              │           │
│                                                              ▼           │
│                           ┌──────────────────────────────────────┐       │
│                           │         Inference Service             │       │
│                           │  ┌────────┐  ┌────────┐  ┌────────┐ │       │
│                           │  │Shadow  │  │Canary  │  │ Full   │ │       │
│                           │  │Mode    │  │(10%)   │  │Rollout │ │       │
│                           │  └────────┘  └────────┘  └────────┘ │       │
│                           └──────────────────────────────────────┘       │
│                                                                           │
│  ┌──────────────────────────────────────────────────────────────────┐    │
│  │                    Monitoring & Alerting                           │    │
│  │  - Prediction drift (PSI)                                         │    │
│  │  - Feature drift (KS test)                                        │    │
│  │  - Accuracy degradation (sliding window)                          │    │
│  │  - Latency percentiles (P50/P95/P99)                              │    │
│  │  - Circuit breaker (auto-fallback to statistical)                 │    │
│  └──────────────────────────────────────────────────────────────────┘    │
│                                                                           │
└─────────────────────────────────────────────────────────────────────────┘
```

### Principes MLOps

| Principe | Implementation |
|----------|----------------|
| Reproductibilite | Datasets versiones (DVC), code entraine dans Git, seeds fixes |
| Automatisation | Pipeline Airflow/Prefect : ingest → train → eval → register |
| Validation gate | Aucun deploiement sans evaluation > baseline + approbation humaine |
| Rollback instantane | Feature flag + circuit breaker : retour stat en <1s |
| Observabilite | Metriques Prometheus sur predictions, drift, latence |
| Audit trail | Chaque prediction loggee (model_version, features_hash, prediction, outcome) |

### Re-entrainement

| Trigger | Frequence | Action |
|---------|-----------|--------|
| Scheduled | Hebdomadaire | Re-entrainement sur 90 derniers jours |
| Drift detecte | PSI > 0.10 | Re-entrainement urgent (alerte equipe) |
| Nouveaux labels | Batch de 50+ labels | Re-entrainement opportuniste |
| Incident ML | Faux negatif CRITICAL | Post-mortem + re-entrainement force |

---

## 8. Feature Store

### Architecture

```
┌───────────────────────────────────────────────────────┐
│                    Feature Store                        │
├───────────────────────────────────────────────────────┤
│                                                        │
│  ┌──────────────────┐     ┌──────────────────────┐    │
│  │  Offline Store   │     │  Online Store         │    │
│  │  (batch training)│     │  (real-time inference)│    │
│  ├──────────────────┤     ├──────────────────────┤    │
│  │  - Parquet/S3    │     │  - Redis              │    │
│  │  - Point-in-time │     │  - Last known values  │    │
│  │  - Historical    │     │  - Sub-10ms lookup    │    │
│  │  - Full dataset  │     │  - Per-device cache   │    │
│  └──────────────────┘     └──────────────────────┘    │
│           ▲                         ▲                  │
│           │                         │                  │
│  ┌────────┴─────────────────────────┴──────────────┐  │
│  │         Feature Computation Engine               │  │
│  │                                                  │  │
│  │  Raw telemetry (TimescaleDB)                     │  │
│  │     → Window aggregation (1min, 15min, 1h)      │  │
│  │     → Statistical features (mean, std, pct)     │  │
│  │     → Derived features (rate, delta, ratio)     │  │
│  │     → Temporal features (hour, dow, season)     │  │
│  │     → Device context (age, type, baseline)      │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
└───────────────────────────────────────────────────────┘
```

### Feature groups

| Groupe | Features | Source | Refresh |
|--------|----------|--------|---------|
| `electrical_realtime` | rms_v, rms_i, thd, temp, hf, arc, pf | TimescaleDB 1min agg | 1min |
| `electrical_stats` | mean_*, std_*, max_*, pct95_* (fenetres 1h/6h/24h) | TimescaleDB aggregats | 15min |
| `device_context` | age_days, baseline_built, learning_days, offline_ratio | Device registry | 1h |
| `temporal` | hour_sin, hour_cos, dow_sin, dow_cos, month_sin, month_cos | timestamp | Chaque prediction |
| `risk_history` | score_24h_ago, score_7d_ago, trend, alert_count_30d | Risk scoring DB | 15min |
| `feedback_stats` | fp_rate_device, fp_rate_anomaly_type, confirmed_rate | Feedback store | Daily |

### Implementation recommandee (MVP 4)

Pour eviter la complexite d'un Feast/Tecton a ce stade :

1. **Offline** : DatasetExportJob existant (Parquet) + vues SQL materialisees TimescaleDB
2. **Online** : Redis (deja deploy) + `DashboardCachePort` pattern etendu
3. **Point-in-time correctness** : requetes SQL avec `WHERE timestamp <= :event_time` (evite data leakage)
4. **Migration future** : interface `FeatureStorePort` dans le domaine, adapter Redis/SQL maintenant, Feast plus tard

---

## 9. Model Registry

### MLflow comme registre central

```
┌─────────────────────────────────────────────────────┐
│                  MLflow Registry                      │
├─────────────────────────────────────────────────────┤
│                                                      │
│  Model: pyrosense-anomaly-detector                   │
│  ├── v1.0.0 (Isolation Forest, 2026-09-15)          │
│  │   ├── Artifacts: model.onnx (2.3 MB)            │
│  │   ├── Metrics: precision=0.82, recall=0.87      │
│  │   ├── Params: n_estimators=200, contamination=5% │
│  │   ├── Tags: stage=shadow, dataset_version=ds-003 │
│  │   └── Status: Shadow (evaluating)                │
│  │                                                   │
│  ├── v1.1.0 (Isolation Forest + feedback, 2026-11)  │
│  │   ├── Status: Canary (10% traffic)               │
│  │   └── Metrics: precision=0.86, recall=0.89      │
│  │                                                   │
│  └── v2.0.0 (Autoencoder, 2027-Q1)                  │
│      └── Status: Staging (offline eval only)         │
│                                                      │
│  Model: pyrosense-risk-predictor                     │
│  ├── v1.0.0 (Cox PH, 2027-Q1)                       │
│  │   └── Status: Shadow                             │
│  └── ...                                             │
│                                                      │
│  Lifecycle Stages:                                   │
│    None → Staging → Shadow → Canary → Production     │
│         (train)  (offline) (no user) (10%)  (full)   │
│                                                      │
└─────────────────────────────────────────────────────┘
```

### Metadata par modele

| Champ | Description |
|-------|-------------|
| model_name | Identifiant unique |
| version | SemVer |
| algorithm | Type de modele |
| dataset_version | Hash du dataset d'entrainement |
| feature_schema | Liste ordonnee des features en entree |
| training_config | Hyperparametres complets |
| evaluation_results | Metriques sur test set |
| comparison_vs_baseline | Delta precision/recall vs statistique |
| approval_status | PENDING/APPROVED/REJECTED |
| approved_by | Identite de l'approbateur |
| deployment_stage | None/Staging/Shadow/Canary/Production |
| created_at | Timestamp |

---

## 10. Evaluation offline

### Protocole d'evaluation

```
1. SPLIT des donnees
   ├── Train: 70% (fenetres temporelles < cutoff)
   ├── Validation: 15% (cutoff → cutoff + N jours)
   └── Test: 15% (plus recentes, JAMAIS vues pendant dev)

   IMPORTANT: split temporel (pas aleatoire) pour eviter data leakage

2. ENTRAINEMENT sur Train set uniquement
   └── Hyperparameter tuning sur Validation set (cross-validation temporelle)

3. EVALUATION sur Test set
   ├── Metriques globales (precision, recall, F1, AUROC, PR-AUC)
   ├── Metriques par type de defaut (macro/micro average)
   ├── Calibration (ECE, reliability diagram)
   ├── Metriques temporelles (lead time)
   └── Comparaison vs baseline statistique (même test set)

4. ANALYSE d'erreur
   ├── Matrice de confusion par type
   ├── Faux positifs : quelles installations/circuits/heures ?
   ├── Faux negatifs : quel pattern manque ?
   └── Distribution des scores (separation classes)

5. DECISION GO/NO-GO
   └── Toutes metriques ≥ seuils GO (section 5) ?
       OUI → passage en Shadow mode
       NON → iteration (features, hyperparams, architecture)
```

### Backtesting

Simuler le modele ML sur donnees historiques :

```python
for event in historical_events:
    features = feature_store.get_point_in_time(event.device, event.timestamp)
    ml_prediction = model.predict(features)
    stat_prediction = statistical_scorer.score(features)
    
    # Comparer : qui a alerte en premier ? qui a eu raison ?
    results.append(BacktestResult(
        event_id=event.id,
        ml_alert_time=ml_prediction.first_alert_time,
        stat_alert_time=stat_prediction.first_alert_time,
        actual_defect_time=event.confirmed_at,
        ml_correct=ml_prediction.matches_outcome(event.label),
        stat_correct=stat_prediction.matches_outcome(event.label),
    ))
```

### Rapport d'evaluation type

```
=== PyroSense Model Evaluation Report ===
Model: pyrosense-anomaly-detector v1.0.0
Date: 2026-11-15
Dataset: ds-003 (147 labeled events, 22,451 normal windows)
Split: temporal (train <2026-09-01, val 09-10, test 10-11)

GLOBAL METRICS:
  Precision:     0.83 (GO threshold: 0.80) ✓
  Recall:        0.88 (GO threshold: 0.85) ✓
  F1-Score:      0.85 (GO threshold: 0.82) ✓
  FP Rate:       0.07 (GO threshold: ≤0.10) ✓
  AUROC:         0.91 (GO threshold: 0.85) ✓
  PR-AUC:        0.72 (GO threshold: 0.60) ✓

VS STATISTICAL BASELINE:
  Precision:     +0.11 (stat=0.72)
  Recall:        +0.03 (stat=0.85)
  Lead time:     +18h (stat avg 6h, ML avg 24h)

CALIBRATION:
  ECE:           0.08 (GO threshold: ≤0.10) ✓
  Brier:         0.12 (GO threshold: ≤0.15) ✓

DECISION: ✓ APPROVE for Shadow Mode
```

---

## 11. Evaluation shadow mode

### Principe

Le modele ML tourne en parallele du scoring statistique, sans impacter l'utilisateur.

```
Telemetry Event
    │
    ├──────────────────────────────────┐
    │                                  │
    ▼                                  ▼
┌──────────────────┐        ┌──────────────────┐
│ Statistical      │        │ ML Model         │
│ Scoring (LIVE)   │        │ (SHADOW)         │
│                  │        │                  │
│ → Alerts users   │        │ → Logs only      │
│ → Creates tasks  │        │ → No user impact │
│ → Triggers notif │        │ → Metrics stored │
└────────┬─────────┘        └────────┬─────────┘
         │                           │
         │                           │
         ▼                           ▼
┌──────────────────────────────────────────────┐
│           Shadow Comparison Engine            │
│                                               │
│  Pour chaque evenement :                      │
│  - stat_predicted vs ml_predicted             │
│  - Quand outcome connu (intervention) :      │
│    → stat_correct? ml_correct?               │
│    → Qui a alerte en premier ?               │
│    → Qui a eu le bon niveau de confiance ?   │
│                                               │
│  Dashboard interne (non visible client) :     │
│  - Concordance stat/ML (%)                    │
│  - ML-only detections (missed by stat)        │
│  - Stat-only detections (missed by ML)        │
│  - Lead time comparison                       │
│                                               │
└──────────────────────────────────────────────┘
```

### Duree et criteres de sortie

| Phase Shadow | Duree min | Critere de sortie |
|-------------|-----------|-------------------|
| Phase 1: Observation | 2 semaines | ML fonctionne sans crash, latence OK |
| Phase 2: Comparaison | 2 semaines | Metriques ML ≥ stat sur 50+ events |
| Phase 3: Decision | 1 semaine | Comite valide les resultats |

**Criteres de sortie shadow → canary** :
- ML precision ≥ stat precision (sur 4 semaines glissantes)
- ML recall ≥ stat recall
- ML FP rate ≤ stat FP rate
- ML lead time ≥ stat lead time
- Aucun faux negatif CRITICAL manque par ML mais detecte par stat
- Approbation comite (Tech Lead + Security + PM)

### Alertes shadow mode

| Alerte | Condition | Action |
|--------|-----------|--------|
| ML disagrees on CRITICAL | stat=CRITICAL, ML=LOW/MODERATE | Investigation immediate |
| ML detects, stat misses | ML alerte, stat non, outcome confirme defaut | +1 pour ML, noter le pattern |
| ML fails repeatedly | 5+ predictions erronees consecutives | Revue modele |
| Latency spike | P99 > 200ms | Check infra inference |

---

## 12. Deploiement progressif

### Pipeline de promotion

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ Staging  │───▶│ Shadow   │───▶│ Canary   │───▶│ Ramped   │───▶│Production│
│          │    │          │    │ (10%)    │    │ (50%)    │    │ (100%)   │
│ Offline  │    │ No user  │    │ Selected │    │ Half     │    │ All      │
│ eval only│    │ impact   │    │ devices  │    │ fleet    │    │ fleet    │
└──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
     │                │                │               │               │
     │                │                │               │               │
     ▼                ▼                ▼               ▼               ▼
  Automated        Automated       Manual          Manual          Automated
  (CI/CD)          (4 weeks)      approval        approval        monitoring
                                  + metrics       + metrics       + rollback
```

### Canary mode (10%)

- Selection : 1-2 devices par site (representatifs, pas critiques)
- Mode **ensemble** : score final = `0.7 × stat + 0.3 × ML`
- L'utilisateur voit le score fusionne avec explication enrichie
- Si ML diverge fortement du stat, le score stat prevaut (guard)
- Duree : 2 semaines minimum, extension si metriques instables

### Ramped mode (50%)

- Extension a la moitie de la flotte
- Poids ML augmente : `0.5 × stat + 0.5 × ML`
- Dashboard operateur montre les deux scores (transparence)
- Critere promotion → production : 2 semaines sans regression

### Production (100%)

- Poids ML : `0.6 × ML + 0.4 × stat` (ML dominant mais stat reste)
- Le scoring statistique ne disparait JAMAIS — il reste comme filet de securite
- Circuit breaker : si ML unavailable > 30s, fallback 100% stat automatique

### Rollback automatique

```java
// Circuit breaker pseudo-code dans RiskScoringEngine
if (mlModel.isAvailable() && mlModel.latency() < MAX_LATENCY) {
    Optional<RiskScore> mlScore = mlModel.predict(deviceId, anomalies);
    if (mlScore.isPresent() && isReasonable(mlScore.get())) {
        return ensemble(statisticalScore, mlScore.get(), weights);
    }
}
// Fallback: statistical scoring only
return statisticalScore;
```

Criteres de rollback automatique :
- Latence P99 > 500ms pendant 5min
- Taux d'erreur inference > 5%
- 3+ faux negatifs CRITICAL en 24h
- PSI > 0.25 (drift severe)

---

## 13. Explicabilite

### Principes non negotiables

1. **Tout score ML doit etre accompagne d'une explication en langage naturel**
2. **L'utilisateur ne voit jamais un chiffre sans savoir d'ou il vient**
3. **"Le modele dit" n'est pas une explication — les facteurs contributeurs le sont**

### Methodes par modele

| Modele | Methode explicabilite | Output |
|--------|----------------------|--------|
| Isolation Forest | Feature importance (isolation depth) | "Top 3 features anomales" |
| Autoencoder | Reconstruction error par feature | "Features avec erreur max" |
| LSTM/TCN | Attention weights + Grad-CAM temporel | "Moment critique dans la sequence" |
| Cox PH | Coefficients hazard ratio | "Facteurs accelerant le risque" |
| Ensemble | Contribution stat vs ML + SHAP global | "Score composite explique" |

### Format d'explication utilisateur

```json
{
  "risk_score": 72,
  "risk_level": "HIGH",
  "explanation": {
    "summary": "Risque eleve: echauffement progressif detecte avec degradation harmonique",
    "factors": [
      {
        "name": "Temperature",
        "contribution": 28,
        "detail": "Temperature en hausse de 3.2°C/h depuis 6h (seuil: 2.0°C/h)"
      },
      {
        "name": "Harmoniques",
        "contribution": 22,
        "detail": "THD a 12.3% — 54% au-dessus de votre baseline (7.8%)"
      },
      {
        "name": "Modele predictif",
        "contribution": 15,
        "detail": "Pattern similaire a 3 defauts confirmes sur installations comparables",
        "model_confidence": 0.78
      }
    ],
    "recommendation": "Planifier inspection sous 48h",
    "predicted_incident_window": "3-7 jours si tendance maintenue"
  }
}
```

### SHAP pour explicabilite globale

- **SHAP (SHapley Additive exPlanations)** calcule la contribution de chaque feature a la prediction
- Dashboard interne : feature importance globale (quelles features comptent le plus sur la flotte)
- Par-prediction : waterfall chart montrant pourquoi CE score est eleve/bas
- Disponible pour Isolation Forest, XGBoost, et ensemble

### Contraintes d'affichage

| Regle | Raison |
|-------|--------|
| Max 5 facteurs affiches | Surcharge cognitive au-dela |
| Pas de jargon technique | "THD" → "Distorsion harmonique" |
| Pas de probabilite brute | "0.78" → "Confiance : elevee" |
| Toujours une action recommandee | L'utilisateur sait quoi faire |
| Distinguer stat vs ML | "Detecte par analyse statistique" / "Confirme par modele predictif" |
| Pas de certitude | "Pattern similaire" pas "Defaut certain" |

---

## 14. Gouvernance

### Comite ML (ML Governance Board)

| Role | Responsabilite | Vote |
|------|---------------|------|
| Tech Lead | Architecture, integration, performance | Oui |
| Data Scientist | Qualite modele, metriques, drift | Oui |
| Security Lead | Risques, donnees, vie privee | Oui |
| Product Manager | Impact utilisateur, UX, alertes | Oui |
| Electricien referent | Pertinence terrain, faux positifs | Consultatif |

### Processus de validation

```
Nouveau modele propose
    │
    ▼
Evaluation offline (section 10)
    │ Metriques ≥ seuils GO ?
    │
    ├── NON → retour iteration
    │
    ▼ OUI
Revue par comite ML
    │ Approbation unanime ?
    │
    ├── NON → feedback, iteration
    │
    ▼ OUI
Shadow mode (4 semaines)
    │ Criteres sortie remplis ?
    │
    ├── NON → extension ou retrait
    │
    ▼ OUI
Revue comite + validation terrain (electricien)
    │
    ▼
Canary deployment (2 semaines)
    │ Pas de regression ?
    │
    ├── NON → rollback, investigation
    │
    ▼ OUI
Production graduelle
```

### Registre de decisions

Chaque decision de deploiement est enregistree :

| Champ | Contenu |
|-------|---------|
| Decision ID | Unique |
| Date | Timestamp |
| Model | Nom + version |
| Action | APPROVE_SHADOW / APPROVE_CANARY / APPROVE_PROD / REJECT / ROLLBACK |
| Raison | Justification ecrite |
| Metriques | Snapshot des metriques au moment |
| Votants | Qui a vote, quel vote |
| Conditions | Conditions de monitoring post-deploy |
| Rollback plan | Comment revenir en arriere |

### Audit trail des predictions

Chaque prediction ML en production est loggee (sans PII) :

```json
{
  "prediction_id": "uuid",
  "model_version": "v1.1.0",
  "timestamp": "2026-11-15T14:30:00Z",
  "pseudonymized_device": "abc123",
  "features_hash": "sha256:...",
  "prediction": 0.73,
  "prediction_label": "HIGH",
  "statistical_score": 68,
  "ensemble_score": 72,
  "explanation_factors": ["temperature", "thd_drift"],
  "outcome": null,
  "outcome_updated_at": null
}
```

L'`outcome` est rempli quand l'intervention confirme ou infirme.

### Ethique et limites

| Regle | Raison |
|-------|--------|
| Pas de decision automatique de coupure electrique | Securite des personnes |
| Pas de garantie d'absence d'incendie | Responsabilite legale |
| Transparence sur les limites du modele | Confiance utilisateur |
| Pas de biais par type d'installation | Equite de service |
| RGPD : pseudonymisation des features d'entrainement | Protection donnees |
| Droit a l'explication : toute alerte explicable | Reglementation IA |

---

## 15. Risques

### Risques techniques

| # | Risque | Probabilite | Impact | Mitigation |
|---|--------|-------------|--------|------------|
| R1 | Donnees insuffisantes pour supervised learning | HAUTE | Modeles sous-entraines | Commencer par unsupervised (IF, AE) |
| R2 | Data leakage dans feature engineering | MOYENNE | Metriques trompeuses | Split temporel strict, point-in-time features |
| R3 | Concept drift (installations changent) | HAUTE | Performance degrade | Monitoring PSI + re-entrainement auto |
| R4 | Class imbalance (defauts rares) | HAUTE | Recall faible | SMOTE, focal loss, class weights |
| R5 | Overfitting sur petit dataset | HAUTE | Generalisation faible | Regularisation, cross-validation, ensemble |
| R6 | Latence inference trop elevee | FAIBLE | Experience utilisateur | ONNX Runtime, model pruning, batch |
| R7 | ML model service unavailable | MOYENNE | Perte couverture ML | Circuit breaker, fallback stat |
| R8 | Feature store inconsistency | MOYENNE | Predictions incoherentes | Point-in-time correctness, tests |

### Risques organisationnels

| # | Risque | Probabilite | Impact | Mitigation |
|---|--------|-------------|--------|------------|
| R9 | Confusion ML = boite noire | HAUTE | Perte confiance utilisateur | Explicabilite obligatoire (section 13) |
| R10 | Alert fatigue (ML trop sensible) | MOYENNE | Operateurs ignorent alertes | Calibration stricte, FP rate ≤ 10% |
| R11 | Dependance a un data scientist unique | MOYENNE | Bus factor | Documentation, code reproductible |
| R12 | Surcout infra GPU | FAIBLE | Budget | Instances spot, entrainement batch |
| R13 | Pas assez de labels terrain | HAUTE | Supervised learning impossible | Interface labelling, incentive techniciens |
| R14 | Divergence stat/ML confuse les utilisateurs | MOYENNE | UX degradee | Un seul score affiche (ensemble), explication |

### Risques specifiques PyroSense

| # | Risque | Probabilite | Impact | Mitigation |
|---|--------|-------------|--------|------------|
| R15 | Faux negatif CRITICAL (ML rate un defaut dangereux) | FAIBLE | Securite des personnes | Stat reste actif en parallele TOUJOURS |
| R16 | ML "apprend" un pattern de capteur defaillant | MOYENNE | Alertes erronees | Signal quality filter en pre-processing |
| R17 | Modele entraine sur un site, deploye partout | HAUTE | Performance heterogene | Fine-tuning par site, ou federated |
| R18 | Reglementation IA (AI Act) | MOYENNE | Conformite | Audit trail, explicabilite, humain in loop |

---

## 16. Roadmap MVP 4

### Phase 4.0 — Fondations (Mois 1-2)

> **Objectif** : Infrastructure MLOps minimale, baseline mesurable.

| Sprint | Livrable | Critere de succes |
|--------|----------|-------------------|
| 4.0.1 | Feature store offline (vues SQL + export Parquet) | Export 1M+ lignes en <5min |
| 4.0.2 | Feature store online (Redis, features per device) | Lookup <10ms P99 |
| 4.0.3 | MLflow deploye (Docker Compose) | Model registry fonctionnel |
| 4.0.4 | Pipeline evaluation offline (notebook → script) | Rapport automatique |
| 4.0.5 | Mesurer baseline statistique sur donnees pilote | Metriques de reference documentees |
| 4.0.6 | Interface labelling (extension FieldDataController) | Technicien peut labeller en 3 clics |

### Phase 4.1 — Premier modele (Mois 2-3)

> **Objectif** : Isolation Forest en shadow mode.

| Sprint | Livrable | Critere de succes |
|--------|----------|-------------------|
| 4.1.1 | Entrainement Isolation Forest (scikit-learn) | Precision ≥ 0.80 offline |
| 4.1.2 | Export ONNX + inference service (container) | Latence <50ms P99 |
| 4.1.3 | Adapter `MachineLearningInferencePort` (signal-analysis) | Predictions loggees |
| 4.1.4 | Shadow mode comparison engine | Dashboard metriques shadow |
| 4.1.5 | 4 semaines shadow mode | Metriques ≥ stat sur 50+ events |

### Phase 4.2 — Scoring ameliore (Mois 3-4)

> **Objectif** : Poids adaptatifs + Isolation Forest en canary.

| Sprint | Livrable | Critere de succes |
|--------|----------|-------------------|
| 4.2.1 | Poids adaptatifs par site (feedback → weights) | FP -20% vs poids fixes |
| 4.2.2 | Adapter `RiskModelPort` (risk-scoring) | Score ensemble fonctionnel |
| 4.2.3 | Canary deployment (10% fleet, ensemble 0.7/0.3) | Pas de regression 2 semaines |
| 4.2.4 | SHAP explanations pour predictions ML | Explicabilite comparable au stat |
| 4.2.5 | Dashboard operateur enrichi (stat + ML side by side) | UX validee |

### Phase 4.3 — Modeles avances (Mois 4-6)

> **Objectif** : Autoencoder + first supervised model si labels suffisants.

| Sprint | Livrable | Critere de succes |
|--------|----------|-------------------|
| 4.3.1 | Autoencoder (Keras/PyTorch → ONNX) | Reconstruction error discriminante |
| 4.3.2 | Shadow mode autoencoder vs IF | Identifier meilleur modele |
| 4.3.3 | LSTM/TCN si ≥200 sequences labelisees | Lead time prediction fonctionnelle |
| 4.3.4 | Survival analysis (Cox PH) si ≥100 events | Planification maintenance predictive |
| 4.3.5 | Model ensemble (vote majoritaire ou stacking) | Score final robuste |

### Phase 4.4 — Production ML (Mois 5-6)

> **Objectif** : ML en production avec gouvernance complete.

| Sprint | Livrable | Critere de succes |
|--------|----------|-------------------|
| 4.4.1 | Production deployment (ensemble stat+ML) | 100% fleet couvert |
| 4.4.2 | Monitoring drift (PSI, KS, accuracy sliding) | Alertes avant degradation |
| 4.4.3 | Re-entrainement automatise (Airflow/Prefect) | Pipeline hebdomadaire |
| 4.4.4 | Circuit breaker + rollback auto | Fallback <1s sur test |
| 4.4.5 | Gouvernance formalisee (comite, registre decisions) | Premier audit reussi |
| 4.4.6 | Documentation scientifique (papier interne) | Methodologie reproductible |

### Timeline visuelle

```
Mois 1          Mois 2          Mois 3          Mois 4          Mois 5          Mois 6
│                │                │                │                │                │
├── 4.0 ────────┤                │                │                │                │
│  Feature store │                │                │                │                │
│  MLflow        │                │                │                │                │
│  Baseline      │                │                │                │                │
│                ├── 4.1 ────────┤                │                │                │
│                │  Isolation     │                │                │                │
│                │  Forest        │                │                │                │
│                │  Shadow mode   │                │                │                │
│                │                ├── 4.2 ────────┤                │                │
│                │                │  Scoring       │                │                │
│                │                │  ameliore      │                │                │
│                │                │  Canary 10%    │                │                │
│                │                │                ├── 4.3 ────────┤                │
│                │                │                │  Autoencoder   │                │
│                │                │                │  LSTM/TCN      │                │
│                │                │                │  Survival      │                │
│                │                │                │                ├── 4.4 ────────┤
│                │                │                │                │  Production    │
│                │                │                │                │  Monitoring    │
│                │                │                │                │  Governance    │
```

### Dependances critiques

```
Pilote 10 devices (MVP 3)
    │
    ├── 3+ mois de donnees terrain ──────────────────► Phase 4.1 (IF)
    │
    ├── 50+ retours technicien ──────────────────────► Phase 4.2 (poids adaptatifs)
    │
    ├── 6+ mois de donnees ──────────────────────────► Phase 4.3 (autoencoder)
    │
    ├── 200+ sequences labellisees ──────────────────► Phase 4.3 (LSTM/TCN)
    │
    └── 100+ events avec outcome ────────────────────► Phase 4.3 (survival)
```

### Definition of Done MVP 4

- [ ] Feature store offline + online fonctionnels
- [ ] MLflow avec ≥1 modele en production
- [ ] Modele ML depasse baseline statistique (precision + recall + lead time)
- [ ] Shadow mode valide pendant 4+ semaines
- [ ] Explicabilite (SHAP) sur chaque prediction ML
- [ ] Circuit breaker et rollback automatique testes
- [ ] Monitoring drift en production (PSI + KS + accuracy)
- [ ] Re-entrainement automatise (hebdomadaire)
- [ ] Comite gouvernance constitue et operationnel
- [ ] Audit trail complet des predictions
- [ ] Documentation scientifique du pipeline
- [ ] Scoring statistique reste actif comme filet de securite (JAMAIS retire)

---

## Annexe A : Compatibilite avec l'architecture existante

### Ports hexagonaux deja prets

```java
// Signal Analysis Service
public interface MachineLearningInferencePort {
    Optional<List<SignalAnomaly>> infer(SignalWindow window);
    boolean isAvailable();
    String modelVersion();
}

// Risk Scoring Service
public interface RiskModelPort {
    Optional<RiskScore> predict(DeviceId deviceId, List<AnomalyInput> anomalies);
    boolean isAvailable();
    String modelVersion();
}
```

### Integration sans rupture

1. **Remplacer NoOp adapters** par des appels HTTP/gRPC vers le service d'inference
2. **Aucune modification du domaine** — les ports abstraient le ML
3. **Feature flag** : `pyrosense.ml.enabled=true/false` (par service)
4. **Metrics** : `ml.inference.latency`, `ml.inference.errors`, `ml.predictions.total`

### Nouveau service optionnel : `pyrosense-ml-service`

```
pyrosense-ml-service/
├── src/main/python/
│   ├── training/          # Pipelines d'entrainement
│   ├── inference/         # Service inference (FastAPI + ONNX Runtime)
│   ├── evaluation/        # Scripts evaluation offline
│   ├── monitoring/        # Drift detection
│   └── feature_store/     # Feature computation
├── models/                # Model artifacts (git-lfs ou DVC)
├── notebooks/             # Exploration, EDA, prototypage
├── tests/
├── Dockerfile
├── requirements.txt
└── mlflow/
    └── MLproject
```

---

## Annexe B : Contraintes de securite

| Contrainte | Implementation |
|------------|----------------|
| Pas de donnees brutes au cloud | Features pre-calculees sur edge uniquement |
| Pseudonymisation pour entrainement | HMAC-SHA256 sur device/tenant IDs |
| Pas de PII dans features | Aucune donnee identifiante dans le feature store |
| Pas de secret dans model artifacts | Modeles ne contiennent que des poids numeriques |
| Audit trail sans payload | Predictions loggees sans features brutes |
| RGPD droit a l'oubli | Pseudonymisation irreversible (pas de re-identification) |
| AI Act conformite | Explicabilite + humain dans la boucle + registre |

---

## Annexe C : Budget et ressources

### Infrastructure

| Ressource | Usage | Cout estime/mois |
|-----------|-------|------------------|
| GPU instance (spot) | Entrainement hebdomadaire | ~50-100 EUR |
| MLflow server | Model registry | ~20 EUR (container) |
| Redis (existant) | Feature store online | 0 (deja deploye) |
| S3/MinIO | Artifacts, datasets, Parquet | ~10 EUR |
| Inference container | 1 replica, CPU sufficient | ~30 EUR |

**Total estime** : ~110-160 EUR/mois (hors ressources humaines)

### Competences requises

| Role | Profil | Temps |
|------|--------|-------|
| ML Engineer | Python, scikit-learn, PyTorch, MLOps | 60% pendant 6 mois |
| Backend Engineer | Java/Spring, integration ports | 20% pendant 6 mois |
| Data Engineer | SQL, feature engineering, pipelines | 30% pendant 3 mois |
| Domain Expert (electricien) | Labelling, validation terrain | 10% continu |
