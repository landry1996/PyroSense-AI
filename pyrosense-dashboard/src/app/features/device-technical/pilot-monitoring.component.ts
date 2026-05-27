import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { Subject, takeUntil } from 'rxjs';
import { DeviceTechnicalApiService } from '../../core/services/device-technical-api.service';
import { PilotDashboard, PilotDeviceSummary } from '../../core/models/device-technical.model';
import { MetricCardComponent } from '../../shared/components/metric-card.component';
import { StatusChipComponent } from '../../shared/components/status-chip.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { SkeletonLoaderComponent } from '../../shared/components/skeleton-loader.component';

@Component({
  selector: 'app-pilot-monitoring',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MetricCardComponent,
    StatusChipComponent,
    EmptyStateComponent,
    SkeletonLoaderComponent,
  ],
  template: `
    <div class="pilot-monitoring-container">
      @if (loading()) {
        <div class="loading-state">
          <mat-spinner diameter="40" />
          <p>Chargement du tableau de bord pilote...</p>
        </div>
      } @else if (error()) {
        <div class="error-state">
          <mat-icon class="error-icon">error_outline</mat-icon>
          <h3>Erreur de chargement</h3>
          <p>{{ error() }}</p>
        </div>
      } @else if (dashboard()) {
        <!-- Header -->
        <div class="pilot-header">
          <h1>{{ dashboard()!.pilotName }}</h1>
          <app-status-chip [status]="dashboard()!.status" />
        </div>

        <!-- KPI Summary Cards -->
        @if (dashboard()!.kpiSnapshot) {
          <h2>Indicateurs cles</h2>
          <div class="kpi-grid">
            <app-metric-card
              icon="speed"
              iconColor="#1976d2"
              [value]="dashboard()!.kpiSnapshot!.uptimePercent + '%'"
              label="Disponibilite"
              [variant]="dashboard()!.kpiSnapshot!.uptimePercent >= 95 ? 'default' : 'warning'"
            />
            <app-metric-card
              icon="signal_cellular_alt"
              [iconColor]="dashboard()!.kpiSnapshot!.avgSignalQuality >= 70 ? '#388e3c' : '#f57c00'"
              [value]="dashboard()!.kpiSnapshot!.avgSignalQuality + '%'"
              label="Qualite signal moyenne"
              [variant]="dashboard()!.kpiSnapshot!.avgSignalQuality >= 70 ? 'default' : 'warning'"
            />
            <app-metric-card
              icon="report_problem"
              [iconColor]="dashboard()!.kpiSnapshot!.incidentsOpen > 0 ? '#f57c00' : '#388e3c'"
              [value]="dashboard()!.kpiSnapshot!.incidentsOpen.toString()"
              label="Incidents ouverts"
              [variant]="dashboard()!.kpiSnapshot!.incidentsOpen > 0 ? 'warning' : 'default'"
            />
            <app-metric-card
              icon="cancel"
              [iconColor]="dashboard()!.kpiSnapshot!.falsePositiveRate > 20 ? '#d32f2f' : '#388e3c'"
              [value]="dashboard()!.kpiSnapshot!.falsePositiveRate + '%'"
              label="Taux faux positifs"
              [variant]="dashboard()!.kpiSnapshot!.falsePositiveRate > 20 ? 'critical' : 'default'"
            />
            <app-metric-card
              icon="devices"
              iconColor="#5c6bc0"
              [value]="dashboard()!.kpiSnapshot!.activeDevices + '/' + dashboard()!.kpiSnapshot!.totalDevices"
              label="Capteurs actifs"
            />
            <app-metric-card
              icon="verified"
              iconColor="#1976d2"
              [value]="dashboard()!.kpiSnapshot!.telemetryValidPercent + '%'"
              label="Telemetrie valide"
              [variant]="dashboard()!.kpiSnapshot!.telemetryValidPercent >= 90 ? 'default' : 'warning'"
            />
          </div>
        }

        <!-- Incidents Summary -->
        <h2>Resume des incidents</h2>
        <div class="incidents-grid">
          <div class="incident-card open">
            <span class="incident-count">{{ dashboard()!.incidentsSummary.open }}</span>
            <span class="incident-label">Ouverts</span>
          </div>
          <div class="incident-card investigating">
            <span class="incident-count">{{ dashboard()!.incidentsSummary.investigating }}</span>
            <span class="incident-label">En investigation</span>
          </div>
          <div class="incident-card resolved">
            <span class="incident-count">{{ dashboard()!.incidentsSummary.resolved }}</span>
            <span class="incident-label">Resolus</span>
          </div>
          <div class="incident-card total">
            <span class="incident-count">{{ dashboard()!.incidentsSummary.total }}</span>
            <span class="incident-label">Total</span>
          </div>
        </div>

        <!-- Device Table -->
        <h2>Capteurs du pilote</h2>
        @if (dashboard()!.devices.length === 0) {
          <app-empty-state
            icon="sensors_off"
            title="Aucun capteur"
            message="Aucun capteur n'est associe a ce pilote pour le moment."
          />
        } @else {
          <mat-card class="table-card">
            <mat-card-content>
              <table mat-table [dataSource]="dashboard()!.devices" class="device-table">
                <ng-container matColumnDef="serialNumber">
                  <th mat-header-cell *matHeaderCellDef>Numero de serie</th>
                  <td mat-cell *matCellDef="let device">{{ device.serialNumber }}</td>
                </ng-container>

                <ng-container matColumnDef="status">
                  <th mat-header-cell *matHeaderCellDef>Statut</th>
                  <td mat-cell *matCellDef="let device">
                    <app-status-chip [status]="device.status" />
                  </td>
                </ng-container>

                <ng-container matColumnDef="signalQuality">
                  <th mat-header-cell *matHeaderCellDef>Signal</th>
                  <td mat-cell *matCellDef="let device">
                    <div class="signal-bar-container">
                      <div class="signal-bar" [style.width.%]="device.signalQuality"
                           [class.signal-good]="device.signalQuality >= 70"
                           [class.signal-warning]="device.signalQuality >= 40 && device.signalQuality < 70"
                           [class.signal-critical]="device.signalQuality < 40">
                      </div>
                      <span class="signal-value">{{ device.signalQuality }}%</span>
                    </div>
                  </td>
                </ng-container>

                <ng-container matColumnDef="dataQualityGrade">
                  <th mat-header-cell *matHeaderCellDef>Qualite</th>
                  <td mat-cell *matCellDef="let device">
                    <span class="grade-badge" [class]="'grade-' + device.dataQualityGrade.toLowerCase()">
                      {{ device.dataQualityGrade }}
                    </span>
                  </td>
                </ng-container>

                <ng-container matColumnDef="lastHeartbeat">
                  <th mat-header-cell *matHeaderCellDef>Dernier heartbeat</th>
                  <td mat-cell *matCellDef="let device">{{ formatRelativeTime(device.lastHeartbeat) }}</td>
                </ng-container>

                <ng-container matColumnDef="installationStatus">
                  <th mat-header-cell *matHeaderCellDef>Installation</th>
                  <td mat-cell *matCellDef="let device">
                    <app-status-chip [status]="device.installationStatus" />
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="deviceColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: deviceColumns;"></tr>
              </table>
            </mat-card-content>
          </mat-card>
        }
      }
    </div>
  `,
  styles: [`
    .pilot-monitoring-container { max-width: 1200px; padding: 16px; }
    .pilot-header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
    .pilot-header h1 { margin: 0; font-size: 24px; font-weight: 500; }
    h2 { margin: 24px 0 16px; font-size: 18px; font-weight: 500; color: #424242; }
    .loading-state { display: flex; flex-direction: column; align-items: center; padding: 48px; gap: 16px; }
    .loading-state p { color: #757575; }
    .error-state { display: flex; flex-direction: column; align-items: center; padding: 48px; text-align: center; }
    .error-icon { font-size: 64px; width: 64px; height: 64px; color: #d32f2f; margin-bottom: 16px; }
    .error-state h3 { margin: 0 0 8px; color: #d32f2f; }
    .error-state p { color: #757575; }

    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
      gap: 16px;
    }

    .incidents-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
      gap: 16px;
      margin-bottom: 16px;
    }
    .incident-card {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 20px 16px;
      border-radius: 8px;
      border: 1px solid #e0e0e0;
    }
    .incident-card.open { border-left: 4px solid #1565c0; background: #e3f2fd; }
    .incident-card.investigating { border-left: 4px solid #f57c00; background: #fff3e0; }
    .incident-card.resolved { border-left: 4px solid #388e3c; background: #e8f5e9; }
    .incident-card.total { border-left: 4px solid #5c6bc0; background: #e8eaf6; }
    .incident-count { font-size: 28px; font-weight: 600; color: #212121; }
    .incident-label { font-size: 12px; color: rgba(0,0,0,0.6); margin-top: 4px; }

    .table-card { overflow-x: auto; }
    .device-table { width: 100%; }

    .signal-bar-container { display: flex; align-items: center; gap: 8px; min-width: 120px; }
    .signal-bar {
      height: 8px;
      border-radius: 4px;
      transition: width 0.3s ease;
    }
    .signal-good { background: #4caf50; }
    .signal-warning { background: #ff9800; }
    .signal-critical { background: #f44336; }
    .signal-value { font-size: 12px; font-weight: 500; color: #616161; white-space: nowrap; }

    .grade-badge {
      display: inline-block;
      padding: 2px 10px;
      border-radius: 12px;
      font-weight: 600;
      font-size: 12px;
    }
    .grade-a { background: #e8f5e9; color: #2e7d32; }
    .grade-b { background: #f1f8e9; color: #558b2f; }
    .grade-c { background: #fff3e0; color: #e65100; }
    .grade-d { background: #fff3e0; color: #bf360c; }
    .grade-f { background: #ffebee; color: #c62828; }

    @media (max-width: 768px) {
      .kpi-grid { grid-template-columns: repeat(2, 1fr); }
      .incidents-grid { grid-template-columns: repeat(2, 1fr); }
      .device-table { font-size: 12px; }
    }
    @media (max-width: 480px) {
      .kpi-grid { grid-template-columns: 1fr; }
      .incidents-grid { grid-template-columns: repeat(2, 1fr); }
    }
  `],
})
export class PilotMonitoringComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private pilotId = '';

  loading = signal(true);
  error = signal<string | null>(null);
  dashboard = signal<PilotDashboard | null>(null);

  deviceColumns = ['serialNumber', 'status', 'signalQuality', 'dataQualityGrade', 'lastHeartbeat', 'installationStatus'];

  constructor(
    private route: ActivatedRoute,
    private api: DeviceTechnicalApiService,
  ) {}

  ngOnInit(): void {
    this.pilotId = this.route.snapshot.paramMap.get('id')!;
    this.loadDashboard();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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

  private loadDashboard(): void {
    this.api.getPilotDashboard(this.pilotId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.dashboard.set(data);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Impossible de charger le tableau de bord du pilote.');
          this.loading.set(false);
        },
      });
  }
}
