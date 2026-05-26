import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { unsavedChangesGuard } from './core/guards/unsaved-changes.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
  },
  {
    path: 'buildings',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/buildings/building-list.component').then(m => m.BuildingListComponent),
  },
  {
    path: 'buildings/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/buildings/building-detail.component').then(m => m.BuildingDetailComponent),
  },
  {
    path: 'devices',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/devices/device-list.component').then(m => m.DeviceListComponent),
  },
  {
    path: 'devices/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/devices/device-detail.component').then(m => m.DeviceDetailComponent),
  },
  {
    path: 'alerts',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/alerts/alert-list.component').then(m => m.AlertListComponent),
  },
  {
    path: 'alerts/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/alerts/alert-detail.component').then(m => m.AlertDetailComponent),
  },
  {
    path: 'interventions',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/interventions/intervention-list.component').then(m => m.InterventionListComponent),
  },
  {
    path: 'interventions/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/interventions/intervention-detail.component').then(m => m.InterventionDetailComponent),
  },
  {
    path: 'reports',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/reports/report-list.component').then(m => m.ReportListComponent),
  },
  {
    path: 'notifications',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/notifications/notification-list.component').then(m => m.NotificationListComponent),
  },
  {
    path: 'settings',
    canActivate: [authGuard, roleGuard],
    canDeactivate: [unsavedChangesGuard],
    data: { roles: ['PLATFORM_ADMIN', 'TENANT_ADMIN'] },
    loadComponent: () =>
      import('./features/settings/settings.component').then(m => m.SettingsComponent),
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['PLATFORM_ADMIN', 'TENANT_ADMIN'] },
    loadComponent: () =>
      import('./features/admin/admin.component').then(m => m.AdminComponent),
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
