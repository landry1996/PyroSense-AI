import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

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
      import('./features/dashboard/dashboard.component').then(
        (m) => m.DashboardComponent
      ),
  },
  {
    path: 'alerts',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/alerts/alert-list.component').then(
        (m) => m.AlertListComponent
      ),
  },
  {
    path: 'alerts/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/alerts/alert-detail.component').then(
        (m) => m.AlertDetailComponent
      ),
  },
  {
    path: 'devices',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/devices/device-list.component').then(
        (m) => m.DeviceListComponent
      ),
  },
  {
    path: 'interventions',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/interventions/intervention-list.component').then(
        (m) => m.InterventionListComponent
      ),
  },
  {
    path: 'reports',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/reports/report-list.component').then(
        (m) => m.ReportListComponent
      ),
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['PLATFORM_ADMIN', 'TENANT_ADMIN'] },
    loadComponent: () =>
      import('./features/admin/admin.component').then(
        (m) => m.AdminComponent
      ),
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
