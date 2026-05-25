# PyroSense Risk Scoring Service — Design & Architecture

## Overview

The Risk Scoring Service transforms electrical anomaly detections into an **explainable risk score (0-100)** per device, electrical panel, or circuit. Every score comes with a breakdown of contributing factors, a recommendation, and a predicted incident window.

## Scoring Philosophy

**Explainable by design.** No black box. Every score can be traced to:
1. Which anomalies contributed (factors)
2. How much each factor weighs (contribution percentage)
3. Why the score increased/decreased (recency, repetition, device state)
4. What action to take (recommendation)

## Risk Levels

| Level | Range | Action |
|-------|-------|--------|
| LOW | 0-29 | Surveillance normale |
| MODERATE | 30-59 | Vigilance accrue |
| HIGH | 60-79 | Intervention planifiée |
| CRITICAL | 80-100 | Intervention immédiate |

## Scoring Formula

```
RiskScore = Σ (weight_i × normalizedValue_i) × 100

Where normalizedValue_i = f(confidence, recency, repetition)
```

### Factor Weights (configurable via application.yml)

| Factor | Weight | Input |
|--------|--------|-------|
| Micro-arcs récurrents | 0.30 | Max confidence × recency × repetition |
| THD drift | 0.20 | THD_ABNORMAL + BASELINE_DRIFT anomalies |
| Température | 0.20 | TEMPERATURE_RISING anomalies |
| Transitoires | 0.10 | TRANSIENT_ABNORMAL anomalies |
| Bruit HF | 0.10 | HF_NOISE_ELEVATED anomalies |
| Fiabilité capteur | 0.10 | Penalty for offline device / no baseline |

### Modifiers

- **Recency decay**: `0.95^(hours_ago / 24)` — recent anomalies weigh more
- **Repetition boost**: `min(1 + (count-1) × 0.1, 1.3)` — repeated patterns amplify
- **Device offline**: +50% penalty on reliability factor
- **No baseline**: +30% penalty on reliability factor

## Trend Detection

Based on last N scores:
- **IMPROVING**: latest score > 5 points below previous
- **STABLE**: delta within ±5 points
- **DEGRADING**: latest score > 5 points above previous
- **CRITICAL**: two consecutive increases of >10 points

## Incident Prediction

| Score Range | Base Window | Modified by Trend |
|-------------|-------------|-------------------|
| 0-29 | 365 days | × trend multiplier |
| 30-59 | 90 days | × trend multiplier |
| 60-79 | 30 days | × trend multiplier |
| 80-100 | 7 days | × trend multiplier |

Trend multipliers: CRITICAL=0.5, DEGRADING=0.75, STABLE=1.0, IMPROVING=1.5

## Events Published

| Event | When | Consumers |
|-------|------|-----------|
| `RiskScoreUpdatedEvent` | Every scoring | Alerting, Dashboard |
| `CriticalRiskDetectedEvent` | Score ≥ 80 | Alerting, Notification |
| `RiskLevelChangedEvent` | Level transitions | Alerting, Audit |

## API

```
GET /api/v1/risk-scores/devices/{deviceId}/latest
GET /api/v1/risk-scores/panels/{panelId}/history?daysBack=7
GET /api/v1/risk-scores/buildings/{buildingId}/summary
```

### Response Example

```json
{
  "deviceId": "abc-123",
  "score": 72,
  "level": "HIGH",
  "trend": "DEGRADING",
  "predictedIncidentDays": 22,
  "recommendation": "Planifier une inspection. Facteur dominant : Micro-arcs récurrents (tendance dégradante).",
  "factors": [
    {"name": "micro_arc", "description": "Micro-arcs récurrents", "weight": 0.30, "value": 0.9, "contribution": 27.0},
    {"name": "thd_drift", "description": "Dérive THD", "weight": 0.20, "value": 0.7, "contribution": 14.0}
  ],
  "computedAt": "2025-01-15T10:00:00Z"
}
```

## ML Extension Point

```java
public interface RiskModelPort {
    Optional<RiskScore> predict(DeviceId deviceId, List<AnomalyInput> anomalies);
    boolean isAvailable();
    String modelVersion();
}
```

MVP ships with `NoOpRiskModelAdapter`. The ML port is **advisory** — it can log predictions for comparison but does not override the rule-based score in MVP.

## Configuration

```yaml
pyrosense:
  scoring:
    weights:
      micro-arc-weight: 0.30
      thd-drift-weight: 0.20
      temperature-trend-weight: 0.20
      transient-weight: 0.10
      hf-noise-weight: 0.10
      recency-decay-factor: 0.95
      repetition-boost-factor: 1.3
      device-reliability-weight: 0.10
      history-window-days: 7
      recency-window-hours: 24
```

## Test Coverage

- 58 tests total
- Domain model: 24 tests (RiskScore boundaries, RiskTrend, RiskFactor)
- Scoring engine: 13 tests (formula, edge cases, decay, repetition)
- Use case: 5 tests (orchestration, events, ML failure)
- ArchUnit: 8 rules
- Context load: 1 integration test (embedded Kafka)

## Extensibility Roadmap

| Phase | Capability |
|-------|-----------|
| MVP (current) | Weighted formula + recency/repetition modifiers |
| Phase 2 | ML shadow scoring (log predictions, compare with formula) |
| Phase 3 | A/B testing between formula and ML model |
| Phase 4 | Graduated model promotion with human override |
