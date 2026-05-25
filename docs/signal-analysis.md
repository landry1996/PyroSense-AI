# PyroSense Signal Analysis Service — Design & Architecture

## Overview

The Signal Analysis Service detects electrical anomalies using a **hybrid approach**:
1. **Rule-based detection** — explicit, interpretable business rules
2. **Statistical baseline** — z-score deviation from learned device behavior
3. **Drift detection** — exponential smoothing and trend analysis
4. **ML-ready port** — interface prepared for future model integration

This is an MVP design: no deep learning, no opaque models. Every anomaly can be explained by referencing a threshold or statistical measure.

## Detection Pipeline

```
Kafka (telemetry-events)
    │
    ▼
┌──────────────────────────────────────┐
│  KafkaTelemetryEventListener         │
│  (parse IntegrationEvent payload)    │
└──────────────┬───────────────────────┘
               │
       ┌───────┴───────┐
       ▼               ▼
BuildBaseline    AnalyzeSignal
       │               │
       ▼               ▼
┌─────────────┐  ┌────────────────────┐
│ Accumulate  │  │ SignalAnalysisEngine│
│ RunningStats│  │ (orchestrator)     │
│ per device  │  └────────┬───────────┘
└─────────────┘           │
                ┌─────────┼─────────────────┐
                ▼         ▼                 ▼
        ZScoreDetector  MicroArcPattern  TemperatureTrend
                ▼         ▼                 ▼
        ExponentialSmoothing  ThdDrift  (ML port - NoOp)
                          │
                          ▼
                  AnalysisResult
                  (deduplicated, scored)
                          │
                          ▼
              ┌───────────┴──────────────┐
              ▼                          ▼
    AnomalyRepository           Kafka (analysis-events)
    (persist)                   (SignalAnomalyDetectedEvent)
```

## Anomaly Types Detected

| Type | Method | Threshold (configurable) |
|------|--------|--------------------------|
| THD abnormal | Absolute max + baseline drift | `thd-max-percent: 8.0` |
| Micro-arc recurrent | Count of non-zero samples in window | `micro-arc-recurrence-threshold: 3` |
| Temperature rising | Absolute max + rise rate °C/hour | `temperature-max-celsius: 85.0`, `rise-rate: 5.0` |
| HF noise elevated | Exponential smoothing + z-score | `z-score-threshold: 3.0` |
| Transient abnormal | Exponential smoothing + z-score | `z-score-threshold: 3.0` |
| Power factor degraded | Z-score from baseline | `z-score-threshold: 3.0` |
| Baseline drift | Moving avg vs. baseline mean (%) | `drift-percent-threshold: 15.0` |

## Baseline Learning

- Uses **Welford's online algorithm** for numerically stable running mean/variance
- Tracks per-feature: mean, stdDev, min, max, p5/p25/p50/p75/p95, count
- Requires configurable minimum samples before activation (`baseline-minimum-samples: 100`)
- Persisted to PostgreSQL (JSONB for feature stats)
- Publishes `BaselineBuiltEvent` when threshold reached

## Scoring

Each anomaly receives:
- **confidence** [0,1] — how certain the detection is
- **weightedScore** — `type.baseWeight × confidence × clamp(|zScore|/3, 1)`

Aggregate risk score = sum of all weighted scores (capped at 100).

## Configuration (application.yml)

All thresholds are externalized:

```yaml
pyrosense:
  analysis:
    thresholds:
      z-score-threshold: 3.0
      thd-max-percent: 8.0
      temperature-max-celsius: 85.0
      temperature-rise-rate-per-hour: 5.0
      micro-arc-recurrence-threshold: 3
      transient-count-threshold: 10
      hf-noise-max-db: -40.0
      power-factor-min: 0.85
      drift-percent-threshold: 15.0
      exponential-smoothing-alpha: 0.3
      baseline-minimum-samples: 100
```

## ML Extension Point

The `MachineLearningInferencePort` interface:

```java
public interface MachineLearningInferencePort {
    Optional<List<SignalAnomaly>> infer(SignalWindow window);
    boolean isAvailable();
    String modelVersion();
}
```

MVP ships with `NoOpMachineLearningAdapter` (always returns `Optional.empty()`).

To integrate a real model:
1. Implement `MachineLearningInferencePort` (e.g., calling a Python inference server via gRPC/REST)
2. Register as a Spring bean with `@Primary` or via profile activation
3. The analysis service will merge ML results with rule-based detections

The ML port is **additive** — it can only add anomalies, not suppress rule-based ones. This ensures the explainable baseline always runs.

## Extensibility Roadmap

| Phase | Capability |
|-------|-----------|
| MVP (current) | Rule-based + statistical baseline |
| Phase 2 | LSTM/Transformer for sequence anomaly detection |
| Phase 3 | Federated learning across tenant devices |
| Phase 4 | Auto-threshold calibration from labeled incidents |

## Test Coverage

- 66 tests total
- Domain detection algorithms: 28 tests (pure Java, no Spring)
- Use case orchestration: 9 tests (mocked ports)
- Domain models: 15 tests
- ArchUnit rules: 10 tests (hexagonal enforcement)
- Context load: 1 integration test (embedded Kafka)
- No test requires external infrastructure
