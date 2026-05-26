# PyroSense AI — Cadrage Complet Dashboard Gestionnaire

## 1. Comparaison : Etat Actuel vs Specification Cible

### Ecran 1 : Dashboard Global

| Exigence | Statut | Detail |
|----------|--------|--------|
| Nombre de batiments surveilles | FAIT | `DashboardStateService.summary().totalBuildings` |
| Nombre de capteurs actifs | FAIT | `summary().activeDevices` |
| Nombre de capteurs offline | FAIT | `summary().offlineDevices` |
| Score de risque moyen du parc | FAIT | `summary().avgRiskScore` + RiskGauge |
| Alertes critiques ouvertes | FAIT | `summary().criticalAlerts` |
| Alertes warning ouvertes | FAIT | `summary().warningAlerts` |
| Interventions en retard | FAIT | `summary().overdueInterventions` |
| Evolution du risque sur 30 jours | FAIT | Chart.js line chart via `getTenantRiskHistory(30)` |

**Verdict : 100% couvert.**

---

### Ecran 2 : Vue Batiments

| Exigence | Statut | Detail |
|----------|--------|--------|
| Liste des batiments | FAIT | `building-list.component.ts` grid cards |
| Adresse | FAIT | Affichee sur chaque card |
| Nombre de capteurs | FAIT | `totalDevices` / `activeDevices` |
| Score moyen | FAIT | `RiskGaugeComponent` par batiment |
| Alerte la plus critique | PARTIEL | Affiche `lastAlertAt` (date) mais PAS la severite de l'alerte la plus critique |
| Statut global (OK/WATCH/AT_RISK/CRITICAL) | FAIT | Badge colore base sur riskScore |

**Ecart : afficher la severite de l'alerte la plus critique par batiment (pas juste la date).**

---

### Ecran 3 : Detail Batiment

| Exigence | Statut | Detail |
|----------|--------|--------|
| Capteurs installes | FAIT | Onglet Capteurs avec tableau device |
| Tableaux electriques | MANQUANT | Pas de groupement par `panelId` |
| Historique de risque | FAIT | Onglet Risque avec chart 30j |
| Alertes liees au batiment | FAIT | Onglet Alertes filtre par buildingId |
| Interventions liees au batiment | FAIT | Onglet Interventions filtre par buildingId |

**Ecart : ajouter un onglet/section "Tableaux electriques" qui groupe les devices par `panelId`.**

---

### Ecran 4 : Vue Capteur (Device)

| Exigence | Statut | Detail |
|----------|--------|--------|
| Liste capteurs | FAIT | `device-list.component.ts` (Sprint G) |
| Statut capteur | FAIT | Badge statut dans detail |
| Dernier heartbeat | FAIT | `lastSeenAt` affiche |
| Firmware version | FAIT | Card firmware |
| Score de risque actuel | FAIT | RiskGauge dans detail |
| Anomalies recentes | FAIT | Tableau anomalies (top 10) |
| Historique mesures agregees | FAIT | Chart.js temperature + puissance/THD avec time range |

**Verdict : 100% couvert.**

---

### Ecran 5 : Vue Alertes

| Exigence | Statut | Detail |
|----------|--------|--------|
| Filtre par severite | FAIT | MatSelect CRITICAL/WARNING/INFO |
| Filtre par statut | FAIT | MatSelect 5 statuts |
| Filtre par batiment | MANQUANT | Pas de filtre batiment sur alert-list |
| Filtre par periode | FAIT | DatePicker from/to |
| Action acknowledge | FAIT | Sur alert-detail |
| Action assign | MANQUANT | Pas de bouton assign sur alert-detail |
| Action create intervention | FAIT | Sur alert-detail |
| Action resolve | FAIT | Sur alert-detail |
| Action mark false positive | FAIT | Sur alert-detail |

**Ecarts : (1) filtre par batiment, (2) action "assigner" sur detail alerte.**

---

### Ecran 6 : Vue Interventions

