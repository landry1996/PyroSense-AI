# PyroSense Dashboard — Plan UX/API Complet

## 1. Analyse des Ecarts (Spec vs MVP 2 Livraison)

### Legende

| Symbole | Signification |
|---------|---------------|
| OK | Implemente et conforme |
| PARTIAL | Implemente mais incomplet |
| MISSING | Non implemente |

### Ecran 1: Dashboard Global

| Exigence Spec | Statut | Detail |
|---------------|--------|--------|
| Nombre de batiments | MISSING | Aucun compteur batiments |
| Score de risque moyen | OK | `avgRiskScore` affiche |
| Alertes critiques actives | OK | `criticalAlerts` affiche |
| Capteurs hors-ligne | MISSING | Pas de compteur offline |
| Interventions en retard | MISSING | Pas de compteur retard |
| Graphique evolution risque 30j | MISSING | Pas de chart historique |
| Raccourcis rapides (batiments, alertes, interventions) | MISSING | Pas de liens rapides |
| Donnees reelles (pas de mock) | PARTIAL | Fallback mock si API echoue |

### Ecran 2: Liste Batiments

| Exigence Spec | Statut | Detail |
|---------------|--------|--------|
| Liste depuis API reelle | MISSING | Donnees hardcodees dans le composant |
| Indicateur statut (OK/WATCH/AT_RISK/CRITICAL) | MISSING | Pas de badge statut |
| Adresse, nombre capteurs, score risque | PARTIAL | Mock: nom+adresse seulement |
| Recherche/filtre | MISSING | Aucun filtre |
| Navigation vers detail | OK | RouterLink present |

### Ecran 3: Detail Batiment

| Exigence Spec | Statut | Detail |
|---------------|--------|--------|
| Tableau capteurs + polling 30s | OK | Implemente avec mock fallback |
| Chart historique risque | MISSING | Pas de graphique |
| Onglet alertes du batiment | MISSING | Pas d'onglet alertes |
| Onglet interventions du batiment | MISSING | Pas d'onglet interventions |
| Plan/etage (zone) | MISSING | Pas de visualisation spatiale |

### Ecran 4: Detail Capteur (Device)

| Exigence Spec | Statut | Detail |
|---------------|--------|--------|
| Charts temperature + puissance/THD | OK | Chart.js implemente |
| Selecteur plage temporelle | OK | 1h/6h/24h/7j |
| Anomalies recentes | MISSING | Pas de section anomalies |
| Heartbeat / derniere communication | MISSING | Pas affiche |
| Statut capteur (badge) | PARTIAL | Pas de badge explicite online/offline |

### Ecran 5: Alertes

| Exigence Spec | Statut | Detail |
|---------------|--------|--------|
| Liste paginee avec filtres | OK | Severite + statut |
| Filtre par periode (date range) | MISSING | Pas de filtre date |
| Statistiques (cards) | OK | 5 cartes stat |
| Detail alerte + timeline | OK | Implemente |
| Actions (acknowledge, resolve, false-positive) | OK | Implemente |
| Commentaires | OK | Implemente |
| Action "Creer intervention" depuis alerte | MISSING | Pas de bouton |

### Ecran 6: Interventions

| Exigence Spec | Statut | Detail |
|---------------|--------|--------|
| Kanban 5 colonnes | OK | Implemente |
| Statistiques | OK | Cards presentes |
| Date prevue sur cartes kanban | MISSING | Non affichee |
| Nom electricien (resolu) | MISSING | userId brut, pas de nom |
| Detail intervention | OK | Lifecycle complet |
| Formulaire diagnostic | OK | Implemente |
| Formulaire completion | OK | Avec result select |

### Ecran 7: Rapports

| Exigence Spec | Statut | Detail |
|---------------|--------|--------|
| Liste rapports | OK | Avec filtre type |
| Download PDF securise | OK | Token single-use |
| Formulaire generation on-demand | MISSING | Pas de formulaire (type, periode, batiment) |
| Indicateur de progression | MISSING | Pas de feedback generation en cours |

### Ecran 8: Notifications

| Exigence Spec | Statut | Detail |
|---------------|--------|--------|
| Historique notifications | OK | Liste avec statuts |
| Statistiques (cards) | OK | sent/pending/retrying/failed |
| Ecran preferences (canaux, consentement) | MISSING | Pas d'ecran preferences |
| Masquage destinataire (RGPD) | MISSING | Pas de masquage dans UI |

### Ecran 9: Settings / Administration

| Exigence Spec | Statut | Detail |
|---------------|--------|--------|
| Gestion utilisateurs | PARTIAL | Table users lecture seule |
| Journal d'audit | OK | Implemente |
| Preferences tenant (seuils, contacts) | MISSING | Ecran settings absent |
| Contacts d'urgence | MISSING | Non implemente |
| Seuils alertes personnalises | MISSING | Non implemente |

