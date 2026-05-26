# Templates de notification PyroSense AI

## Vue d'ensemble

Le systeme de templates gere le contenu des notifications envoyees aux utilisateurs via 4 canaux (dashboard, email, SMS, push) pour 9 types d'evenements. Chaque template definit un contenu adapte au canal, un niveau de priorite, un call-to-action, et des regles de confidentialite.

## Architecture

```
NotificationTemplateCode (enum)       -- identifiant type du template
NotificationTemplateDefinition        -- definition complete (4 canaux, CTA, privacy)
NotificationTemplateRegistry          -- registre statique de tous les templates
TemplateRendererPort                  -- port hexagonal (rendering)
DefaultTemplateRenderer               -- implementation simple (substitution {var})
```

### Extension future

L'architecture est preparee pour un moteur de template (Thymeleaf, Mustache) :
- Implementer `TemplateRendererPort` avec le moteur choisi
- Les `NotificationTemplateDefinition` fournissent les templates texte brut
- Le registre est consulte par le renderer pour obtenir le contenu par code + canal

## Canaux

| Canal | Taille max | Format | Usage |
|-------|-----------|--------|-------|
| Dashboard | ~200 chars | Texte court | Toast/notification in-app |
| Email | Illimite | Texte structure (salutation, details, recommandations, disclaimer) | Communication detaillee |
| SMS | 160 chars | Texte ultra-court | Alertes urgentes, pas de PII |
| Push | ~100 chars | Titre + phrase | Notification mobile |

## Evenements et templates

### 1. Alerte WARNING (`ALERT_WARNING`)

| Propriete | Valeur |
|-----------|--------|
| Priorite | MEDIUM |
| Canaux | EMAIL, DASHBOARD |
| Call-to-action | Consulter le tableau de bord |
| Variables | `alertType`, `deviceId`, `buildingId`, `occurredAt` |
| Confidentialite | NO_RAW_ELECTRICAL_DATA, NO_GUARANTEE_FIRE_PREDICTION |

**Ton** : informatif, pas alarmiste. Recommande une verification sous 48h.

### 2. Alerte CRITICAL (`ALERT_CRITICAL`)

| Propriete | Valeur |
|-----------|--------|
| Priorite | URGENT |
| Canaux | SMS, PUSH, EMAIL, DASHBOARD |
| Call-to-action | Contacter un professionnel qualifie |
| Variables | `alertType`, `buildingId`, `occurredAt` |
| Confidentialite | NO_RAW_ELECTRICAL_DATA, NO_GUARANTEE_FIRE_PREDICTION, NO_EXACT_LOCATION |

**Regles editoriales CRITICAL** :
- Ne pas paniquer l'utilisateur (pas de "incendie imminent")
- Recommander une action claire (contacter un professionnel)
- Indiquer le batiment concerne (pas de localisation exacte)
- Demander de contacter un professionnel qualifie
- Ne pas garantir qu'un incendie va se produire ("ne garantit pas la survenue")
- Ne pas afficher de donnees electriques brutes (pas de THD, amperes, volts)
- Inclure le disclaimer "aide a la decision" et "ne se substitue pas a un diagnostic professionnel"

### 3. Risque critique detecte (`CRITICAL_RISK_DETECTED`)

| Propriete | Valeur |
|-----------|--------|
| Priorite | URGENT |
| Canaux | SMS, PUSH, EMAIL, DASHBOARD |
| Call-to-action | Planifier une inspection professionnelle |
| Variables | `buildingId`, `riskScore`, `occurredAt` |
| Confidentialite | NO_RAW_ELECTRICAL_DATA, NO_GUARANTEE_FIRE_PREDICTION, NO_EXACT_LOCATION |

**Ton** : serieux mais pas alarmiste. Score presente comme "estimation probabiliste".

### 4. Intervention creee (`INTERVENTION_CREATED`)

| Propriete | Valeur |
|-----------|--------|
| Priorite | MEDIUM |
| Canaux | EMAIL, DASHBOARD |
| Call-to-action | Suivre l'intervention |
| Variables | `interventionType`, `buildingId`, `occurredAt` |
| Confidentialite | NO_RAW_ELECTRICAL_DATA |

### 5. Intervention assignee (`INTERVENTION_ASSIGNED`)

| Propriete | Valeur |
|-----------|--------|
| Priorite | HIGH |
| Canaux | PUSH, EMAIL, DASHBOARD |
| Call-to-action | Consulter les details de l'intervention |
| Variables | `interventionType`, `buildingId`, `occurredAt` |
| Confidentialite | NO_RAW_ELECTRICAL_DATA, NO_PERSONAL_INFO_IN_PUSH |

### 6. Intervention completee (`INTERVENTION_COMPLETED`)

