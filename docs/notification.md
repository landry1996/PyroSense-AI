# PyroSense Notification Service

## Rôle

Envoyer les notifications selon le niveau d'alerte et le profil utilisateur, via 5 canaux différents avec respect du consentement RGPD.

## Canaux

| Canal | Description | Déclencheur |
|-------|-------------|-------------|
| `DASHBOARD` | Notification in-app | INFO, WARNING, CRITICAL |
| `EMAIL` | Email via SMTP | WARNING, CRITICAL |
| `SMS` | SMS via provider | CRITICAL |
| `PUSH` | Push mobile (FCM/APNs) | CRITICAL |
| `WEBHOOK` | Webhook partenaire | CRITICAL |

## Routing par sévérité

| Sévérité | Canaux activés | Destinataires |
|----------|---------------|---------------|
| `INFO` | Dashboard | Gestionnaire, Admin tenant |
| `WARNING` | Email + Dashboard | Gestionnaire, Admin tenant, Occupant |
| `CRITICAL` | SMS + Push + Email + Dashboard | Gestionnaire, Admin tenant, Occupant, Électricien |

## Architecture

```
adapter/
├── in/rest/           → NotificationController (API historique)
├── in/messaging/      → KafkaAlertEventListener (consomme alerting-events)
├── out/provider/      → LoggingEmailProvider, LoggingSmsProvider, LoggingPushProvider, LoggingWebhookProvider
├── out/resolver/      → StubRecipientResolver (MVP, à remplacer)
├── out/deduplication/ → InMemoryDeduplicationAdapter
└── out/persistence/   → JdbcNotificationRepository
application/
├── port/in/           → SendNotificationUseCase, GetNotificationQuery, RetryNotificationUseCase
├── port/out/          → EmailProviderPort, SmsProviderPort, PushProviderPort, WebhookProviderPort, RecipientResolverPort, DeduplicationPort, NotificationRepositoryPort
└── usecase/           → SendNotificationService, GetNotificationService, RetryNotificationService, NotificationDispatcher
domain/model/          → Notification, NotificationStatus, NotificationChannel, RecipientType, Recipient, NotificationTemplate, ChannelRoutingPolicy, DeduplicationKey
config/                → SecurityConfig, UseCaseConfig, KafkaConfig, RetryScheduler
```

## Cycle de vie d'une notification

```
PENDING → SENT        (envoi réussi)
PENDING → RETRYING    (échec, tentatives restantes)
RETRYING → SENT       (retry réussi)
RETRYING → RETRYING   (échec, encore des tentatives)
RETRYING → FAILED     (max retries atteint = 3)
```

## Retry avec backoff

| Tentative | Délai |
|-----------|-------|
| 1ère | 30 secondes |
| 2ème | 2 minutes |
| 3ème | 10 minutes |

Après 3 échecs → statut `FAILED`, envoi en DLQ.

## Anti-spam / Déduplication

Chaque notification a un `alertFingerprint` composé de `alertId:deviceId:severity`. Combiné avec le `recipientId` et le `channel`, cela forme une clé de déduplication.

Fenêtre de déduplication : **30 minutes**. Une même notification ne sera pas renvoyée au même destinataire sur le même canal pendant cette période.

## API REST

### Historique des notifications
```
GET /api/v1/notifications?tenantId=uuid&status=SENT
GET /api/v1/notifications/{id}
GET /api/v1/notifications/recipient/{recipientId}
GET /api/v1/notifications/statistics?tenantId=uuid
```

### Statistiques
Retourne le nombre de notifications par statut (sent, failed, pending, retrying).

## Événement consommé

| Événement | Topic | Action |
|-----------|-------|--------|
| `alerting.alert.created` | alerting-events | Dispatch notifications selon sévérité |

## Sécurité & RGPD

- **Masquage des données personnelles** : les numéros de téléphone et tokens push ne sont jamais loggés en clair
  - Téléphone : `***1234` (4 derniers chiffres)
  - Email : `j***e@domain.com`
  - Token push : `push***abc1`
- **Consentement** : chaque `Recipient` porte des flags `consentEmail`, `consentSms`, `consentPush`
- **Pas d'envoi sans consentement** : `Recipient.canReceive(channel)` vérifie le consentement avant tout dispatch
- **Pas de données PII en base** : seuls les IDs de destinataires sont stockés (pas les coordonnées)
- **Logs** : aucun contenu de notification dans les logs, seulement IDs et statuts

## Templates

Templates intégrés par sévérité avec variables substituables :
- `{alertType}` — type d'alerte
- `{deviceId}` — identifiant de l'appareil
- `{occurredAt}` — horodatage de l'événement

## Configuration

| Variable | Description | Défaut |
|----------|-------------|--------|
| `DB_USERNAME` | PostgreSQL user | pyrosense |
| `DB_PASSWORD` | PostgreSQL password | pyrosense |
| `KAFKA_SERVERS` | Kafka bootstrap servers | localhost:9092 |
| `KEYCLOAK_ISSUER` | Keycloak issuer URI | http://localhost:8180/realms/pyrosense |
| `pyrosense.notification.retry.interval-ms` | Intervalle du scheduler de retry | 30000 |

Port : **8087**

## Tests

- **Domain** : NotificationTest (8), ChannelRoutingPolicyTest (6), RecipientTest (7), NotificationTemplateTest (6)
- **Use cases** : SendNotificationServiceTest (7), RetryNotificationServiceTest (3)
- **Architecture** : NotificationArchitectureTest (10) — ArchUnit hexagonal
- **Context** : NotificationServiceApplicationTest (1) — embedded Kafka

## Évolutions prévues

- Remplacer `StubRecipientResolver` par un appel au Identity Service
- Remplacer les Logging*Provider par de vrais adaptateurs (SendGrid, Twilio, Firebase)
- Redis pour la déduplication (remplacement de l'InMemory)
- DLQ Kafka pour les notifications définitivement échouées
- Préférences de notification par utilisateur (horaires, fréquence max)
- Templating avancé (Thymeleaf ou Mustache pour HTML emails)