### Architecture Frontend

| Exigence Spec | Statut | Detail |
|---------------|--------|--------|
| Smart/Presentational separation | MISSING | Tout en composants monolithiques |
| Guards fonctionnels | OK | authGuard + roleGuard |
| Interceptors (JWT, tenant) | OK | Via ApiService/AuthService |
| Signals pour state | OK | Tous composants utilisent signal() |
| Lazy loading routes | OK | loadComponent partout |
| Angular Material | OK | Utilise dans tous composants |
| Responsive | PARTIAL | max-width fixe, pas de breakpoints |
| Accessibilite (a11y) | MISSING | Pas de aria-labels, pas de focus management |

---

## 2. Design Fonctionnel (12 Livrables)

### Livrable 1: Specification Fonctionnelle par Ecran

#### E1 — Dashboard Global
- **Objectif**: Vue synthetique de l'etat du parc en un coup d'oeil
- **Utilisateurs**: PLATFORM_ADMIN, TENANT_ADMIN, PROPERTY_MANAGER
- **Donnees affichees**:
  - Nombre total de batiments (GET /buildings?tenantId → count)
  - Score de risque moyen (GET /risk-scoring/buildings/summary)
  - Alertes critiques actives (GET /alerting/alerts/statistics)
  - Capteurs hors-ligne (GET /devices?tenantId&status=OFFLINE → count)
  - Interventions en retard (GET /maintenance/interventions/statistics → overdue)
  - Graphique evolution risque 30 jours (GET /risk-scoring/tenant/history?days=30)
- **Interactions**:
  - Clic card → navigation vers ecran correspondant
  - Auto-refresh 60s (configurable)
- **Regles UX**:
  - Cards: fond blanc, ombre legere, couleur accent selon criticite
  - Chart: Line chart (Chart.js), axe Y 0-100, zones colorees (vert <30, orange 30-70, rouge >70)
  - Skeleton loader pendant chargement

#### E2 — Liste Batiments
- **Objectif**: Inventaire du parc immobilier avec indicateurs risque
- **Utilisateurs**: PLATFORM_ADMIN, TENANT_ADMIN, PROPERTY_MANAGER
- **Donnees affichees par batiment**:
  - Nom, adresse
  - Nombre de capteurs actifs / total
  - Score de risque agrege
  - Statut derive: OK (score <30), WATCH (30-50), AT_RISK (50-70), CRITICAL (>70)
  - Derniere alerte
- **Interactions**:
  - Recherche par nom/adresse (client-side filter)
  - Tri par score/nom/date
  - Clic ligne → /buildings/:id
- **Regles UX**:
  - Card grid (responsive: 1 col mobile, 2 tablet, 3 desktop)
  - Badge statut colore (vert/jaune/orange/rouge)
  - Animation subtle au hover

#### E3 — Detail Batiment
- **Objectif**: Vue 360 d'un batiment avec capteurs, risque, alertes, interventions
- **Utilisateurs**: PLATFORM_ADMIN, TENANT_ADMIN, PROPERTY_MANAGER, ELECTRICIAN
- **Onglets**:
  1. **Capteurs** (existant): tableau avec polling 30s, statut online/offline
  2. **Risque** (nouveau): chart evolution score 7j/30j/90j, facteurs contributifs
  3. **Alertes** (nouveau): liste filtree par buildingId (reutilise alert-list filtree)
  4. **Interventions** (nouveau): liste filtree par buildingId
- **Donnees**:
  - Header: nom, adresse, score actuel, statut, nombre capteurs
  - Onglet Risque: GET /risk-scoring/buildings/:id/history
  - Onglet Alertes: GET /alerting/alerts?buildingId=:id
  - Onglet Interventions: GET /maintenance/interventions?buildingId=:id
- **Regles UX**:
  - Tabs Material avec lazy loading contenu
  - Score en gros, couleur dynamique
  - Responsive: tabs deviennent accordion sur mobile

#### E4 — Detail Capteur
- **Objectif**: Visualisation telemetrie + anomalies + etat sante capteur
- **Utilisateurs**: tous (lecture), ELECTRICIAN (actions)
- **Sections**:
  1. **En-tete** (enrichir): nom, type, statut (badge), derniere communication, firmware
  2. **Telemetrie** (existant): charts temperature + puissance/THD, selecteur plage
  3. **Anomalies** (nouveau): liste des 10 dernieres anomalies detectees
  4. **Heartbeat** (nouveau): timeline derniers heartbeats, frequence
- **Donnees nouvelles**:
  - GET /signal-analysis/anomalies/:deviceId (top 10 recentes)
  - GET /devices/:id (inclure lastHeartbeatAt, firmwareVersion)
- **Regles UX**:
  - Badge statut: vert (ACTIVE), gris (OFFLINE), rouge (ERROR)
  - Anomalies: icone type + timestamp + description courte
  - Heartbeat: dot timeline, gap = alerte