| Exigence | Statut | Detail |
|----------|--------|--------|
| Kanban ou tableau | FAIT | Kanban 5 colonnes |
| Statuts PLANNED/ASSIGNED/IN_PROGRESS/COMPLETED/CANCELLED | FAIT | 6 colonnes (incl. CREATED) |
| Priorite | FAIT | Badge priorite sur cartes |
| Electricien assigne | FAIT | Affiche assignedElectricianId |
| Date prevue | FAIT | `scheduledAt` sur cartes |
| Resultat terrain | FAIT | Visible dans intervention-detail |

**Verdict : 100% couvert.**

---

### Ecran 7 : Vue Rapports

| Exigence | Statut | Detail |
|----------|--------|--------|
| Generer rapport mensuel | FAIT | Formulaire generation |
| Generer certificat de monitoring | FAIT | Type COMPLIANCE_CERTIFICATE |
| Generer rapport d'alerte critique | FAIT | Type CRITICAL_ALERT_REPORT |
| Telecharger PDF | FAIT | Token-based download |
| Historique rapports | FAIT | Tableau avec statut/date/type |

**Verdict : 100% couvert.**

---

### Ecran 8 : Vue Notifications

| Exigence | Statut | Detail |
|----------|--------|--------|
| Historique notifications | FAIT | Tableau avec pagination |
| Statut envoi | FAIT | Chips sent/pending/retrying/failed |
| Canal | FAIT | Email/SMS/Push/Webhook/Dashboard |
| Destinataire masque | FAIT | Backend masque PII (j***e@domain.com) |
| Raison d'echec si echec | FAIT | `failureReason` affiche |

**Verdict : 100% couvert.**

---

### Ecran 9 : Parametres

| Exigence | Statut | Detail |
|----------|--------|--------|
| Preferences de notification | MANQUANT | Onglet present dans notification-list mais PAS dans settings |
| Seuils d'alerte configurables | FAIT | Onglet Seuils dans settings |
| Utilisateurs du tenant | FAIT | Admin component, Users tab |
| Contacts d'urgence | FAIT | Onglet Contacts dans settings |

