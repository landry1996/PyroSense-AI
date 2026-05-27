# Boucle de Feedback Terrain — Scoring PyroSense AI

## Objectif

La boucle de feedback terrain permet d'améliorer progressivement le scoring de risque en intégrant les retours des électriciens après intervention. Chaque confirmation ou invalidation d'alerte ajuste la confiance du modèle de scoring pour un type d'anomalie donné sur un appareil spécifique.

## Architecture

```
┌─────────────────────┐      Kafka       ┌──────────────────────────┐
│ maintenance-service │ ──────────────── │ risk-scoring-service     │
│ (interventions)     │  maintenance-    │                          │
└─────────────────────┘  events topic    │ KafkaFeedbackEventListener│
                                          │         │                │
                                          │         ▼                │
                                          │ ProcessFieldFeedbackService│
                                          │         │                │
                                          │    ┌────┴────┐           │
                                          │    ▼         ▼           │
                                          │ Feedback  Confidence     │
                                          │ Storage   Engine         │
                                          │              │           │
                                          │              ▼           │
                                          │    ScoringAdjustment     │
                                          │    (SUGGESTION_ONLY)     │
                                          └──────────────────────────┘
```

## Événements Kafka Consommés

| Événement | Source | Mapping |
|-----------|--------|---------|
| `maintenance.intervention.completed` | maintenance-service | Déterminé par champ `result` |
| `maintenance.defect.confirmed` | maintenance-service | → `CONFIRMED_DEFECT` |
| `maintenance.false_positive.confirmed` | maintenance-service | → `FALSE_POSITIVE` |

### Champ `result` (intervention completed)

| Valeur | Outcome |
|--------|---------|
| `DEFECT_CONFIRMED` | `CONFIRMED_DEFECT` |
| `ELECTRICAL_DEFECT` | `CONFIRMED_DEFECT` |
| `FALSE_POSITIVE` | `FALSE_POSITIVE` |
| `INCONCLUSIVE` | `INCONCLUSIVE` |
| `NO_DEFECT_FOUND` | `NO_DEFECT_FOUND` |

## Moteur de Confiance (FeedbackConfidenceEngine)

### Règles d'Ajustement

Le moteur applique des ajustements **conservateurs** avec un facteur de décroissance pour éviter l'instabilité :

| Outcome | Ajustement de base | Formule | Limite cumulative |
|---------|-------------------|---------|-------------------|
| `CONFIRMED_DEFECT` | +0.05 | `+0.05 × 0.8^n` | Max +0.20 |
| `FALSE_POSITIVE` | -0.03 | `-0.03 × 0.8^n` | Max -0.15 |
| `NO_DEFECT_FOUND` | -0.01 | Fixe, pas de décroissance | Si confiance > 0.2 |
| `INCONCLUSIVE` | Aucun | — | — |

Où `n` = nombre d'ajustements précédents du même type pour le même device+anomaly.

### Décroissance (Decay Factor)

```
decay = 0.8^previousCount
```

Chaque feedback successif a moins d'impact → stabilisation naturelle du modèle.

### Bornes de Sécurité

- Confiance ne peut pas dépasser 1.0 ni descendre sous 0.0
- Boost bloqué si confiance ≥ 0.99
- Réduction bloquée si confiance ≤ 0.1 (FALSE_POSITIVE) ou ≤ 0.2 (NO_DEFECT)
- Cumul boost plafonné à +0.20
- Cumul réduction plafonné à -0.15

## Mode SUGGESTION_ONLY

**Tous les ajustements sont en mode `SUGGESTION_ONLY` par défaut.** Ils ne modifient pas directement le scoring en production. Ce mode permet :

1. Accumuler suffisamment de données avant d'activer les ajustements automatiques
2. Valider la cohérence des suggestions via dashboard
3. Activer progressivement (`APPLIED`) après période d'observation
4. Traçabilité complète de chaque suggestion

## API REST

### GET /api/v1/scoring/feedback/history