#### E5 — Alertes (enrichissement)
- **Ajouts**:
  - Filtre date range (date picker Material, from/to)
  - Bouton "Creer intervention" sur detail alerte (POST /maintenance/interventions, pre-remplit alertId + deviceId)
- **Backend requis**:
  - GET /alerting/alerts accepte deja `from`/`to` params (confirme dans AuditLogController pattern)
  - Alerting-service: verifier/ajouter params from/to sur GET /alerts

#### E6 — Interventions (enrichissement)
- **Ajouts**:
  - Date prevue affichee sur cartes kanban (`scheduledDate`)
  - Nom electricien resolu (appel GET /users/:id ou enrichissement cote backend)
- **Backend requis**:
  - Maintenance-service: enrichir InterventionResponse avec `assigneeName` (JOIN ou lookup)
  - Ou: Frontend resout via cache local GET /users

#### E7 — Rapports (enrichissement)
- **Ajouts**:
  - Formulaire generation: type (select), periode (date range), batiment (select)
  - Barre progression / statut generation (polling ou SSE)
- **Composants**:
  - `report-generation-form` (dialog ou inline)
  - Feedback: spinner + message "Generation en cours..." + refresh auto a la completion
- **Backend requis**:
  - POST /reports/generate (existant, verifier params: type, from, to, buildingId)

#### E8 — Notifications (enrichissement)
- **Ajouts**:
  - Sous-onglet "Preferences" (canaux actifs, consentement email/sms/push)
  - Masquage email/telephone dans l'historique (RGPD)
- **Backend requis**:
  - Notification-service: GET/PUT /notifications/preferences/:userId
  - Schema: `notification_preferences` (userId, consentEmail, consentSms, consentPush, channels JSON)
  - Migration Flyway V002

#### E9 — Settings
- **Ecran entierement nouveau**, accessible TENANT_ADMIN + PLATFORM_ADMIN
- **Onglets**:
  1. **Utilisateurs** (existant dans admin, deplacer/dupliquer)
  2. **Seuils & Alertes**: configurer seuils par type alerte (temperature max, THD max, score critique)
  3. **Contacts d'urgence**: CRUD liste contacts (nom, telephone, email, role)
  4. **Tenant Info**: nom, adresse, logo, plan (lecture pour admin)
- **Backend requis**:
  - Identity-service: GET/PUT /tenants/:id/settings (seuils JSON, contacts JSON)
  - Ou nouveau endpoint dedicate: GET/PUT /api/v1/tenant-config
  - Migration Flyway: `tenant_settings` table

---

### Livrable 2: Composants Angular

#### Architecture Smart/Presentational

```
features/
  dashboard/
    containers/
      dashboard-page.component.ts        (smart: orchestre appels API)
    components/
      stat-card.component.ts             (presentational: input count/label/color)
      risk-evolution-chart.component.ts  (presentational: input data[])
      quick-link-card.component.ts       (presentational: input icon/label/route)

  buildings/
    containers/
      building-list-page.component.ts    (smart: charge buildings, gere filtre)
      building-detail-page.component.ts  (smart: charge building + tabs data)
    components/
      building-card.component.ts         (presentational: input building)
      building-status-badge.component.ts (presentational: input score → statut)
      building-risk-tab.component.ts     (presentational: input history data)
      building-alerts-tab.component.ts   (presentational: input alerts[])
      building-interventions-tab.component.ts

  devices/
    containers/
      device-list-page.component.ts
      device-detail-page.component.ts
    components/
      device-status-badge.component.ts
      telemetry-chart.component.ts       (presentational: existant, extraire)
      anomaly-list.component.ts          (presentational: input anomalies[])
      heartbeat-timeline.component.ts    (presentational: input heartbeats[])

  alerts/
    containers/
      alert-list-page.component.ts
      alert-detail-page.component.ts
    components/
      alert-stat-cards.component.ts
      alert-table.component.ts
      alert-filters.component.ts         (presentational: outputs filterChange)
      alert-comment-thread.component.ts
      create-intervention-dialog.component.ts

  interventions/
    containers/
      intervention-list-page.component.ts
      intervention-detail-page.component.ts
    components/
      kanban-board.component.ts          (presentational: input columns[])
      kanban-card.component.ts           (presentational: input intervention)
      intervention-stat-cards.component.ts
      diagnostic-form.component.ts
      completion-form.component.ts

  reports/
    containers/
      report-list-page.component.ts
    components/
      report-table.component.ts
      report-generation-dialog.component.ts  (nouveau)
      report-progress-indicator.component.ts (nouveau)

  notifications/
    containers/
      notification-list-page.component.ts
      notification-preferences-page.component.ts  (nouveau)
    components/
      notification-table.component.ts
      notification-stat-cards.component.ts
      preference-channel-toggle.component.ts      (nouveau)

  settings/                              (nouveau module)
    containers/
      settings-page.component.ts
    components/
      threshold-config.component.ts
      emergency-contacts.component.ts
      tenant-info.component.ts

shared/
  components/
    date-range-picker.component.ts       (reutilisable alertes + rapports)
    skeleton-loader.component.ts
    empty-state.component.ts
    confirm-dialog.component.ts
    status-badge.component.ts
```

