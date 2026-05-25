import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatBadgeModule,
  ],
  template: `
    <mat-sidenav-container class="layout-container">
      <mat-sidenav mode="side" opened class="sidenav">
        <div class="logo">
          <mat-icon class="logo-icon">local_fire_department</mat-icon>
          <span class="logo-text">PyroSense</span>
        </div>
        <mat-nav-list>
          <a mat-list-item routerLink="/dashboard" routerLinkActive="active">
            <mat-icon matListItemIcon>dashboard</mat-icon>
            <span matListItemTitle>Dashboard</span>
          </a>
          <a mat-list-item routerLink="/buildings" routerLinkActive="active">
            <mat-icon matListItemIcon>apartment</mat-icon>
            <span matListItemTitle>Batiments</span>
          </a>
          <a mat-list-item routerLink="/alerts" routerLinkActive="active">
            <mat-icon matListItemIcon>warning</mat-icon>
            <span matListItemTitle>Alertes</span>
          </a>
          <a mat-list-item routerLink="/devices" routerLinkActive="active">
            <mat-icon matListItemIcon>sensors</mat-icon>
            <span matListItemTitle>Capteurs</span>
          </a>
          <a mat-list-item routerLink="/interventions" routerLinkActive="active">
            <mat-icon matListItemIcon>build</mat-icon>
            <span matListItemTitle>Interventions</span>
          </a>
          <a mat-list-item routerLink="/reports" routerLinkActive="active">
            <mat-icon matListItemIcon>description</mat-icon>
            <span matListItemTitle>Rapports</span>
          </a>
          <a mat-list-item routerLink="/notifications" routerLinkActive="active">
            <mat-icon matListItemIcon>notifications</mat-icon>
            <span matListItemTitle>Notifications</span>
          </a>
          @if (auth.hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')) {
            <a mat-list-item routerLink="/admin" routerLinkActive="active">
              <mat-icon matListItemIcon>admin_panel_settings</mat-icon>
              <span matListItemTitle>Administration</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content class="content">
        <mat-toolbar color="primary" class="topbar">
          <span class="spacer"></span>
          <button mat-icon-button [matBadge]="'3'" matBadgeColor="warn" matBadgeSize="small">
            <mat-icon>notifications</mat-icon>
          </button>
          <button mat-icon-button (click)="auth.logout()">
            <mat-icon>logout</mat-icon>
          </button>
        </mat-toolbar>
        <main class="main-content">
          <ng-content />
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .layout-container {
      height: 100vh;
    }
    .sidenav {
      width: 250px;
      background: #1a237e;
      color: white;
    }
    .logo {
      display: flex;
      align-items: center;
      padding: 16px;
      gap: 8px;
    }
    .logo-icon {
      color: #ff5722;
      font-size: 32px;
      width: 32px;
      height: 32px;
    }
    .logo-text {
      font-size: 20px;
      font-weight: 500;
    }
    mat-nav-list a {
      color: rgba(255, 255, 255, 0.8);
    }
    mat-nav-list a.active {
      color: white;
      background: rgba(255, 255, 255, 0.1);
    }
    .content {
      display: flex;
      flex-direction: column;
    }
    .topbar {
      position: sticky;
      top: 0;
      z-index: 100;
    }
    .spacer {
      flex: 1;
    }
    .main-content {
      padding: 24px;
      flex: 1;
      overflow-y: auto;
    }
  `],
})
export class LayoutComponent {
  constructor(public auth: AuthService) {}
}
