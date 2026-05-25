# Machine Learning Strategy

## Current State: Statistical MVP

PyroSense currently uses **purely statistical methods** for anomaly detection and risk scoring. No machine learning models are deployed. This is intentional.

### Statistical Methods in Use

| Method | Purpose |
|--------|---------|
| Z-Score anomaly detection | Measures deviation from learned baseline |
| Micro-arc pattern detection | Counts recurrence of arc-like signal patterns |
| Temperature trend analysis | Computes rate of change over sliding windows |
| THD drift detection | Tracks total harmonic distortion evolution |
| Exponential smoothing (EMA) | Filters transient noise to reveal true signal |

### Why Statistical First?

1. **Interpretable** — Every alert can be explained in plain language ("temperature rose 15% above your baseline in 2 hours")
2. **No training data needed** — Works from day one with zero historical data
3. **Fast to implement** — Weeks, not months
4. **Good enough for MVP** — Catches the obvious degradation patterns that represent 80% of preventable faults
5. **Establishes comparison baseline** — When ML arrives, we can measure whether it actually outperforms statistics

---

## Baseline Learning (Welford's Algorithm)

Each device builds its own profile of "normal" using Welford's online algorithm for computing running mean and variance.

### Key Properties

- **Online computation**: Mean and variance updated with each new reading (no batch processing, no full dataset in memory)
- **Per-device profiles**: Device A in a bakery has a completely different "normal" than Device B in an office — each learns independently
- **Adaptive**: Baseline evolves naturally with seasonal patterns (heating season, summer AC loads)
- **Running percentiles**: Dynamic thresholds adjust as the device's environment changes
- **7-day minimum learning period**: Detection does not activate until at least 7 days of data establish a stable baseline — avoids false positives during cold start

### Why Welford's?

Traditional mean/variance requires storing all historical values. Welford's computes the same result incrementally with O(1) memory per metric, which is critical when monitoring thousands of devices simultaneously.

---

## Risk Scoring Formula

### Weighted Multi-Factor Score

```
RiskScore = (micro_arc    × 0.30)
          + (thd_drift    × 0.20)
          + (temperature  × 0.20)
          + (transient    × 0.10)
          + (hf_noise     × 0.10)
          + (reliability  × 0.10)
```

Each factor is normalized to [0, 100] before weighting.

### Score Modifiers

| Modifier | Effect |
|----------|--------|
| Recency decay | Anomalies from the last hour weight more than those from last week |
| Repetition boost | Same anomaly recurring 3+ times increases its factor score |
| Trend detection | IMPROVING / STABLE / DEGRADING / CRITICAL classification |
| Incident prediction | Estimated days to incident = f(score level, trend multiplier) |

### Explainability

Every risk score includes a per-factor breakdown shown to the user:

> "Your risk score is 67 (HIGH). Main contributors: Micro-arc signals (28/30), Temperature trend (18/20), THD drift (12/20)."

This transparency is non-negotiable — building managers must understand WHY an alert fired.

---

## Limitations Without Real Dataset

| Limitation | Impact |
|------------|--------|
| No validated detection thresholds | Current thresholds are educated guesses based on literature, not empirical measurement |
| No false-positive rate measurement | No labeled data to benchmark against — we cannot quantify our precision/recall |
| No correlation between score and actual fire risk | Needs historical incident data to validate that score=75 truly means danger |
| Simulated data misses real-world factors | Electromagnetic interference, sensor drift, environmental conditions, aging wiring |
| No seasonal calibration | Heating season vs. summer loads produce different baselines we have not observed |
| No multi-device correlation | Adjacent panels may affect each other — not modeled without real deployments |

**Bottom line**: The statistical system is architecturally sound but empirically unvalidated. Real-world deployment is required to tune it.

---

## ML Integration Architecture (Future)

### Ports Already in Place

The hexagonal architecture includes ML-ready ports with NoOp adapters:

- **`MachineLearningInferencePort`** — Interface for submitting signal windows and receiving predictions. Currently returns "no prediction" (NoOp adapter).
- **`RiskModelPort`** — Interface for plugging in alternative risk scoring models. Currently delegates to the statistical formula (NoOp adapter).

### Design Principles

- **ML as sidecar or separate service**: The ML model runs outside the core domain — a separate container/service callable via the port
- **Input contract**: `SignalWindow` (N readings over time) is submitted to the model, which returns an `AnomalyPrediction` (probability, type, confidence)
- **Ensemble scoring**: Statistical + ML predictions combined via weighted voting. Neither system alone makes the final decision.
- **A/B testing**: Same device scored by both statistical and ML pipelines — compare alert accuracy over time
- **Gradual rollout**: ML predictions only surface to users when model confidence exceeds a threshold
- **Graceful degradation**: If ML service is unavailable (crash, timeout, scaling), the statistical system takes over seamlessly. Users never lose coverage.