| Propriete | Valeur |
|-----------|--------|
| Priorite | LOW |
| Canaux | EMAIL, DASHBOARD |
| Call-to-action | Consulter le rapport d'intervention |
| Variables | `interventionType`, `buildingId`, `occurredAt` |
| Confidentialite | NO_RAW_ELECTRICAL_DATA |

### 7. Rapport genere (`REPORT_GENERATED`)

| Propriete | Valeur |
|-----------|--------|
| Priorite | LOW |
| Canaux | EMAIL, DASHBOARD |
| Call-to-action | Telecharger le rapport |
| Variables | `reportType`, `reportNumber`, `buildingId`, `occurredAt` |
| Confidentialite | NO_RAW_ELECTRICAL_DATA |

### 8. Capteur offline (`DEVICE_OFFLINE`)

| Propriete | Valeur |
|-----------|--------|
| Priorite | HIGH |
| Canaux | EMAIL, PUSH, DASHBOARD |
| Call-to-action | Verifier l'etat du capteur |
| Variables | `deviceId`, `buildingId`, `occurredAt` |
| Confidentialite | MASK_DEVICE_ID_IN_SMS, NO_EXACT_LOCATION |

**Note** : le template SMS mentionne que la zone n'est plus surveillee.

### 9. Capteur revenu online (`DEVICE_BACK_ONLINE`)

| Propriete | Valeur |
|-----------|--------|
| Priorite | LOW |
| Canaux | EMAIL, DASHBOARD |
| Call-to-action | Aucune action requise |
| Variables | `deviceId`, `buildingId`, `occurredAt` |
| Confidentialite | MASK_DEVICE_ID_IN_SMS |

## Regles de confidentialite

| Regle | Description |
|-------|-------------|
| `NO_RAW_ELECTRICAL_DATA` | Ne jamais inclure de valeurs techniques brutes (amperes, volts, THD, kW) |
| `NO_EXACT_LOCATION` | Indiquer le batiment mais pas l'adresse exacte |
| `NO_PERSONAL_INFO_IN_PUSH` | Pas de noms/emails dans les notifications push (visibles lock screen) |
| `MASK_DEVICE_ID_IN_SMS` | Masquer ou abreger l'identifiant technique dans les SMS |
| `NO_GUARANTEE_FIRE_PREDICTION` | Ne jamais affirmer qu'un incendie va se produire |

## Variables dynamiques

| Variable | Description | Exemple |
|----------|-------------|---------|
| `alertType` | Type d'anomalie detectee | TEMPERATURE_RISE, MICRO_ARC |
| `deviceId` | Identifiant du capteur | sensor-042 |
| `buildingId` | Nom ou ID du batiment | Immeuble Haussmann |
| `occurredAt` | Date/heure de l'evenement | 2025-06-15T14:30:00Z |
| `riskScore` | Score de risque (0-100) | 87 |
| `interventionType` | Type d'intervention | CORRECTIVE, PREVENTIVE |
| `reportType` | Type de rapport | MONTHLY_HEALTH |
| `reportNumber` | Numero unique du rapport | MH-202506-00001 |

## Tests

### Tests de rendu avec variables (NotificationTemplateRegistryTest)
- Substitution correcte de toutes les variables
- Rendu par canal (email plus long que SMS/dashboard)
- SMS respecte la limite 160 caracteres
- Dashboard respecte la limite 200 caracteres

### Tests de variables manquantes
- Detection des variables requises absentes
- Variable `null` traitee comme manquante
- Placeholder non substitue reste visible dans le rendu (pas d'exception)

### Tests de conformite editoriale CRITICAL
- Pas de panique (pas de "feu imminent")
- Recommandation claire (professionnel qualifie)
- Indication du batiment
- Pas de garantie d'incendie
- Pas de donnees electriques brutes
- Disclaimer "aide a la decision"

### Tests DefaultTemplateRenderer
- Backward-compatible avec l'API legacy (string keys)
- API channel-aware fonctionne pour tous les codes et canaux
- Email body plus long que SMS/dashboard pour CRITICAL

## Implementation technique

```java
// Obtenir un template
NotificationTemplateDefinition def = NotificationTemplateRegistry.get(ALERT_CRITICAL);

// Verifier les variables
List<String> missing = def.missingVariables(variables);

// Rendre pour un canal specifique
String subject = def.renderSubject(NotificationChannel.EMAIL, variables);
String body = def.render(NotificationChannel.EMAIL, variables);

// Via le port (injection)
String subject = templateRenderer.renderSubject(ALERT_CRITICAL, NotificationChannel.SMS, vars);
String body = templateRenderer.renderBody(ALERT_CRITICAL, NotificationChannel.SMS, vars);
```

## Roadmap

- [ ] Moteur de template (Thymeleaf) pour emails HTML riches
- [ ] Internationalisation (i18n) des templates
- [ ] Templates personnalisables par tenant
- [ ] A/B testing sur le wording des notifications
- [ ] Templates pour notifications digest (resume quotidien)