#### Nombre total: ~45 composants (dont ~20 existants a refactorer, ~25 nouveaux)

---

### Livrable 3: Routes Angular

```typescript
export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/containers/dashboard-page.component')
      .then(m => m.DashboardPageComponent),
  },
  {
    path: 'buildings',
    canActivate: [authGuard],
    loadComponent: () => import('./features/buildings/containers/building-list-page.component')
      .then(m => m.BuildingListPageComponent),
  },
  {
    path: 'buildings/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/buildings/containers/building-detail-page.component')
      .then(m => m.BuildingDetailPageComponent),
  },
  {
    path: 'devices',
    canActivate: [authGuard],
    loadComponent: () => import('./features/devices/containers/device-list-page.component')
      .then(m => m.DeviceListPageComponent),
  },
  {
    path: 'devices/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/devices/containers/device-detail-page.component')
      .then(m => m.DeviceDetailPageComponent),
  },
  {
    path: 'alerts',
    canActivate: [authGuard],
    loadComponent: () => import('./features/alerts/containers/alert-list-page.component')
      .then(m => m.AlertListPageComponent),
  },
  {
    path: 'alerts/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/alerts/containers/alert-detail-page.component')
      .then(m => m.AlertDetailPageComponent),
  },
  {
    path: 'interventions',
    canActivate: [authGuard],
    loadComponent: () => import('./features/interventions/containers/intervention-list-page.component')
      .then(m => m.InterventionListPageComponent),
  },
  {
    path: 'interventions/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/interventions/containers/intervention-detail-page.component')
      .then(m => m.InterventionDetailPageComponent),
  },
  {
    path: 'reports',
    canActivate: [authGuard],
    loadComponent: () => import('./features/reports/containers/report-list-page.component')
      .then(m => m.ReportListPageComponent),
  },
  {
    path: 'notifications',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./features/notifications/containers/notification-list-page.component')
          .then(m => m.NotificationListPageComponent),
      },
      {
        path: 'preferences',
        loadComponent: () => import('./features/notifications/containers/notification-preferences-page.component')
          .then(m => m.NotificationPreferencesPageComponent),
      },
    ],
  },
  {
    path: 'settings',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['PLATFORM_ADMIN', 'TENANT_ADMIN'] },
    loadComponent: () => import('./features/settings/containers/settings-page.component')
      .then(m => m.SettingsPageComponent),
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['PLATFORM_ADMIN', 'TENANT_ADMIN'] },
    loadComponent: () => import('./features/admin/admin.component')
      .then(m => m.AdminComponent),
  },
  { path: '**', redirectTo: 'dashboard' },
];
```

---

### Livrable 4: Services Angular

#### Nouveaux services / methodes a ajouter

```typescript
// building.service.ts (nouveau)
interface BuildingService {
  getBuildings(tenantId: string): Observable<BuildingResponse[]>;
  getBuildingById(id: string): Observable<BuildingDetailResponse>;
  getBuildingRiskHistory(id: string, days: number): Observable<RiskHistoryPoint[]>;
}

// dashboard.service.ts (nouveau)
interface DashboardService {
  getDashboardSummary(): Observable<DashboardSummary>;
  getRiskEvolution(days: number): Observable<RiskHistoryPoint[]>;
}

// device.service.ts (enrichir api.service existant)
// Ajouter:
//   getDeviceAnomalies(deviceId: string): Observable<AnomalyResponse[]>

// alert.service.ts (enrichir api.service existant)
// Ajouter:
//   getAlerts(params: AlertFilterParams): Observable<AlertResponse[]>  // avec from/to
//   createInterventionFromAlert(alertId: string, deviceId: string): Observable<InterventionResponse>

// intervention.service.ts (enrichir api.service existant)
// Ajouter:
//   getInterventionsByBuilding(buildingId: string): Observable<InterventionResponse[]>

// notification-preferences.service.ts (nouveau)
interface NotificationPreferencesService {
  getPreferences(userId: string): Observable<NotificationPreferences>;
  updatePreferences(userId: string, prefs: NotificationPreferences): Observable<void>;
}

// tenant-settings.service.ts (nouveau)
interface TenantSettingsService {
  getSettings(tenantId: string): Observable<TenantSettings>;
  updateThresholds(tenantId: string, thresholds: AlertThresholds): Observable<void>;
  getEmergencyContacts(tenantId: string): Observable<EmergencyContact[]>;
  upsertEmergencyContact(tenantId: string, contact: EmergencyContact): Observable<EmergencyContact>;
  deleteEmergencyContact(tenantId: string, contactId: string): Observable<void>;
}
```