---

## Candidate ML Approaches

| Approach | Type | Use Case | Data Requirement |
|----------|------|----------|-----------------|
| Autoencoders | Unsupervised | Reconstruction error as anomaly signal | Unlabeled telemetry (6+ months) |
| LSTM networks | Supervised | Time-series forecasting (predict next values, flag deviations) | Labeled faults for training |
| Isolation Forest | Semi-supervised | Point anomaly detection in high-dimensional signal space | Mostly normal data + few labeled anomalies |
| XGBoost | Supervised | Risk classification (low/medium/high/critical) | Labeled incidents (100+ confirmed faults) |
| Federated Learning | Distributed | Privacy-preserving training across tenants (no raw data sharing) | Multiple tenant deployments |

### Selection Criteria

The first model deployed will likely be an **Autoencoder** or **Isolation Forest** because:
- They require no labeled incident data (which we do not yet have)
- They learn "normal" patterns and flag deviations (aligns with our baseline approach)
- They can be trained on simulated data and fine-tuned on real data later

---

## Data Requirements for ML

| Requirement | Minimum | Ideal |
|-------------|---------|-------|
| Telemetry per device | 6 months | 2+ years (all seasons) |
| Confirmed electrical faults | 100 labeled incidents | 500+ across diverse failure modes |
| Confirmed false positives | 500 labeled non-fault events | 2000+ (for precision calibration) |
| Installation diversity | 10 buildings | 100+ (residential, commercial, industrial) |
| Sampling rate | 1 reading/second | 1 reading/second (current design) |

### Why So Much Data?

Electrical faults are rare events (thankfully). A model needs to see enough examples to distinguish "unusual but safe" from "unusual and dangerous." With too little data, the model either misses real faults or cries wolf constantly.

---

## Feedback Loop (Already Implemented)

The platform already captures maintenance outcomes to enable future model improvement:

### Positive Reinforcement
- `ElectricalDefectConfirmedEvent` — Maintenance found a real problem
- Effect: Lower detection thresholds for this pattern (we were right to alert)

### Negative Reinforcement
- `FalsePositiveConfirmedEvent` — Maintenance found nothing wrong
- Effect: Raise detection thresholds for this pattern (we alerted unnecessarily)

### Historical Tracking
- Hit-rate: Percentage of alerts that led to real interventions
- Per-pattern accuracy: Which anomaly types produce the most false positives
- Per-device accuracy: Which devices are "noisy" vs. reliable alerters

This feedback loop means the system improves with every maintenance visit, even before ML is introduced.

---

## IoT Simulator Role

The IoT simulator generates synthetic telemetry for development and testing.

### Available Scenarios

| # | Scenario | Description |
|---|----------|-------------|
| 1 | Normal | Healthy electrical system with natural variation |
| 2 | Insulation degradation | Gradual breakdown over weeks |
| 3 | Loose connection | Intermittent contact resistance spikes |
| 4 | Micro-arc | Recurring small arc events, increasing frequency |
| 5 | Overload | Progressive current increase beyond rated capacity |
| 6 | Temperature rise | Slow thermal buildup in connection point |
| 7 | Offline | Device goes silent (communication loss) |

### Features
- **Progressive degradation**: Patterns evolve realistically over time (not instant jumps)
- **Multi-tenant topology**: Simulates multiple buildings, floors, and panels
- **Time acceleration**: Generate months of data in minutes for testing

### Critical Limitation

> Simulated data is NOT real-world physics. It follows mathematical models of what we think faults look like. Real sensors in real buildings will produce data with noise, interference, and patterns we have not imagined. The simulator is for development — not for model training or threshold validation.

---

## Roadmap

| Phase | Description | Prerequisites |
|-------|-------------|---------------|
| **Phase 1** (Current) | Statistical detection + explainable risk scoring | None — delivered |
| **Phase 2** | Collect real data from pilot deployment (10 buildings) | Hardware sensors, pilot partners |
| **Phase 3** | Train initial ML models on pilot data | 6+ months of Phase 2 data |
| **Phase 4** | Ensemble (statistical + ML) with A/B testing | Validated ML model with acceptable accuracy |
| **Phase 5** | Full ML with statistical as fallback | ML outperforms statistical on real-world metrics |
| **Phase 6** | Federated learning across tenants | 50+ tenant deployments, privacy framework |

### Key Principle

Each phase must **prove** it outperforms the previous before graduating. Statistical methods are not "temporary" — they are the safety net that guarantees baseline coverage regardless of ML maturity.
