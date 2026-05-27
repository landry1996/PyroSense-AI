import { Component, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { Subject, takeUntil } from 'rxjs';
import { DeviceTechnicalApiService } from '../../core/services/device-technical-api.service';
import { DeviceTechnicalHealth } from '../../core/models/device-technical.model';
import { MetricCardComponent } from '../../shared/components/metric-card.component';
import { StatusChipComponent } from '../../shared/components/status-chip.component';
import { SkeletonLoaderComponent } from '../../shared/components/skeleton-loader.component';

@Component({
  selector: 'app-device-technical-overview',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MetricCardComponent,
    StatusChipComponent,
    SkeletonLoaderComponent,
  ],
  template: `
    <div class="technical-overview-container">
      <h1>Surveillance technique</h1>

      @if (loading()) {
        <div class="loading-state">
          <mat-spinner diameter="40" />
          <p>Chargement des donnees techniques...</p>
        </div>
      } @else if (error()) {
        <div class="error-state">
          <mat-icon class="error-icon">error_outline</mat-icon>
          <h3>Erreur de chargement</h3>
          <p>{{ error() }}</p>
        </div>
      } @else if (health()) {
        <!-- Device Identification -->
        <mat-card class="identification-card">
          <mat-card-header>
            <mat-card-title>Identification du capteur</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="identification-grid">
              <div class="id-item">
                <span class="id-label">Numero de serie</span>
                <span class="id-value">{{ health()!.serialNumber }}</span>
              </div>
              <div class="id-item">
                <span class="id-label">Firmware</span>
                <span class="id-value">{{ health()!.firmwareVersion }}</span>
              </div>
              <div class="id-item">
                <span class="id-label">Revision materielle</span>
                <span class="id-value">{{ health()!.hardwareRevision }}</span>
              </div>
              <div class="id-item">
                <span class="id-label">Connectivite</span>
                <span class="id-value">{{ health()!.connectivity }}</span>
              </div>
              <div class="id-item">
                <span class="id-label">Statut</span>
                <app-status-chip [status]="health()!.status" />
              </div>
              <div class="id-item">
                <span class="id-label">Dernier heartbeat</span>
                <span class="id-value">{{ formatRelativeTime(health()!.lastHeartbeat) }}</span>
              </div>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Health Metrics -->
        <h2>Metriques de sante</h2>
        <div class="metrics-grid">
          <app-metric-card
            icon="signal_cellular_alt"
            [iconColor]="getSignalColor(health()!.signalQuality)"
            [value]="health()!.signalQuality + '%'"
            label="Qualite du signal"
            [variant]="getSignalVariant(health()!.signalQuality)"
          />
          <app-metric-card
            icon="verified"
            [iconColor]="getQualityColor(health()!.dataQualityScore)"
            [value]="health()!.dataQualityScore + ' (' + health()!.dataQualityGrade + ')'"
            label="Score qualite donnees"
            [variant]="getQualityVariant(health()!.dataQualityScore)"
          />
          <app-metric-card
            icon="timer"
            iconColor="#1976d2"
            [value]="formatUptime(health()!.uptimeSeconds)"
            label="Temps de fonctionnement"
          />
          <app-metric-card
            icon="schedule"
            [iconColor]="getClockDriftColor(health()!.clockDriftMs)"
            [value]="health()!.clockDriftMs + ' ms'"
            label="Derive horloge"
            [variant]="getClockDriftVariant(health()!.clockDriftMs)"
          />
          <app-metric-card
            icon="broken_image"
            [iconColor]="getGapsColor(health()!.sequenceGaps)"
            [value]="health()!.sequenceGaps.toString()"
            label="Trous de sequence"
            [variant]="getGapsVariant(health()!.sequenceGaps)"
          />
          <app-metric-card
            icon="block"
            [iconColor]="getRejectedColor(health()!.rejectedTelemetryCount)"
            [value]="health()!.rejectedTelemetryCount.toString()"
            label="Telemetries rejetees"
            [variant]="getRejectedVariant(health()!.rejectedTelemetryCount)"
          />
        </div>

        <!-- Additional info -->
        @if (health()!.batteryPercent !== null || health()!.deviceTemperature !== null) {
          <h2>Informations complementaires</h2>
          <div class="metrics-grid">
            @if (health()!.batteryPercent !== null) {
              <app-metric-card
                icon="battery_std"
                [iconColor]="getBatteryColor(health()!.batteryPercent!)"
                [value]="health()!.batteryPercent + '%'"
                label="Batterie"
                [variant]="getBatteryVariant(health()!.batteryPercent!)"
              />
            }
            @if (health()!.deviceTemperature !== null) {
              <app-metric-card
                icon="thermostat"
                [iconColor]="getTempColor(health()!.deviceTemperature!)"
                [value]="health()!.deviceTemperature + ' °C'"
                label="Temperature capteur"
                [variant]="getTempVariant(health()!.deviceTemperature!)"
              />
            }
          </div>
        }
      }
    </div>
  `,
  styles: [`
    .technical-overview-container { max-width: 1100px; padding: 16px; }
    h1 { margin: 0 0 24px; font-size: 24px; font-weight: 500; }
    h2 { margin: 24px 0 16px; font-size: 18px; font-weight: 500; color: #424242; }
    .loading-state { display: flex; flex-direction: column; align-items: center; padding: 48px; gap: 16px; }
    .loading-state p { color: #757575; }
    .error-state { display: flex; flex-direction: column; align-items: center; padding: 48px; text-align: center; }
    .error-icon { font-size: 64px; width: 64px; height: 64px; color: #d32f2f; margin-bottom: 16px; }
    .error-state h3 { margin: 0 0 8px; color: #d32f2f; }
    .error-state p { color: #757575; }
    .identification-card { margin-bottom: 16px; }
    .identification-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
      padding: 16px 0;
    }
    .id-item { display: flex; flex-direction: column; gap: 4px; }
    .id-label { font-size: 12px; color: rgba(0,0,0,0.6); text-transform: uppercase; letter-spacing: 0.5px; }
    .id-value { font-size: 14px; font-weight: 500; color: #212121; }
    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 16px;
    }

    @media (max-width: 600px) {
      .identification-grid { grid-template-columns: repeat(2, 1fr); }
      .metrics-grid { grid-template-columns: repeat(2, 1fr); }
    }
  `],
})
export class DeviceTechnicalOverviewComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private deviceId = '';

  loading = signal(true);
  error = signal<string | null>(null);
  health = signal<DeviceTechnicalHealth | null>(null);

  constructor(
    private route: ActivatedRoute,
    private api: DeviceTechnicalApiService,
  ) {}

  ngOnInit(): void {
    this.deviceId = this.route.snapshot.paramMap.get('id')!;
    this.loadHealth();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadHealth(): void {
    this.api.getDeviceTechnicalHealth(this.deviceId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.health.set(data);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Impossible de charger les donnees techniques du capteur.');
          this.loading.set(false);
        },
      });
  }

  formatUptime(seconds: number): string {
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    if (days > 0) return `${days}j ${hours}h`;
    const minutes = Math.floor((seconds % 3600) / 60);
    return `${hours}h ${minutes}min`;
  }

  formatRelativeTime(isoDate: string): string {
    const diff = Date.now() - new Date(isoDate).getTime();
    const minutes = Math.floor(diff / 60000);
    if (minutes < 1) return 'A l\'instant';
    if (minutes < 60) return `Il y a ${minutes} min`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `Il y a ${hours}h`;
    const days = Math.floor(hours / 24);
    return `Il y a ${days}j`;
  }

  // Signal quality thresholds
  getSignalColor(quality: number): string {
    if (quality >= 70) return '#388e3c';
    if (quality >= 40) return '#f57c00';
    return '#d32f2f';
  }

  getSignalVariant(quality: number): 'default' | 'critical' | 'warning' {
    if (quality >= 70) return 'default';
    if (quality >= 40) return 'warning';
    return 'critical';
  }

  // Data quality thresholds
  getQualityColor(score: number): string {
    if (score >= 80) return '#388e3c';
    if (score >= 50) return '#f57c00';
    return '#d32f2f';
  }

  getQualityVariant(score: number): 'default' | 'critical' | 'warning' {
    if (score >= 80) return 'default';
    if (score >= 50) return 'warning';
    return 'critical';
  }

  // Clock drift thresholds
  getClockDriftColor(ms: number): string {
    if (ms <= 100) return '#388e3c';
    if (ms <= 500) return '#f57c00';
    return '#d32f2f';
  }

  getClockDriftVariant(ms: number): 'default' | 'critical' | 'warning' {
    if (ms <= 100) return 'default';
    if (ms <= 500) return 'warning';
    return 'critical';
  }

  // Sequence gaps thresholds
  getGapsColor(gaps: number): string {
    if (gaps === 0) return '#388e3c';
    if (gaps <= 5) return '#f57c00';
    return '#d32f2f';
  }

  getGapsVariant(gaps: number): 'default' | 'critical' | 'warning' {
    if (gaps === 0) return 'default';
    if (gaps <= 5) return 'warning';
    return 'critical';
  }

  // Rejected telemetry thresholds
  getRejectedColor(count: number): string {
    if (count === 0) return '#388e3c';
    if (count <= 10) return '#f57c00';
    return '#d32f2f';
  }

  getRejectedVariant(count: number): 'default' | 'critical' | 'warning' {
    if (count === 0) return 'default';
    if (count <= 10) return 'warning';
    return 'critical';
  }

  // Battery thresholds
  getBatteryColor(percent: number): string {
    if (percent >= 50) return '#388e3c';
    if (percent >= 20) return '#f57c00';
    return '#d32f2f';
  }

  getBatteryVariant(percent: number): 'default' | 'critical' | 'warning' {
    if (percent >= 50) return 'default';
    if (percent >= 20) return 'warning';
    return 'critical';
  }

  // Temperature thresholds
  getTempColor(temp: number): string {
    if (temp <= 60) return '#388e3c';
    if (temp <= 80) return '#f57c00';
    return '#d32f2f';
  }

  getTempVariant(temp: number): 'default' | 'critical' | 'warning' {
    if (temp <= 60) return 'default';
    if (temp <= 80) return 'warning';
    return 'critical';
  }
}