---

### Livrable 5: Modeles TypeScript

```typescript
// --- Dashboard ---
interface DashboardSummary {
  totalBuildings: number;
  avgRiskScore: number;
  criticalAlerts: number;
  offlineDevices: number;
  overdueInterventions: number;
  totalDevices: number;
  activeDevices: number;
}

interface RiskHistoryPoint {
  date: string; // ISO
  score: number;
}

// --- Buildings ---
interface BuildingResponse {
  id: string;
  name: string;
  address: string;
  totalDevices: number;
  activeDevices: number;
  riskScore: number;
  status: 'OK' | 'WATCH' | 'AT_RISK' | 'CRITICAL';
  lastAlertAt: string | null;
}

interface BuildingDetailResponse extends BuildingResponse {
  floors: number;
  panels: number;
  circuits: number;
  createdAt: string;
}

// --- Devices (enrichissement) ---
interface DeviceResponse {
  id: string;
  name: string;
  serialNumber: string;
  status: 'REGISTERED' | 'PROVISIONED' | 'ACTIVE' | 'OFFLINE' | 'REVOKED';
  lastHeartbeatAt: string | null;
  firmwareVersion: string | null;
  buildingId: string;
  buildingName: string;
  panelId: string | null;
  circuitId: string | null;
}

interface AnomalyResponse {
  id: string;
  type: string; // MICRO_ARC, TEMPERATURE_SPIKE, THD_DRIFT, etc.
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  description: string;
  detectedAt: string;
  score: number;
}

interface HeartbeatResponse {
  timestamp: string;
  rssi: number | null;
  batteryLevel: number | null;
}

// --- Alerts (enrichissement) ---
interface AlertFilterParams {
  severity?: string;
  status?: string;
  buildingId?: string;
  from?: string; // ISO date
  to?: string;   // ISO date
  page?: number;
  size?: number;
}

// --- Interventions (enrichissement) ---
interface InterventionResponse {
  // ... champs existants ...
  scheduledDate: string | null;    // nouveau
  assigneeName: string | null;     // nouveau (resolu)
}

// --- Notifications Preferences (nouveau) ---
interface NotificationPreferences {
  userId: string;
  consentEmail: boolean;
  consentSms: boolean;
  consentPush: boolean;
  channels: NotificationChannelConfig[];
}

interface NotificationChannelConfig {
  channel: 'EMAIL' | 'SMS' | 'PUSH' | 'WEBHOOK' | 'DASHBOARD';
  enabled: boolean;
  minSeverity: 'INFO' | 'WARNING' | 'CRITICAL';
}

// --- Settings (nouveau) ---
interface TenantSettings {
  tenantId: string;
  thresholds: AlertThresholds;
  emergencyContacts: EmergencyContact[];
}

interface AlertThresholds {
  temperatureMax: number;     // default 85
  thdMax: number;             // default 40
  riskScoreCritical: number;  // default 70
  riskScoreWarning: number;   // default 50
  microArcCountMax: number;   // default 3
}

interface EmergencyContact {
  id?: string;
  name: string;
  phone: string;
  email: string;
  role: string; // 'ELECTRICIAN' | 'PROPERTY_MANAGER' | 'FIRE_DEPT'
  priority: number;
}
```

---

### Livrable 6: Guards et Interceptors

#### Existants (OK)
- `authGuard`: verifie token Keycloak valide
- `roleGuard`: verifie roles depuis route data

#### A ajouter

```typescript
// unsaved-changes.guard.ts
// Protege les formulaires (settings, diagnostic) contre navigation accidentelle
export const unsavedChangesGuard: CanDeactivateFn<HasUnsavedChanges> = (component) => {
  return component.hasUnsavedChanges() ? confirm('Modifications non sauvegardees. Quitter ?') : true;
};

// tenant.interceptor.ts (enrichir existant)
// Deja gere via AuthService, mais s'assurer que X-Tenant-Id est toujours envoye

// error.interceptor.ts (nouveau)
// Intercepte 401 → redirect login
// Intercepte 403 → snackbar "Acces refuse"
// Intercepte 429 → snackbar "Trop de requetes, reessayez"
// Intercepte 500 → snackbar generique avec correlationId
```

---

### Livrable 7: Gestion d'Etat (Signals)

#### Pattern adopte: Service + Signal Store

Pas de NgRx. Chaque feature a un service qui expose des signals:

