import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subject, takeUntil } from 'rxjs';
import { DeviceTechnicalApiService } from '../../core/services/device-technical-api.service';
import { DeviceSecurityStatus } from '../../core/models/device-technical.model';
import { StatusChipComponent } from '../../shared/components/status-chip.component';
import { SkeletonLoaderComponent } from '../../shared/components/skeleton-loader.component';

@Component({
  selector: 'app-device-security',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    StatusChipComponent,
    SkeletonLoaderComponent,
  ],
  template: `
    <div class="security-container">
      <h1>Securite du capteur</h1>

      @if (loading()) {
        <div class="loading-state">
          <mat-spinner diameter="40" />
          <p>Chargement du statut de securite...</p>
        </div>
      } @else if (error()) {
        <div class="error-state">
          <mat-icon class="error-icon">error_outline</mat-icon>
          <h3>Erreur de chargement</h3>
          <p>{{ error() }}</p>
        </div>
      } @else if (security()) {
        <!-- Revoked warning banner -->
        @if (security()!.isRevoked) {
          <div class="warning-banner revoked-banner">
            <mat-icon>gpp_bad</mat-icon>
            <div class="banner-content">
              <strong>Capteur revoque</strong>
              <p>Les credentials de ce capteur ont ete revoques. Il ne peut plus transmettre de donnees.</p>
            </div>
          </div>
        }

        <!-- Credential Status -->
        <mat-card class="security-card">
          <mat-card-header>
            <mat-card-title>Statut des credentials</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="security-grid">
              <div class="security-item">
                <span class="security-label">Statut</span>
                <div class="credential-status">
                  <span class="status-dot" [class.active]="security()!.credentialStatus === 'ACTIVE'"
                        [class.revoked]="security()!.credentialStatus === 'REVOKED'"></span>
                  <app-status-chip [status]="security()!.credentialStatus" />
                </div>
              </div>
              <div class="security-item">
                <span class="security-label">Version credential</span>
                <span class="security-value">v{{ security()!.credentialVersion }}</span>
              </div>
              <div class="security-item">
                <span class="security-label">Derniere rotation</span>
                <span class="security-value">
                  {{ security()!.lastRotation ? (security()!.lastRotation | date:'dd/MM/yyyy HH:mm') : 'Jamais' }}
                </span>
              </div>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Security Events -->
        <mat-card class="security-card">
          <mat-card-header>
            <mat-card-title>Evenements de securite</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="security-events-grid">
              <div class="event-item" [class.alert]="security()!.failedAuthAttempts > 0">
                <mat-icon [class.danger-icon]="security()!.failedAuthAttempts > 0">
                  {{ security()!.failedAuthAttempts > 0 ? 'warning' : 'check_circle' }}
                </mat-icon>
                <div class="event-details">
                  <span class="event-count" [class.danger-count]="security()!.failedAuthAttempts > 0">
                    {{ security()!.failedAuthAttempts }}
                  </span>
                  <span class="event-label">Tentatives d'authentification echouees</span>
                </div>
              </div>

              <div class="event-item" [class.alert]="security()!.replayAttemptsBlocked > 0">
                <mat-icon [class.danger-icon]="security()!.replayAttemptsBlocked > 0">
                  {{ security()!.replayAttemptsBlocked > 0 ? 'shield' : 'verified_user' }}
                </mat-icon>
                <div class="event-details">
                  <span class="event-count" [class.danger-count]="security()!.replayAttemptsBlocked > 0">
                    {{ security()!.replayAttemptsBlocked }}
                  </span>
                  <span class="event-label">Tentatives de replay bloquees</span>
                </div>
              </div>

              @if (security()!.lastAuthFailure) {
                <div class="event-item alert">
                  <mat-icon class="danger-icon">access_time</mat-icon>
                  <div class="event-details">
                    <span class="event-value">{{ security()!.lastAuthFailure | date:'dd/MM/yyyy HH:mm' }}</span>
                    <span class="event-label">Dernier echec d'authentification</span>
                  </div>
                </div>
              }
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Security Summary -->
        <mat-card class="security-card summary-card" [class.secure]="isSecure()" [class.insecure]="!isSecure()">
          <mat-card-content>
            <div class="summary-content">
              <mat-icon class="summary-icon">
                {{ isSecure() ? 'verified_user' : 'gpp_maybe' }}
              </mat-icon>
              <div class="summary-text">
                <strong>{{ isSecure() ? 'Capteur securise' : 'Attention requise' }}</strong>
                <p>
                  @if (isSecure()) {
                    Aucune anomalie de securite detectee. Les credentials sont actifs et a jour.
                  } @else {
                    Des evenements de securite ont ete detectes. Verifiez les tentatives d'authentification.
                  }
                </p>
              </div>
            </div>
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .security-container { max-width: 900px; padding: 16px; }
    h1 { margin: 0 0 24px; font-size: 24px; font-weight: 500; }
    .loading-state { display: flex; flex-direction: column; align-items: center; padding: 48px; gap: 16px; }
    .loading-state p { color: #757575; }
    .error-state { display: flex; flex-direction: column; align-items: center; padding: 48px; text-align: center; }
    .error-icon { font-size: 64px; width: 64px; height: 64px; color: #d32f2f; margin-bottom: 16px; }
    .error-state h3 { margin: 0 0 8px; color: #d32f2f; }
    .error-state p { color: #757575; }

    .warning-banner {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 16px;
      border-radius: 8px;
      margin-bottom: 16px;
    }
    .revoked-banner {
      background: #ffebee;
      border: 1px solid #ef9a9a;
      color: #c62828;
    }
    .revoked-banner mat-icon { color: #c62828; margin-top: 2px; }
    .banner-content p { margin: 4px 0 0; font-size: 13px; opacity: 0.9; }

    .security-card { margin-bottom: 16px; }
    .security-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 24px;
      padding: 16px 0;
    }
    .security-item { display: flex; flex-direction: column; gap: 8px; }
    .security-label { font-size: 12px; color: rgba(0,0,0,0.6); text-transform: uppercase; letter-spacing: 0.5px; }
    .security-value { font-size: 16px; font-weight: 500; color: #212121; }
    .credential-status { display: flex; align-items: center; gap: 8px; }
    .status-dot { width: 10px; height: 10px; border-radius: 50%; }
    .status-dot.active { background: #4caf50; }
    .status-dot.revoked { background: #d32f2f; }

    .security-events-grid { display: flex; flex-direction: column; gap: 16px; padding: 16px 0; }
    .event-item {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 12px 16px;
      border-radius: 8px;
      background: #f5f5f5;
    }
    .event-item.alert { background: #fff3e0; }
    .event-details { display: flex; flex-direction: column; gap: 2px; }
    .event-count { font-size: 20px; font-weight: 600; color: #388e3c; }
    .event-count.danger-count { color: #d32f2f; }
    .event-value { font-size: 14px; font-weight: 500; color: #d32f2f; }
    .event-label { font-size: 12px; color: rgba(0,0,0,0.6); }
    .danger-icon { color: #d32f2f; }
    mat-icon:not(.danger-icon):not(.error-icon):not(.summary-icon) { color: #388e3c; }

    .summary-card.secure { border-left: 4px solid #4caf50; }
    .summary-card.insecure { border-left: 4px solid #f57c00; }
    .summary-content { display: flex; align-items: center; gap: 16px; padding: 8px 0; }
    .summary-icon { font-size: 40px; width: 40px; height: 40px; }
    .secure .summary-icon { color: #388e3c; }
    .insecure .summary-icon { color: #f57c00; }
    .summary-text p { margin: 4px 0 0; font-size: 13px; color: #757575; }

    @media (max-width: 600px) {
      .security-grid { grid-template-columns: 1fr; }
    }
  `],
})
export class DeviceSecurityComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private deviceId = '';

  loading = signal(true);
  error = signal<string | null>(null);
  security = signal<DeviceSecurityStatus | null>(null);

  constructor(
    private route: ActivatedRoute,
    private api: DeviceTechnicalApiService,
  ) {}

  ngOnInit(): void {
    this.deviceId = this.route.snapshot.paramMap.get('id')!;
    this.loadSecurity();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  isSecure(): boolean {
    const s = this.security();
    if (!s) return true;
    return !s.isRevoked && s.failedAuthAttempts === 0 && s.replayAttemptsBlocked === 0;
  }

  private loadSecurity(): void {
    this.api.getDeviceSecurityStatus(this.deviceId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.security.set(data);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Impossible de charger le statut de securite du capteur.');
          this.loading.set(false);
        },
      });
  }
}
