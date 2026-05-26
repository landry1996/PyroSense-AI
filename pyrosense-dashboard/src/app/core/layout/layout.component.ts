import { Component, ViewChild, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatSidenavModule, MatSidenav } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BreakpointObserver } from '@angular/cdk/layout';
import { Subject, takeUntil } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { WebSocketService, WsEvent } from '../services/websocket.service';
import { LiveAlertToastComponent, LiveAlertData } from '../../shared/components/live-alert-toast.component';

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
    MatSnackBarModule,
    MatTooltipModule,
  ],
  template: `
    <mat-sidenav-container class="layout-container">
      <mat-sidenav #sidenav [mode]="isMobile() ? 'over' : 'side'" [opened]="!isMobile()" class="sidenav"
                   role="navigation" aria-label="Navigation principale">
        <div class="logo">
          <mat-icon class="logo-icon">local_fire_department</mat-icon>
          <span class="logo-text">PyroSense</span>
        </div>
        <mat-nav-list aria-label="Menu principal">
          <a mat-list-item routerLink="/dashboard" routerLinkActive="active" #rla="routerLinkActive"
             [attr.aria-current]="rla.isActive ? 'page' : null" (click)="closeMobileSidenav()">
            <mat-icon matListItemIcon>dashboard</mat-icon>
            <span matListItemTitle>Dashboard</span>
          </a>
          <a mat-list-item routerLink="/buildings" routerLinkActive="active" #rlaB="routerLinkActive"
             [attr.aria-current]="rlaB.isActive ? 'page' : null" (click)="closeMobileSidenav()">
            <mat-icon matListItemIcon>apartment</mat-icon>
            <span matListItemTitle>Batiments</span>
          </a>
          <a mat-list-item routerLink="/alerts" routerLinkActive="active" #rlaAl="routerLinkActive"
             [attr.aria-current]="rlaAl.isActive ? 'page' : null" (click)="closeMobileSidenav()">
            <mat-icon matListItemIcon>warning</mat-icon>
            <span matListItemTitle>Alertes</span>
          </a>
          <a mat-list-item routerLink="/devices" routerLinkActive="active" #rlaD="routerLinkActive"
             [attr.aria-current]="rlaD.isActive ? 'page' : null" (click)="closeMobileSidenav()">
            <mat-icon matListItemIcon>sensors</mat-icon>
            <span matListItemTitle>Capteurs</span>
          </a>
          <a mat-list-item routerLink="/interventions" routerLinkActive="active" #rlaI="routerLinkActive"
             [attr.aria-current]="rlaI.isActive ? 'page' : null" (click)="closeMobileSidenav()">
            <mat-icon matListItemIcon>build</mat-icon>
            <span matListItemTitle>Interventions</span>
          </a>
          <a mat-list-item routerLink="/reports" routerLinkActive="active" #rlaR="routerLinkActive"
             [attr.aria-current]="rlaR.isActive ? 'page' : null" (click)="closeMobileSidenav()">
            <mat-icon matListItemIcon>description</mat-icon>
            <span matListItemTitle>Rapports</span>
          </a>
          <a mat-list-item routerLink="/notifications" routerLinkActive="active" #rlaN="routerLinkActive"
             [attr.aria-current]="rlaN.isActive ? 'page' : null" (click)="closeMobileSidenav()">
            <mat-icon matListItemIcon>notifications</mat-icon>
            <span matListItemTitle>Notifications</span>
          </a>
          @if (auth.hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')) {
            <a mat-list-item routerLink="/settings" routerLinkActive="active" #rlaS="routerLinkActive"
               [attr.aria-current]="rlaS.isActive ? 'page' : null" (click)="closeMobileSidenav()">
              <mat-icon matListItemIcon>settings</mat-icon>
              <span matListItemTitle>Parametres</span>
            </a>
            <a mat-list-item routerLink="/admin" routerLinkActive="active" #rlaAd="routerLinkActive"
               [attr.aria-current]="rlaAd.isActive ? 'page' : null" (click)="closeMobileSidenav()">
              <mat-icon matListItemIcon>admin_panel_settings</mat-icon>
              <span matListItemTitle>Administration</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content class="content">
        <mat-toolbar color="primary" class="topbar">
          @if (isMobile()) {
            <button mat-icon-button (click)="sidenav.toggle()" aria-label="Ouvrir le menu">
              <mat-icon>menu</mat-icon>
            </button>
          }
          <span class="spacer"></span>
          <span class="connection-indicator" [class.connected]="ws.connected()" [class.reconnecting]="ws.reconnecting()"
                [matTooltip]="ws.connected() ? 'Temps reel actif' : ws.reconnecting() ? 'Reconnexion...' : 'Deconnecte'">
            <mat-icon>{{ ws.connected() ? 'wifi' : 'wifi_off' }}</mat-icon>
          </span>
          <button mat-icon-button [matBadge]="liveAlertCount()" [matBadgeHidden]="liveAlertCount() === 0"
                  matBadgeColor="warn" matBadgeSize="small"
                  aria-label="Notifications non lues">
            <mat-icon>notifications</mat-icon>
          </button>
          <button mat-icon-button (click)="auth.logout()" aria-label="Deconnexion">
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
    @media (max-width: 960px) {
      .sidenav { width: 220px; }
    }
    .connection-indicator {
      display: flex; align-items: center; margin-right: 8px;
      opacity: 0.7; font-size: 20px;
    }
    .connection-indicator mat-icon { font-size: 20px; width: 20px; height: 20px; }
    .connection-indicator.connected { color: #4caf50; opacity: 1; }
    .connection-indicator.reconnecting { color: #ff9800; animation: pulse 1.5s infinite; }
    @keyframes pulse { 0%, 100% { opacity: 0.4; } 50% { opacity: 1; } }
    @media (max-width: 600px) {
      .main-content { padding: 12px; }
      .sidenav { width: 200px; }
    }
  `],
})
export class LayoutComponent implements OnInit, OnDestroy {
  @ViewChild('sidenav') sidenav!: MatSidenav;
  isMobile = signal(false);
  liveAlertCount = signal(0);
  private destroy$ = new Subject<void>();

  constructor(
    public auth: AuthService,
    public ws: WebSocketService,
    private breakpointObserver: BreakpointObserver,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.breakpointObserver.observe('(max-width: 960px)')
      .pipe(takeUntil(this.destroy$))
      .subscribe(result => this.isMobile.set(result.matches));

    this.ws.connect();
    this.ws.onAlerts()
      .pipe(takeUntil(this.destroy$))
      .subscribe(event => this.handleLiveAlert(event));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.ws.disconnect();
  }

  closeMobileSidenav(): void {
    if (this.isMobile()) this.sidenav.close();
  }

  private handleLiveAlert(event: WsEvent): void {
    this.liveAlertCount.update(c => c + 1);
    const data = event.data;
    const toastData: LiveAlertData = {
      alertId: data.alertId || '',
      severity: data.severity || 'INFO',
      title: data.title || 'Nouvelle alerte',
      type: data.type || event.eventType,
    };
    this.snackBar.openFromComponent(LiveAlertToastComponent, {
      data: toastData,
      duration: toastData.severity === 'CRITICAL' ? 10000 : 5000,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['live-alert-snackbar'],
    });
  }
}