```typescript
// Exemple: building-state.service.ts
@Injectable({ providedIn: 'root' })
export class BuildingStateService {
  private _buildings = signal<BuildingResponse[]>([]);
  private _loading = signal(false);
  private _error = signal<string | null>(null);
  private _selectedBuilding = signal<BuildingDetailResponse | null>(null);

  readonly buildings = this._buildings.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly selectedBuilding = this._selectedBuilding.asReadonly();

  // Computed signals
  readonly buildingCount = computed(() => this._buildings().length);
  readonly criticalBuildings = computed(() =>
    this._buildings().filter(b => b.status === 'CRITICAL')
  );
}
```

#### Services d'etat a creer:
1. `DashboardStateService` — summary + risk history
2. `BuildingStateService` — list + detail + tabs data
3. `DeviceStateService` — enrichir existant (anomalies, heartbeats)
4. `AlertStateService` — enrichir existant (date filters)
5. `InterventionStateService` — enrichir existant
6. `NotificationPrefsStateService` — preferences utilisateur
7. `SettingsStateService` — seuils + contacts

#### Regles:
- Smart components injectent le state service + appellent les methodes load/action
- Presentational components recoivent les donnees via `input()` signals
- Aucun `HttpClient` direct dans les composants (seulement via services)

---

### Livrable 8: Endpoints Backend (Nouveaux / Modifies)

#### Device Service (pyrosense-device-service)

| Methode | Endpoint | Description | Statut |
|---------|----------|-------------|--------|
| GET | /api/v1/devices?tenantId&status | Liste avec filtre statut | ENRICHIR (ajouter status filter) |
| GET | /api/v1/devices/:id | Detail enrichi (lastHeartbeatAt, firmware) | ENRICHIR |
| GET | /api/v1/devices/statistics?tenantId | Compteurs par statut | NOUVEAU |

#### Risk Scoring Service (pyrosense-risk-scoring-service)

| Methode | Endpoint | Description | Statut |
|---------|----------|-------------|--------|
| GET | /api/v1/risk-scoring/buildings/:id/history?days=30 | Historique score | NOUVEAU |
| GET | /api/v1/risk-scoring/tenant/summary | Score moyen tenant | NOUVEAU |
| GET | /api/v1/risk-scoring/tenant/history?days=30 | Evolution globale 30j | NOUVEAU |

#### Alerting Service (pyrosense-alerting-service)

| Methode | Endpoint | Description | Statut |
|---------|----------|-------------|--------|
| GET | /api/v1/alerts?from&to&buildingId | Ajouter filtres date + building | ENRICHIR |
| GET | /api/v1/alerts/overdue | Alertes non-traitees hors SLA | NOUVEAU |

#### Maintenance Service (pyrosense-maintenance-service)

| Methode | Endpoint | Description | Statut |
|---------|----------|-------------|--------|
| GET | /api/v1/interventions?buildingId | Filtre par batiment | ENRICHIR |
| GET | /api/v1/interventions/overdue | Interventions en retard | NOUVEAU |

#### Notification Service (pyrosense-notification-service)

| Methode | Endpoint | Description | Statut |
|---------|----------|-------------|--------|
| GET | /api/v1/notifications/preferences/:userId | Preferences utilisateur | NOUVEAU |
| PUT | /api/v1/notifications/preferences/:userId | MAJ preferences | NOUVEAU |

#### Identity Service (pyrosense-identity-service)

| Methode | Endpoint | Description | Statut |
|---------|----------|-------------|--------|
| GET | /api/v1/tenants/:id/settings | Configuration tenant | NOUVEAU |
| PUT | /api/v1/tenants/:id/settings | MAJ configuration | NOUVEAU |
| GET | /api/v1/tenants/:id/emergency-contacts | Contacts urgence | NOUVEAU |
| POST | /api/v1/tenants/:id/emergency-contacts | Ajout contact | NOUVEAU |
| PUT | /api/v1/tenants/:id/emergency-contacts/:cid | MAJ contact | NOUVEAU |
| DELETE | /api/v1/tenants/:id/emergency-contacts/:cid | Suppression contact | NOUVEAU |

#### Building Data (a definir — option 1: enrichir device-service, option 2: nouveau endpoint dans gateway qui agrege)

| Methode | Endpoint | Description | Statut |
|---------|----------|-------------|--------|
| GET | /api/v1/buildings?tenantId | Liste batiments + score agrege | NOUVEAU |
| GET | /api/v1/buildings/:id | Detail batiment | NOUVEAU |

**Decision architecturale**: Les batiments sont geres par device-service (BuildingId existe deja comme value object). Ajouter un `BuildingController` dans device-service qui agrege devices + appelle risk-scoring en interne (ou via gateway composition).

---

### Livrable 9: DTOs Backend (Nouveaux)

