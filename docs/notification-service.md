# Notification Service — PyroSense AI Platform

## Vue d'ensemble

Service de notification multi-canal qui délivre les alertes, informations d'intervention, rapports et événements critiques aux bons destinataires, via le bon canal, sans spam.

## Architecture

```
Kafka Topics (5)
  alerting-events
  scoring-events
  maintenance-events
  reporting-events
  device-events
       │
       ▼
KafkaAlertEventListener
       │
       ▼
ProcessNotificationEventUseCase (8 event types)
       │
       ▼
SendNotificationService
  ├── ChannelRoutingPolicy (severity → channels)
  ├── RecipientResolverPort (tenant → recipients)
  ├── NotificationPreferences (respect user prefs except CRITICAL)
  ├── DeduplicationPort (Redis 30min window)
  └── NotificationDispatcher
        ├── EmailProviderPort
        ├── SmsProviderPort
        ├── PushProviderPort
        ├── WebhookProviderPort
        └── DASHBOARD (persist only)
```

## Canaux

| Canal | Implémentation | Description |
|-------|---------------|-------------|
| DASHBOARD | Persist en base | Visible dans l'IHM |
| EMAIL | LoggingEmailProvider (MVP) | Email avec subject/body |
| SMS | LoggingSmsProvider (MVP) | Message court |
| PUSH | LoggingPushProvider (MVP) | Notification push mobile |
| WEBHOOK | LoggingWebhookProvider (MVP) | Callback HTTP |

## Événements consommés

| Événement | Topic | Action |
|-----------|-------|--------|
| `alerting.alert.created` | alerting-events | Notification par sévérité |
| `alerting.alert.escalated` | alerting-events | Notification [ESCALATION] |
| `scoring.critical.risk.detected` | scoring-events | Notification CRITIQUE au gestionnaire |
| `maintenance.intervention.created` | maintenance-events | Notification au gestionnaire |
| `maintenance.intervention.assigned` | maintenance-events | Notification à l'électricien |
| `maintenance.intervention.completed` | maintenance-events | Notification de complétion |
| `reporting.report.generated` | reporting-events | Notification au demandeur |
| `device.offline.detected` | device-events | Notification au gestionnaire |

## Domaine

### Entités
- **Notification** — Aggregate principal (id, tenant, recipient, channel, severity, status, retry logic)
- **NotificationDeliveryAttempt** — Historique de chaque tentative d'envoi
- **NotificationPreferences** — Préférences utilisateur par canal + heures calmes

### Enums
- **NotificationStatus**: PENDING, SENT, FAILED, RETRYING, CANCELLED, SUPPRESSED
- **NotificationChannel**: EMAIL, SMS, PUSH, WEBHOOK, DASHBOARD
- **RecipientType**: OCCUPANT, PROPERTY_MANAGER, ELECTRICIAN, TENANT_ADMIN

## Règles de routage

| Sévérité | Canaux | Destinataires |
|----------|--------|---------------|
| INFO | DASHBOARD | PROPERTY_MANAGER, TENANT_ADMIN |
| WARNING | EMAIL + DASHBOARD | PROPERTY_MANAGER, TENANT_ADMIN, OCCUPANT |
| CRITICAL | SMS + PUSH + EMAIL + DASHBOARD | Tous (PROPERTY_MANAGER, TENANT_ADMIN, OCCUPANT, ELECTRICIAN) |

## Retry avec backoff exponentiel

| Tentative | Délai |
|-----------|-------|
| 1 | 30 secondes |
| 2 | 2 minutes |
| 3 | 10 minutes |
| Épuisé | Status FAILED |

## Endpoints REST

| Méthode | URL | Description |
|---------|-----|-------------|
| GET | /api/v1/notifications | Liste notifications (filtre status, pagination) |
| GET | /api/v1/notifications/{id} | Détail notification |
| POST | /api/v1/notifications/{id}/retry | Relancer une notification en échec |
| GET | /api/v1/notification-preferences/me | Mes préférences |
| PUT | /api/v1/notification-preferences/me | Mettre à jour mes préférences |
| GET | /api/v1/notification-preferences/{userId} | Préférences d'un utilisateur (admin) |
| PUT | /api/v1/notification-preferences/{userId} | Modifier préférences (admin) |
| GET | /api/v1/notifications/statistics | Compteurs par status |

## Configuration

```yaml
pyrosense:
  notification:
    kafka:
      alerting-topic: alerting-events
      scoring-topic: scoring-events
      maintenance-topic: maintenance-events
      reporting-topic: reporting-events
      device-topic: device-events
    retry:
      interval-ms: 30000
      max-retries: 3
    deduplication:
      window-minutes: 30
```

## Sécurité

- JWT OAuth2 + TenantContext
- @PreAuthorize par endpoint et rôle
- Masquage PII dans les logs (email, phone)
- Isolation tenant sur toutes les requêtes

## Tests

- 72 tests passent (0 failures)
- Unitaires domain (NotificationTest, ChannelRoutingPolicyTest, RecipientTest, NotificationTemplateTest, NotificationDeliveryAttemptTest, PreferenceBypassTest, DeduplicationAntiSpamTest)
- Unitaires use case (SendNotificationServiceTest, RetryNotificationServiceTest, ProcessNotificationEventServiceTest)
- Architecture (10 règles ArchUnit)
- Intégration (context load + embedded Kafka)
