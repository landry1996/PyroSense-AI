# Notification Rules — PyroSense AI Platform

## Règles de routage par sévérité

### Règle 1 — INFO → Dashboard uniquement
Les notifications INFO sont stockées en base et visibles dans le tableau de bord. Aucun envoi externe.

### Règle 2 — WARNING → Dashboard + Email
Les alertes WARNING déclenchent un email et une entrée dashboard. Le SMS et le push ne sont pas utilisés pour ce niveau.

### Règle 3 — CRITICAL → Dashboard + Email + SMS + Push
Les alertes CRITICAL utilisent tous les canaux disponibles pour garantir la réception rapide.

## Règles événementielles

### Règle 4 — Intervention assignée → Notification à l'électricien
Quand une intervention est assignée (`maintenance.intervention.assigned`), l'électricien concerné reçoit une notification PUSH + EMAIL + DASHBOARD.

### Règle 5 — Rapport généré → Notification au demandeur
Quand un rapport est généré (`reporting.report.generated`), le PROPERTY_MANAGER et le TENANT_ADMIN reçoivent un EMAIL + DASHBOARD.

### Règle 6 — Device offline critique → Gestionnaire
Quand un capteur est détecté hors ligne (`device.offline.detected`), le gestionnaire reçoit une notification EMAIL + PUSH + DASHBOARD.

## Règles anti-spam

### Règle 7 — Fenêtre de déduplication
Ne pas renvoyer la même notification (même fingerprint = alertId:deviceId:severity, même recipient, même canal) dans une fenêtre configurable (défaut: 30 minutes).

**Exception** : une escalade (`alerting.alert.escalated`) bypasse la déduplication car le severity change, générant un nouveau fingerprint.

**Implémentation** : Redis SET avec TTL = `pyrosense.notification.deduplication.window-minutes`.

### Règle 8 — Respect des préférences sauf CRITICAL
Les préférences utilisateur (email_enabled, sms_enabled, push_enabled) sont respectées pour INFO et WARNING. Pour CRITICAL, **toutes les notifications sont envoyées** indépendamment des préférences.

Le canal DASHBOARD n'est jamais filtré par les préférences.

### Règle 9 — Masquage des données sensibles
Les logs ne contiennent jamais :
- Adresses email complètes → `j***e@domain.com`
- Numéros de téléphone → `***1234`
- Tokens push → `abcd***wxyz`

### Règle 10 — DLQ en cas d'échec persistant
Après 3 tentatives (backoff: 30s, 2min, 10min), la notification passe en status `FAILED`. Les notifications en `FAILED` sont candidates à la Dead Letter Queue pour analyse manuelle.

## Matrice décisionnelle

```
Événement reçu
     │
     ▼
Routage canaux (severity → channels)
     │
     ▼
Résolution destinataires (tenant + recipientTypes)
     │
     ▼
Pour chaque (recipient, channel):
  ├── recipient.canReceive(channel)? ──── Non → Skip
  ├── preferences.isEnabled(channel)? ─── Non + severity != CRITICAL → Skip (SUPPRESSED)
  ├── deduplication.isDuplicate(key)? ──── Oui → Skip
  └── dispatch
       ├── Succès → markSent() + markSent(dedup)
       └── Échec → markFailed(reason)
                    ├── retryCount < 3 → RETRYING (backoff)
                    └── retryCount >= 3 → FAILED (DLQ candidate)
```

## Configuration

| Paramètre | Défaut | Description |
|-----------|--------|-------------|
| `deduplication.window-minutes` | 30 | Fenêtre anti-spam en minutes |
| `retry.interval-ms` | 30000 | Intervalle de retry scheduler |
| `retry.max-retries` | 3 | Nombre max de tentatives |

## Statuts de notification

| Status | Description | Terminal? |
|--------|-------------|----------|
| PENDING | Créée, pas encore envoyée | Non |
| SENT | Envoyée avec succès | Oui |
| RETRYING | En attente de retry | Non |
| FAILED | Échec après max retries | Oui |
| CANCELLED | Annulée manuellement | Oui |
| SUPPRESSED | Supprimée par les préférences | Oui |