```java
// Device Service
record DeviceStatisticsResponse(int total, int active, int offline, int provisioned, int revoked) {}
record BuildingListResponse(String id, String name, String address, int totalDevices,
                            int activeDevices, double riskScore, String status, Instant lastAlertAt) {}
record BuildingDetailResponse(String id, String name, String address, int totalDevices,
                              int activeDevices, int floors, int panels, int circuits,
                              double riskScore, String status, Instant createdAt) {}

// Risk Scoring Service
record RiskHistoryPointResponse(Instant date, double score) {}
record TenantRiskSummaryResponse(double avgScore, String trend, int buildingsAtRisk,
                                  List<RiskHistoryPointResponse> history) {}

// Alerting Service
record AlertOverdueResponse(int count, List<AlertResponse> alerts) {}

// Maintenance Service
record InterventionOverdueResponse(int count, List<InterventionResponse> interventions) {}
// Enrichir InterventionResponse existant:
//   + String scheduledDate
//   + String assigneeName

// Notification Service
record NotificationPreferencesResponse(String userId, boolean consentEmail,
    boolean consentSms, boolean consentPush, List<ChannelConfigResponse> channels) {}
record ChannelConfigResponse(String channel, boolean enabled, String minSeverity) {}
record UpdatePreferencesRequest(@NotNull Boolean consentEmail, @NotNull Boolean consentSms,
    @NotNull Boolean consentPush, List<ChannelConfigRequest> channels) {}

// Identity Service
record TenantSettingsResponse(String tenantId, AlertThresholdsDto thresholds) {}
record AlertThresholdsDto(double temperatureMax, double thdMax,
    double riskScoreCritical, double riskScoreWarning, int microArcCountMax) {}
record EmergencyContactResponse(String id, String name, String phone, String email,
    String role, int priority) {}
record CreateEmergencyContactRequest(@NotBlank String name, @NotBlank String phone,
    @Email String email, @NotBlank String role, int priority) {}
```

---

### Livrable 10: Regles UX

#### Palette Couleurs

| Usage | Couleur | Hex |
|-------|---------|-----|
| Primaire (sidebar, toolbar) | Indigo | #1a237e |
| Accent (actions) | Deep Orange | #ff5722 |
| Succes | Green | #2e7d32 |
| Warning | Orange | #f57c00 |
| Danger | Red | #d32f2f |
| Info | Blue | #1976d2 |
| Neutre | Grey | #616161 |
| Background | Light Grey | #fafafa |

#### Regles de Chargement
- **< 300ms**: pas de feedback (eviter flash)
- **300ms - 2s**: skeleton loader (formes grises animees)
- **> 2s**: spinner + message "Chargement en cours..."
- **Erreur**: snackbar rouge 5s + bouton "Reessayer"

#### Responsive Breakpoints
- **Mobile** (<600px): 1 colonne, tabs → accordion, sidebar masquee
- **Tablet** (600-960px): 2 colonnes, sidebar collapsible
- **Desktop** (>960px): layout complet 3 colonnes max

#### Accessibilite (a11y)
- Tout element interactif: `aria-label` ou `aria-labelledby`
- Focus visible (outline) sur tous boutons/liens
- Contraste minimum 4.5:1 (WCAG AA)
- Navigation clavier complete (tab order logique)
- `role="alert"` sur les snackbars
- `aria-live="polite"` sur les zones de contenu dynamique

#### Animations
- Transitions: 200ms ease-in-out (Material spec)
- Skeleton: pulse animation 1.5s infini
- Cards: elevation shadow on hover (2dp → 8dp)
- Pas d'animation si `prefers-reduced-motion: reduce`

#### Patterns Recurrants
- **Empty state**: icone 48px + message + bouton action contextuel
- **Error state**: icone erreur + message + bouton retry
- **Loading state**: skeleton matching la structure finale
- **Confirmation**: dialog Material pour actions destructives
- **Toast/Snackbar**: 5s auto-dismiss, action "Fermer"

---

### Livrable 11: Cas d'Erreur

