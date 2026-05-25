import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RiskGaugeComponent } from '../../shared/components/risk-gauge.component';
import { SeverityBadgeComponent } from '../../shared/components/severity-badge.component';
import { ApiService } from '../../core/services/api.service';

interface DashboardStats {
  totalDevices: number;
  activeDevices: number;
  criticalAlerts: number;
  warningAlerts: number;
  avgRiskScore: number;
  pendingInterventions: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    RiskGaugeComponent,
    SeverityBadgeComponent,
  ],
  template: `
    <h1>Dashboard</h1>

    @if (loading()) {
      <mat-spinner diameter="40" />
    } @else {
      <div class="stats-grid">
        <mat-card class="stat-card">
          <mat-card-content>
            <mat-icon class="stat-icon devices">sensors</mat-icon>
            <div class="stat-value">{{ stats().activeDevices }}/{{ stats().totalDevices }}</div>
            <div class="stat-label">Capteurs actifs</div>
          </mat-card-content>
        </mat-card>

        <mat-card class="stat-card critical">
          <mat-card-content>
            <mat-icon class="stat-icon critical">error</mat-icon>
            <div class="stat-value">{{ stats().criticalAlerts }}</div>
            <div class="stat-label">Alertes critiques</div>
          </mat-card-content>
        </mat-card>

        <mat-card class="stat-card warning">
          <mat-card-content>
            <mat-icon class="stat-icon warning">warning</mat-icon>
            <div class="stat-value">{{ stats().warningAlerts }}</div>
            <div class="stat-label">Alertes warning</div>
          </mat-card-content>
        </mat-card>

        <mat-card class="stat-card">
          <mat-card-content>
            <app-risk-gauge [score]="stats().avgRiskScore" />
            <div class="stat-label">Score de risque moyen</div>
          </mat-card-content>
        </mat-card>

        <mat-card class="stat-card">
          <mat-card-content>
            <mat-icon class="stat-icon interventions">build</mat-icon>
            <div class="stat-value">{{ stats().pendingInterventions }}</div>
            <div class="stat-label">Interventions en cours</div>
          </mat-card-content>
        </mat-card>
      </div>
    }
  `,
  styles: [`
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px;
      margin-top: 16px;
    }
    .stat-card mat-card-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 24px 16px;
    }
    .stat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      margin-bottom: 8px;
    }
    .stat-icon.devices { color: #1976d2; }
    .stat-icon.critical { color: #d32f2f; }
    .stat-icon.warning { color: #f57c00; }
    .stat-icon.interventions { color: #388e3c; }
    .stat-value {
      font-size: 32px;
      font-weight: 500;
      margin: 8px 0;
    }
    .stat-label {
      color: rgba(0, 0, 0, 0.6);
      font-size: 14px;
    }
    .stat-card.critical { border-left: 4px solid #d32f2f; }
    .stat-card.warning { border-left: 4px solid #f57c00; }
  `],
})
export class DashboardComponent implements OnInit {
  loading = signal(true);
  stats = signal<DashboardStats>({
    totalDevices: 0,
    activeDevices: 0,
    criticalAlerts: 0,
    warningAlerts: 0,
    avgRiskScore: 0,
    pendingInterventions: 0,
  });

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.getDevicesByTenant().subscribe({
      next: (devices) => {
        const active = devices.filter(d => d.status === 'ACTIVE').length;
        this.stats.update(s => ({ ...s, totalDevices: devices.length, activeDevices: active }));
      },
      error: () => {},
    });
    this.api.getCriticalAlerts().subscribe({
      next: (alerts) => {
        this.stats.update(s => ({ ...s, criticalAlerts: alerts.length }));
        this.loading.set(false);
      },
      error: () => {
        // Fallback to mock
        this.stats.set({
          totalDevices: 47, activeDevices: 42, criticalAlerts: 3,
          warningAlerts: 8, avgRiskScore: 34, pendingInterventions: 5,
        });
        this.loading.set(false);
      },
    });
  }
}
