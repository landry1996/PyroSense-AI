# PyroSense Dashboard

Frontend Angular pour la plateforme PyroSense AI — interface SaaS B2B pour gestionnaires immobiliers, bailleurs sociaux et syndics.

## Stack technique

- Angular 19 (standalone components, signals)
- TypeScript strict
- Angular Material
- Chart.js (graphiques de tendances)
- Keycloak (authentification SSO)
- STOMP/WebSocket (temps reel)
- Karma/Jasmine (tests unitaires)
- Cypress (tests E2E)

## Demarrage

```bash
npm install
npm start
```

L'application demarre sur `http://localhost:4200` et utilise un proxy vers les APIs backend.

## Structure

```
src/app/
├── core/               # Services singleton, guards, interceptors, models
│   ├── guards/         # auth, role, unsaved-changes
│   ├── interceptors/   # JWT, correlation-id, tenant-id, error
│   ├── models/         # Interfaces TypeScript du domaine
│   ├── layout/         # Layout principal (sidebar + topbar)
│   └── services/       # auth, api, tenant, websocket
├── shared/             # Composants, pipes, utils reutilisables
│   ├── components/     # metric-card, confirm-dialog, severity-badge, etc.
│   ├── pipes/          # risk-level, relative-time
│   └── utils/          # risk.utils
├── features/           # Modules fonctionnels (lazy-loaded)
│   ├── dashboard/      # Vue d'ensemble avec KPIs et graphiques
│   ├── buildings/      # Liste et detail des batiments
│   ├── devices/        # Gestion des capteurs IoT
│   ├── alerts/         # Alertes avec filtres et actions
│   ├── interventions/  # Kanban board + detail interventions
│   ├── reports/        # Rapports periodiques
│   ├── notifications/  # Historique des notifications
│   ├── settings/       # Parametres tenant et notifications
│   └── admin/          # Administration plateforme
```

## Commandes

| Commande | Description |
|----------|-------------|
| `npm start` | Serveur de dev (port 4200) |
| `npm test` | Tests unitaires (Karma) |
| `npm run test:ci` | Tests en mode CI (headless) |
| `npm run build` | Build de production |
| `npm run lint` | Linting ESLint |
| `npx cypress open` | Tests E2E interactifs |

## Architecture

- **Hexagonale frontend** : les composants ne communiquent pas directement avec les APIs. Ils utilisent des services (`ApiService`, `DashboardStateService`) qui encapsulent la logique HTTP.
- **Signals** : etat reactif sans RxJS pour l'etat local des composants.
- **Interceptors** : chaine JWT → Tenant → Correlation → Error sur chaque requete HTTP.
- **Guards** : protection par authentification et role, confirmation de navigation.
- **Responsive** : breakpoints 960px (tablette) et 600px (mobile), sidebar collapsible.

## Roles supportes

| Role | Acces |
|------|-------|
| PLATFORM_ADMIN | Tout + admin |
| TENANT_ADMIN | Tout (propre tenant) + settings |
| PROPERTY_MANAGER | Dashboard, batiments, alertes, interventions |
| ELECTRICIAN | Dashboard, interventions assignees |
| SUPPORT_READONLY | Lecture seule, actions desactivees |

## Configuration

Le fichier `proxy.conf.json` redirige `/api/*` vers le backend (par defaut `http://localhost:8080`).

Variables d'environnement Keycloak dans `src/app/app.config.ts`.

## Docker

```bash
docker build -t pyrosense-dashboard .
docker run -p 80:80 pyrosense-dashboard
```

Le conteneur utilise nginx avec la configuration `nginx.conf` pour le SPA routing.