| Scenario | Code HTTP | Comportement Frontend |
|----------|-----------|----------------------|
| Token expire | 401 | Redirect silencieux vers Keycloak (silent SSO refresh d'abord) |
| Acces refuse (mauvais role) | 403 | Snackbar "Vous n'avez pas les droits" + redirect dashboard |
| Ressource inexistante | 404 | Page "Ressource introuvable" avec lien retour |
| Validation echouee | 400 | Highlight champs en erreur + messages inline |
| Rate limited | 429 | Snackbar "Trop de requetes" + retry automatique apres delai |
| Service indisponible | 503 | Banner "Service temporairement indisponible" + retry 10s |
| Erreur serveur | 500 | Snackbar generique + correlationId pour support |
| Timeout reseau | 0 | Snackbar "Probleme de connexion" + bouton retry |
| Donnees vides (liste vide) | 200 [] | Empty state contextuel (pas "erreur") |
| Conflit (edition concurrente) | 409 | Dialog "Donnees modifiees, recharger ?" |

#### Strategie de Retry
- GET automatique: 1 retry apres 2s en cas de 503/timeout
- POST/PUT/DELETE: jamais de retry automatique (risque doublon)
- Afficher bouton "Reessayer" pour l'utilisateur

#### Offline Handling
- Detecter `navigator.onLine` changes
- Banner fixe "Mode hors ligne — donnees potentiellement obsoletes"
- Desactiver boutons d'action (POST/PUT/DELETE)
- Reprendre auto-refresh quand online

---

### Livrable 12: Tests Frontend

#### Strategie de Test

| Couche | Outil | Couverture Cible |
|--------|-------|-----------------|
| Composants presentational | Jest + Angular Testing Library | 90% |
| Composants smart (containers) | Jest + HttpClientTestingModule | 80% |
| Services | Jest + HttpClientTestingModule | 90% |
| Guards/Interceptors | Jest | 100% |
| E2E (golden paths) | Cypress | 5 scenarios critiques |

#### Tests Unitaires (Jest)

```
Composants presentational (input/output):
- stat-card: renders value + label + color correctly
- building-card: displays status badge based on score thresholds
- kanban-card: shows scheduled date when present
- anomaly-list: renders empty state when no anomalies
- date-range-picker: emits correct ISO dates on change

Composants smart (containers):
- dashboard-page: calls DashboardStateService.load on init
- dashboard-page: displays loading skeleton when loading=true
- dashboard-page: navigates to alerts on card click
- building-list-page: filters buildings by search term
- alert-detail-page: shows "create intervention" button when status OPEN

Services:
- building.service: constructs correct URL with tenantId
- dashboard.service: maps API response to DashboardSummary
- notification-preferences.service: sends PUT with correct body
- tenant-settings.service: handles 404 (new tenant) gracefully

Guards:
- unsavedChangesGuard: returns false when component has changes
- unsavedChangesGuard: returns true when no changes
```

#### Tests E2E (Cypress) — 5 Golden Paths

1. **Login → Dashboard**: Auth Keycloak → dashboard charge → 6 cards visibles → chart risque
2. **Dashboard → Building → Device**: Clic batiment → onglet capteurs → clic capteur → charts
3. **Alertes → Acknowledge → Create Intervention**: Liste alertes → detail → acknowledge → creer intervention
4. **Intervention Lifecycle**: Liste → detail → assigner → demarrer → diagnostic → completer
5. **Report Generation → Download**: Liste rapports → generer → attendre → telecharger PDF

---

## 3. Plan d'Implementation (Sprints)

### Sprint A — Refactoring Architecture + Dashboard (5j)
1. Restructurer dossiers features/ en containers/ + components/
2. Creer services d'etat (signals)
3. Implementer DashboardPageComponent avec vraies donnees
4. Ajouter endpoints backend: device statistics, risk tenant summary/history
5. Supprimer tous mocks/fallbacks

### Sprint B — Buildings + Devices (5j)
1. Creer BuildingController dans device-service
2. Implementer building-list avec vraie API + filtres + status
3. Enrichir building-detail (onglets risque, alertes, interventions)
4. Enrichir device-detail (anomalies, heartbeats)
5. Endpoint risk-scoring history par building

### Sprint C — Alertes + Interventions Enrichis (4j)
1. Ajouter filtres date sur alertes (frontend + backend from/to)
2. Bouton "Creer intervention" depuis alerte
3. Date prevue + nom electricien sur kanban
4. Endpoint interventions/overdue

### Sprint D — Reports + Notifications + Settings (5j)
1. Formulaire generation rapport (dialog)
2. Notification preferences (frontend + backend CRUD)
3. Ecran Settings complet (seuils, contacts, tenant info)
4. Backend: tenant-settings + emergency-contacts endpoints

### Sprint E — Polish + Tests + A11y (4j)
1. Responsive breakpoints sur tous ecrans
2. Accessibilite (aria-labels, focus, contraste)
3. Error interceptor global
4. Tests unitaires Jest (objectif 80%+ coverage)
5. Tests E2E Cypress (5 golden paths)

---

## 4. Resume des Lacunes Critiques

| Priorite | Gap | Impact Utilisateur |
|----------|-----|-------------------|
| P0 | Buildings list = mock hardcode | Ecran inutilisable en production |
| P0 | Dashboard incomplet (3 metrics manquants) | Vision partielle du parc |
| P0 | Pas de smart/presentational separation | Maintenabilite, testabilite |
| P1 | Device: pas d'anomalies | Electricien ne voit pas les problemes detectes |
| P1 | Alerts: pas de filtre date | Impossible d'analyser l'historique |
| P1 | Reports: pas de generation on-demand | Workflow manuel obligatoire |
| P1 | Settings: ecran absent | Admin ne peut pas configurer |
| P2 | Responsive insuffisant | Mobile inutilisable |
| P2 | Accessibilite absente | Non-conforme RGAA/WCAG |
| P2 | Error handling basique | UX degradee en cas d'erreur |