**Ecart : integrer les preferences de notification dans Settings (au lieu d'etre seulement dans Notifications).**

---

### Roles et Securite

| Exigence | Statut | Detail |
|----------|--------|--------|
| PLATFORM_ADMIN | FAIT | Reconnu dans layout + roleGuard |
| TENANT_ADMIN | FAIT | Reconnu dans layout + roleGuard |
| PROPERTY_MANAGER | FAIT | Backend @PreAuthorize, pas de restriction frontend specifique |
| ELECTRICIAN | FAIT | Backend @PreAuthorize |
| SUPPORT_READONLY | MANQUANT | Pas gere dans le frontend (pas de mode lecture seule) |

**Ecart : implementer le mode SUPPORT_READONLY (masquer boutons d'action, UI lecture seule).**

---

### Contraintes Transversales

| Exigence | Statut | Detail |
|----------|--------|--------|
| Angular moderne | FAIT | Angular 18, standalone, signals |
| UI SaaS B2B professionnelle | FAIT | Angular Material, dark nav |
| Responsive desktop/tablette | FAIT | BreakpointObserver, media queries |
| Accessibilite | FAIT | aria-labels, role, aria-current |
| Pas de mock definitif | FAIT | Tout pointe vers API reelle |
| Separation smart/presentational | PARTIEL | Shared components existent mais features ne sont pas split en containers/components |
| State management simple | FAIT | Signals (DashboardStateService) |

---

## 2. ECARTS A COMBLER — Specification d'Implementation

### Ecart 1 : Alerte la plus critique par batiment

**Composant** : `building-list.component.ts`
**Changement** : Afficher un badge severite a cote du `lastAlertAt`
**Backend** : Le `BuildingResponse` a deja `lastAlertAt` mais pas `criticalAlertSeverity`. Option : enrichir coté API ou deduire coté frontend via un appel complementaire.
**Recommandation** : Ajouter un champ `highestAlertSeverity: string | null` au `BuildingResponse` backend. En attendant, le frontend peut fetch les alertes critiques en batch.

### Ecart 2 : Tableaux electriques dans detail batiment

**Composant** : `building-detail.component.ts`
**Changement** : Ajouter un onglet "Tableaux electriques" qui :
- Recupere les devices du batiment
- Les groupe par `panelId`
- Affiche pour chaque panel : nombre de capteurs, statut agrege, score risque max
**Backend** : Le `DeviceResponse` contient deja `panelId`. Aucun nouveau endpoint necessaire — groupement cote client.
**Modele** : Pas de nouveau DTO, groupement local par `panelId`.

### Ecart 3 : Filtre par batiment sur alertes

**Composant** : `alert-list.component.ts`
**Changement** : Ajouter un `MatSelect` "Batiment" qui charge la liste des batiments du tenant et filtre par `buildingId`.
**Backend** : Le endpoint `GET /api/v1/alerts` accepte deja `buildingId` comme parametre (utilise dans building-detail).
**Dependance** : `ApiService.getBuildings()` pour peupler le select.

### Ecart 4 : Action "Assigner" sur detail alerte

**Composant** : `alert-detail.component.ts`
**Changement** : Ajouter un bouton "Assigner" avec un champ pour saisir l'ID/nom de l'utilisateur.
**Backend** : `POST /api/v1/alerts/{id}/assign` existe deja (body: `{ userId }`).
**Condition** : Visible seulement si statut OPEN ou ACKNOWLEDGED.

### Ecart 5 : Preferences de notification dans Settings

**Composant** : `settings.component.ts`
**Changement** : Ajouter un 4eme onglet "Notifications" dans settings qui reprend le contenu preferences du composant notifications.
**Backend** : `GET/PUT /api/v1/notifications/preferences/{userId}` existe deja (Sprint D).
**Note** : Garder aussi l'onglet Preferences dans notification-list pour acces rapide.

### Ecart 6 : Role SUPPORT_READONLY

**Composant** : Layout, tous les composants avec actions
**Changement** :
- `AuthService` : ajouter un computed signal `isReadOnly` qui retourne true si le seul role est SUPPORT_READONLY
- Layout : masquer "Settings" et "Admin" pour SUPPORT_READONLY
- Alert-detail, intervention-detail, settings : masquer les boutons d'action si `isReadOnly`
- Utiliser une directive ou un signal partage pour conditionner l'affichage
**Backend** : SUPPORT_READONLY est deja defini dans `PlatformRole.java` et les controllers backend. C'est purement frontend.

---

## 3. Composants Angular Recommandes

### Architecture Actuelle (a conserver)

```
src/app/
├── core/
│   ├── guards/         auth.guard, role.guard, unsaved-changes.guard
│   ├── interceptors/   auth, tenant, correlation, error
│   ├── layout/         LayoutComponent (smart, shell)
│   └── services/       ApiService, AuthService, WebSocketService, TenantService
├── shared/
│   └── components/     RiskGauge, DeviceStatusBadge, SeverityBadge, SkeletonLoader, EmptyState, LiveAlertToast
├── features/
│   ├── dashboard/      DashboardComponent + DashboardStateService
│   ├── buildings/      BuildingListComponent, BuildingDetailComponent
│   ├── devices/        DeviceListComponent, DeviceDetailComponent
│   ├── alerts/         AlertListComponent, AlertDetailComponent
│   ├── interventions/  InterventionListComponent, InterventionDetailComponent
│   ├── reports/        ReportListComponent
│   ├── notifications/  NotificationListComponent
│   ├── settings/       SettingsComponent
│   └── admin/          AdminComponent
```

### Nouveaux composants a creer

| Composant | Type | Emplacement |
|-----------|------|-------------|
| `ElectricalPanelCardComponent` | Presentational | `shared/components/` |
| `AssignDialogComponent` | Smart (dialog) | `features/alerts/` |
| `ReadOnlyDirective` | Structural directive | `shared/directives/` |

---

## 4. Routes Angular

Routes existantes (toutes correctes) :

| Path | Guard | Component |
|------|-------|-----------|
| `/` | redirect → dashboard | — |
| `/dashboard` | authGuard | DashboardComponent |
| `/buildings` | authGuard | BuildingListComponent |
| `/buildings/:id` | authGuard | BuildingDetailComponent |
| `/devices` | authGuard | DeviceListComponent |
| `/devices/:id` | authGuard | DeviceDetailComponent |
| `/alerts` | authGuard | AlertListComponent |
| `/alerts/:id` | authGuard | AlertDetailComponent |
| `/interventions` | authGuard | InterventionListComponent |
| `/interventions/:id` | authGuard | InterventionDetailComponent |
| `/reports` | authGuard | ReportListComponent |
| `/notifications` | authGuard | NotificationListComponent |
| `/settings` | authGuard + roleGuard + unsavedChangesGuard | SettingsComponent |
| `/admin` | authGuard + roleGuard | AdminComponent |
| `**` | redirect → dashboard | — |

**Aucune nouvelle route necessaire.**

---

## 5. Services Angular

### Existants (complets)

| Service | Responsabilite |
|---------|---------------|
| `ApiService` | HTTP calls vers tous les endpoints backend |
| `AuthService` | Keycloak SSO, user profile, roles, signals |
| `WebSocketService` | STOMP client, real-time events |
| `DashboardStateService` | Etat dashboard (forkJoin 6 APIs) |
| `TenantService` | Contexte tenant |

### A enrichir

| Service | Changement |
|---------|-----------|
| `AuthService` | Ajouter `isReadOnly = computed(() => this.userRoles().length === 1 && this.userRoles()[0] === 'SUPPORT_READONLY')` |

---

## 6. Modeles TypeScript

### Existants (complets pour la spec)

Tous les DTOs request/response sont definis dans `api.service.ts` :
- `DeviceResponse`, `DeviceStatisticsResponse`
- `BuildingResponse`
- `AlertResponse`, `AlertDetailResponse`, `AlertStatistics`, `AlertCommentResponse`
- `InterventionResponse`, `KanbanResponse`, `InterventionStatistics`, `DiagnosticResponse`, `RiskImpactResponse`
- `ReportResponse`, `ReportMetadata`, `DownloadTokenResponse`
- `NotificationResponse`, `NotificationStatistics`
- `TelemetryPoint`, `RawTelemetryPoint`
- `RiskSummary`, `TenantRiskSummaryResponse`, `RiskHistoryPoint`
- `AnomalyResponse`

### A ajouter

```typescript
// Pour le groupement tableaux electriques
interface ElectricalPanel {
  panelId: string;
  devices: DeviceResponse[];
  activeCount: number;
  offlineCount: number;
  maxRiskScore: number;
}

// Pour l'enrichissement batiment
// (backend change) ajouter dans BuildingResponse :
highestAlertSeverity: string | null;
```

---

## 7. Guards de Securite

### Existants

| Guard | Type | Usage |
|-------|------|-------|
| `authGuard` | `CanActivateFn` | Verifie `isAuthenticated` |
| `roleGuard` | `CanActivateFn` | Verifie `hasAnyRole(route.data.roles)` |
| `unsavedChangesGuard` | `CanDeactivateFn` | Verifie `hasUnsavedChanges()` |

**Aucun nouveau guard necessaire.** Le mode SUPPORT_READONLY se gere via signal dans les composants, pas via un guard (car la route est accessible, seules les actions sont masquees).

---

## 8. Strategie State Management

### Approche Actuelle (correcte, a conserver)

- **Signals Angular** pour l'etat local des composants
- **DashboardStateService** : service injectable avec signals readonly + methode `load()`
- **WebSocketService** : observables RxJS pour evenements temps reel
- **Pas de store global** (NgRx/NGRX non necessaire a cette echelle)

### Pattern pour chaque ecran

```
Component.ngOnInit() → service.load() → signal.set(data) → template reactive
WebSocket.onEvent() → service.load() (refresh)
setInterval(60s) → service.load() (fallback polling)
```

---

## 9. Endpoints Backend Necessaires

### Existants et Suffisants

Tous les endpoints requis par la spec sont deja implementes :

| Endpoint | Service | Port |
|----------|---------|------|
| GET /api/v1/devices, /devices/statistics | device-service | 8082 |
| GET /api/v1/buildings, /buildings/:id | device-service | 8082 |
| GET /api/v1/alerts, /alerts/:id, /alerts/statistics | alerting-service | 8086 |
| POST /alerts/:id/acknowledge, /assign, /resolve, /false-positive, /comments | alerting-service | 8086 |
| GET /api/v1/interventions, /kanban, /statistics | maintenance-service | 8089 |
| POST /interventions/:id/assign, /start, /diagnostic, /complete, /cancel | maintenance-service | 8089 |
| GET /api/v1/reports | reporting-service | 8091 |
| POST /reports/monthly | reporting-service | 8091 |
| GET /api/v1/notifications, /statistics | notification-service | 8087 |
| GET/PUT /notifications/preferences/:userId | notification-service | 8087 |
| GET /api/v1/risk-scoring/tenant/summary, /tenant/history | risk-scoring | 8085 |
| GET /api/v1/risk-scoring/buildings/:id/summary, /history | risk-scoring | 8085 |
| GET /api/v1/telemetry/devices/:id | ingestion-service | 8083 |
| GET /api/v1/analysis/anomalies/:id | signal-analysis | 8084 |
| GET/PUT /tenants/:id/settings | identity-service | 8081 |
| CRUD /tenants/:id/emergency-contacts | identity-service | 8081 |

### A Ajouter (1 seul)

| Endpoint | Service | Justification |
|----------|---------|---------------|
| Enrichir `BuildingResponse` avec `highestAlertSeverity` | device-service ou alerting-service | Ecart 1 |

**Recommandation** : Ajouter le champ `highestAlertSeverity` dans le controller buildings existant, en faisant un appel interne au alerting-service, ou en ajoutant une vue denormalisee.

---

## 10. DTOs Request/Response

### Existants (pas de changement)

Les DTOs sont documentes dans `docs/api-documentation.md` et implementes dans les controllers backend.

### A Modifier

```java
// BuildingResponse enrichi (device-service ou via gateway aggregation)
{
  "id": "uuid",
  "name": "Residence Les Ormes",
  "address": "12 rue des Lilas, 75011 Paris",
  "totalDevices": 8,
  "activeDevices": 7,
  "riskScore": 42,
  "status": "WATCH",            // derive de riskScore
  "lastAlertAt": "2026-05-25T10:30:00Z",
  "highestAlertSeverity": "WARNING"  // NOUVEAU
}
```

---

## 11. Regles UX

### Navigation
- Sidebar toujours visible sur desktop, overlay sur mobile (<960px)
- Logo + nom app en haut de la sidebar
- Item actif surligne (aria-current="page")
- Badge notification dynamique dans la topbar

### Feedback
- Skeleton loaders pour tous les chargements
- Empty states contextuels (icone + message)
- Toast Material pour alertes temps reel (10s critique, 5s warning)
- SnackBar pour confirmations d'action (3s, "OK" dismiss)
- Indicateur connexion WebSocket (wifi/wifi_off)

### Tableaux
- Pagination Material (25/50/100 items)
- Colonnes fixes : pas de scroll horizontal sur desktop
- Lignes hover avec cursor pointer
- Liens vers details en bleu 1976d2

### Formulaires
- UnsavedChangesGuard sur Settings
- Validation client-side + messages d'erreur Material
- Bouton submit disabled tant que formulaire invalide

### Couleurs semantiques
- Critique/Critical : #d32f2f (rouge)
- Warning : #f57c00 (orange)
- Info : #1976d2 (bleu)
- OK/Resolved : #2e7d32 (vert)
- Offline/Revoked : #616161 (gris)

### Accessibilite
- Tous les boutons ont `aria-label`
- Canvas chart ont `role="img"` + `aria-label` descriptif
- Navigation a `role="navigation"` + `aria-label`
- Contraste WCAG AA sur tous les textes

### SUPPORT_READONLY
- Tous les boutons d'action masques (pas disabled, masques)
- Message contextuel "Mode consultation" si tentative navigation vers settings
- Badge "Lecture seule" dans la topbar

---

## 12. Cas d'Erreur

| Erreur | Comportement Frontend |
|--------|----------------------|
| 401 Unauthorized | Redirect vers Keycloak login |
| 403 Forbidden | SnackBar "Acces non autorise" + redirect dashboard |
| 404 Not Found | Empty state "Ressource introuvable" |
| 409 Conflict | SnackBar "Conflit — l'element a ete modifie par un autre utilisateur" |
| 422 Validation | Afficher erreurs inline sur les champs |
| 429 Rate Limited | SnackBar "Trop de requetes, reessayez dans quelques secondes" |
| 500 Internal Error | SnackBar "Erreur serveur — reessayez plus tard" + log correlationId |
| WebSocket disconnect | Indicateur passe en orange (reconnexion auto), polling fallback 60s |
| Timeout reseau | Retry 1x apres 3s, puis erreur |

---

## 13. Tests Frontend a Prevoir

### Tests Unitaires (Karma/Jasmine — 59 existants)

| Composant/Service | Tests a ajouter |
|-------------------|-----------------|
| `DeviceListComponent` | 6 tests (create, stats display, filter, search, pagination, empty state) |
| `AuthService.isReadOnly` | 2 tests (true quand SUPPORT_READONLY seul, false sinon) |
| `ElectricalPanelCardComponent` | 3 tests (render, device count, risk color) |
| `AssignDialogComponent` | 3 tests (open, submit, cancel) |
| `BuildingListComponent` | 2 tests (highestAlertSeverity badge, status derivation) |

**Total apres implementation : ~75 unit tests**

### Tests E2E (Cypress — 6 fichiers existants)

| Scenario | Fichier |
|----------|---------|
| Dashboard → stats → click building | `dashboard.cy.ts` (existant) |
| Buildings → detail → panels tab | `buildings.cy.ts` (existant, a enrichir) |
| Alerts → filter by building → detail → assign | `alerts.cy.ts` (existant, a enrichir) |
| Interventions → kanban | `interventions.cy.ts` (existant) |
| Reports → generate → download | `reports.cy.ts` (existant) |
| Realtime → connection indicator | `realtime.cy.ts` (existant) |
| Settings → notification prefs → save | A creer : `settings.cy.ts` |

---

## 14. Plan d'Implementation (Ordre des Ecarts)

| # | Ecart | Effort | Priorite |
|---|-------|--------|----------|
| 1 | Filtre par batiment sur alertes | 30 min | Haute |
| 2 | Action "Assigner" sur alert-detail | 30 min | Haute |
| 3 | Tableaux electriques dans building-detail | 1h | Moyenne |
| 4 | Preferences notification dans Settings | 45 min | Moyenne |
| 5 | Mode SUPPORT_READONLY | 1h | Moyenne |
| 6 | highestAlertSeverity sur building cards | 30 min (frontend) | Basse |

**Effort total estime : ~4h30**

---

## 15. Resume Executif

Le dashboard PyroSense est **a 90% conforme** a la specification cible. Les fondations sont solides :
- Architecture Angular 18 moderne (standalone, signals, lazy loading)
- Tous les ecrans implementes avec donnees reelles
- Temps reel WebSocket operationnel
- Tests unitaires (59) et E2E (6 fichiers)
- Responsive + accessible
- CI/CD + Docker complet

Les 6 ecarts identifies sont des enrichissements mineurs, pas des manques structurels. L'implementation requiert environ 4h30 de travail frontend, sans aucun nouveau endpoint backend (sauf enrichissement optionnel du `BuildingResponse`).
