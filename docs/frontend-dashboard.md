# PyroSense Frontend Dashboard — Documentation technique

## Vue d'ensemble

Le dashboard PyroSense est une application Angular SaaS B2B qui fournit une interface de supervision pour la detection preventive de risques electriques dans les batiments. Il consomme les APIs REST des microservices backend et affiche des donnees en temps reel via WebSocket.

## Architecture

```
┌───────────────────────────────────────────────────────┐
│ Browser (Angular 19 SPA)                              │
├───────────────────────────────────────────────────────┤
│ Layout: Sidebar + Topbar + Content                    │
├───────────────────────────────────────────────────────┤
│ Features (lazy-loaded standalone components)          │
│  dashboard | buildings | devices | alerts |           │
│  interventions | reports | notifications | settings   │
├───────────────────────────────────────────────────────┤
│ Core Services                                         │
│  ApiService | AuthService | TenantService | WS        │
├───────────────────────────────────────────────────────┤
│ Interceptors Chain                                    │
│  auth → tenant → correlation → error                 │
├───────────────────────────────────────────────────────┤
│ Keycloak SSO                   │ STOMP WebSocket      │
└────────────────────────────────┴──────────────────────┘
```

## Flux d'authentification

1. L'application demarre avec `APP_INITIALIZER` qui initialise Keycloak (silent SSO)
2. Si non authentifie, `authGuard` redirige vers Keycloak login
3. Apres login, le token JWT est parse pour extraire: `sub`, `email`, `name`, `tenant_id`, `roles`
4. L'`authInterceptor` injecte `Authorization: Bearer <token>` sur chaque requete
5. Le `tenantInterceptor` ajoute `X-Tenant-Id` pour l'isolation multi-tenant

## Composants reutilisables (shared/)

| Composant | Usage |
|-----------|-------|
| `MetricCardComponent` | Carte KPI avec icone, valeur, label, lien et variante de couleur |
| `ConfirmDialogComponent` | Dialog Material pour confirmer les actions destructives |
| `SeverityBadgeComponent` | Badge colore selon la severite (CRITICAL, WARNING, INFO) |
| `StatusChipComponent` | Chip colore selon le statut (OPEN, IN_PROGRESS, RESOLVED, etc.) |
| `DeviceStatusBadgeComponent` | Badge statut capteur (ACTIVE, OFFLINE, PROVISIONED, REVOKED) |
| `EmptyStateComponent` | Etat vide avec icone et message |
| `SkeletonLoaderComponent` | Placeholder de chargement anime |
| `RiskGaugeComponent` | Jauge circulaire de score de risque |
| `LiveAlertToastComponent` | Toast temps reel pour nouvelles alertes |

## Pipes

| Pipe | Description |
|------|-------------|
| `riskLevel` | Convertit un score numerique en niveau (LOW/MODERATE/HIGH/CRITICAL) |
| `riskColor` | Convertit un score en code couleur hex |
| `relativeTime` | Formate une date ISO en temps relatif en francais |

## Ecrans

### Dashboard (/)
- 6 cartes KPI: batiments, capteurs actifs, capteurs offline, alertes critiques, alertes warning, interventions en retard
- Jauge de score de risque moyen avec tendance
- Graphique Chart.js de l'evolution du risque sur 30 jours
- Rafraichissement automatique (60s) + WebSocket pour temps reel

### Batiments (/buildings)
- Liste filtrable avec score de risque, severite max, nombre de capteurs
- Detail: onglets capteurs, tableaux electriques, alertes, interventions
- Badge de severite la plus haute

### Capteurs (/devices)
- Liste avec recherche, filtres par statut/batiment
- Statistiques (total, actifs, offline, provisioned)
- Detail: telemetrie temps reel, anomalies, historique

### Alertes (/alerts)
- Liste avec filtres statut/severite, statistiques
- Detail: timeline, commentaires, actions (acknowledge, resolve, assign, false positive)
- Formulaire d'assignation

### Interventions (/interventions)
- Vue Kanban (drag conceptuel par colonnes statut)
- Statistiques: total, en cours, completees, faux positifs, reduction risque moyenne
- Detail: diagnostic, impact risque, lifecycle complet

### Rapports (/reports)
- Liste avec filtre par type (MONTHLY, COMPLIANCE, INCIDENT)
- Generation de rapport, telechargement securise (token temporaire)

### Notifications (/notifications)
- Historique des notifications envoyees
- Filtres par statut (SENT, FAILED, PENDING)
- Statistiques d'envoi

### Parametres (/settings)
- Configuration tenant (nom, adresse, telephone, seuils de risque, alertes email)
- Contacts d'urgence (CRUD)
- Preferences de notification par utilisateur
- Acces restreint aux roles PLATFORM_ADMIN et TENANT_ADMIN

## Gestion d'erreur

L'`errorInterceptor` centralise le traitement des erreurs HTTP:

| Code | Comportement |
|------|-------------|
| 401 | Redirection vers Keycloak login |
| 403 | Snackbar "Acces refuse" + navigation dashboard |
| 429 | Snackbar "Trop de requetes" |
| 0 | Snackbar "Probleme de connexion reseau" |
| 5xx | Snackbar avec reference correlation-id |

## WebSocket temps reel

- Protocole: STOMP over WebSocket (`/ws`)
- Topics par tenant: `/topic/tenant.{id}.alerts`, `/topic/tenant.{id}.dashboard`
- Topics par device: `/topic/device.{id}.telemetry`
- Reconnexion automatique avec backoff (5s)
- Indicateur visuel dans la topbar (wifi/wifi_off)
- Toast live pour les nouvelles alertes (10s pour CRITICAL, 5s pour autres)

## Responsive

| Breakpoint | Comportement |
|------------|-------------|
| > 960px | Sidebar ouverte, grille 6 colonnes |
| 600-960px | Sidebar en overlay, grille 3 colonnes |
| < 600px | Sidebar overlay, grille 2 colonnes, padding reduit, tenant name cache |

## Securite

- Tokens JWT Keycloak avec refresh automatique
- RBAC: `roleGuard` sur les routes sensibles
- `unsavedChangesGuard` avec dialog de confirmation Material
- Isolation tenant: header `X-Tenant-Id` sur toutes les requetes
- `SUPPORT_READONLY` desactive tous les boutons d'action via `auth.isReadOnly`
- Correlation-ID sur chaque requete pour tracabilite

## Tests

- **Unitaires** (Karma/Jasmine): composants, services, interceptors, guards, pipes
- **E2E** (Cypress): flows critiques (login, dashboard, alertes)
- Commande: `npm test` (unitaires), `npx cypress run` (E2E)

## Performance

- Lazy loading de tous les feature components
- Signals pour l'etat reactif (pas de zone.js overhead inutile)
- Cache navigateur via service worker (production)
- Images optimisees, tree-shaking, ahead-of-time compilation
- Skeleton loaders pour perceived performance