Historique paginé des feedbacks.

**Paramètres :**
- `tenantId` (requis) : identifiant tenant
- `deviceId` (optionnel) : filtrer par appareil
- `anomalyType` (optionnel) : filtrer par type d'anomalie
- `from` / `to` (optionnel) : plage temporelle ISO-8601
- `page` (défaut: 0), `size` (défaut: 20, max: 100)

**Réponse :**
```json
{
  "feedbacks": [...],
  "totalCount": 42,
  "page": 0,
  "totalPages": 3
}
```

### GET /api/v1/scoring/feedback/stats

Statistiques agrégées.

**Paramètres :** `tenantId` (requis), `deviceId` (optionnel)

**Réponse :**
```json
{
  "confirmedDefects": 15,
  "falsePositives": 3,
  "inconclusive": 7,
  "noDefectFound": 2,
  "adjustmentsApplied": 0,
  "adjustmentsSuggested": 12
}
```

### GET /api/v1/scoring/feedback/adjustments

Historique des ajustements de scoring pour un appareil.

**Paramètres :** `deviceId` (requis), `anomalyType` (optionnel)

## Métriques (Micrometer)

| Métrique | Description |
|----------|-------------|
| `pyrosense.scoring.confirmed_defects_total` | Nombre total de défauts confirmés |
| `pyrosense.scoring.false_positives_total` | Nombre total de faux positifs |
| `pyrosense.scoring.inconclusive_feedback_total` | Feedbacks inconclusifs |
| `pyrosense.scoring.scoring_adjustments_total` | Ajustements de scoring générés |

## Schéma de Données

### scoring_feedbacks

| Colonne | Type | Description |
|---------|------|-------------|
| id | UUID | Identifiant unique |
| device_id | UUID | Appareil concerné |
| tenant_id | VARCHAR | Tenant |
| alert_id | UUID | Alerte originale (nullable) |
| intervention_id | UUID | Intervention liée (nullable) |
| outcome | VARCHAR | CONFIRMED_DEFECT, FALSE_POSITIVE, etc. |
| anomaly_type | VARCHAR | Type d'anomalie (MICRO_ARC, THD_DRIFT, etc.) |
| risk_score_at_alert | DOUBLE | Score de risque au moment de l'alerte |
| feedback_source | VARCHAR | Source (maintenance-service, manual, etc.) |
| comment | TEXT | Commentaire libre du technicien |
| received_at | TIMESTAMPTZ | Date de réception |

### scoring_adjustments

| Colonne | Type | Description |
|---------|------|-------------|
| id | UUID | Identifiant unique |
| device_id | UUID | Appareil concerné |
| tenant_id | VARCHAR | Tenant |
| anomaly_type | VARCHAR | Type d'anomalie |
| adjustment_type | VARCHAR | CONFIDENCE_BOOST, CONFIDENCE_REDUCTION |
| previous_value | DOUBLE | Confiance avant ajustement |
| new_value | DOUBLE | Confiance après ajustement |
| delta | DOUBLE | Variation appliquée |
| mode | VARCHAR | SUGGESTION_ONLY ou APPLIED |
| reason | TEXT | Explication de l'ajustement |
| applied_at | TIMESTAMPTZ | Date |

## Labels ML

Chaque feedback enrichit le futur dataset ML :

- `CONFIRMED_DEFECT` → label `MICRO_ARC_CONFIRMED`, `INSULATION_DEGRADATION_CONFIRMED`, etc.
- `FALSE_POSITIVE` → label `FALSE_POSITIVE`
- `NO_DEFECT_FOUND` → label `FALSE_POSITIVE` (suspicion)
- `INCONCLUSIVE` → label `INCONCLUSIVE`

## Contraintes de Sécurité

- Séparation par tenant stricte
- Pas d'accès aux données électriques brutes via cette API
- Ajustements traçables et auditables
- Mode suggestion par défaut → pas d'impact production sans validation humaine
