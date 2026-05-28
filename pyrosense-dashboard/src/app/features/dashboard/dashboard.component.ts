import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RiskGaugeComponent } from '../../shared/components/risk-gauge.component';
import { SkeletonLoaderComponent } from '../../shared/components/skeleton-loader.component';
import { DashboardStateService } from './dashboard-state.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { Subject, takeUntil } from 'rxjs';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatCardModule, MatIconModule, MatButtonModule,
    RiskGaugeComponent, SkeletonLoaderComponent,
  ],
  template: `
    <div class="dashboard-container">
      <h1>Dashboard</h1>

      @if (state.loading()) {
        <div class="stats-grid">
          @for (i of [1,2,3,4,5,6]; track i) {
            <mat-card class="stat-card"><mat-card-content><app-skeleton type="stat" /></mat-card-content></mat-card>
          }
        </div>
        <div class="bottom-row">
          <mat-card><mat-card-content><app-skeleton type="card" /></mat-card-content></mat-card>
          <mat-card><mat-card-content><app-skeleton type="card" [count]="5" /></mat-card-content></mat-card>
        </div>
      } @else {
        <div class="stats-grid">
          <mat-card class="stat-card clickable" routerLink="/buildings" role="link"
                    [attr.aria-label]="'Batiments: ' + state.summary().totalBuildings">
            <mat-card-content>
              <mat-icon class="stat-icon buildings">apartment</mat-icon>
              <div class="stat-value">{{ state.summary().totalBuildings }}</div>
              <div class="stat-label">Batiments</div>
            </mat-card-content>
          </mat-card>

          <mat-card class="stat-card clickable" routerLink="/devices" role="link"
                    [attr.aria-label]="'Capteurs actifs: ' + state.summary().activeDevices + ' sur ' + state.summary().totalDevices">
            <mat-card-content>
              <mat-icon class="stat-icon devices">sensors</mat-icon>
              <div class="stat-value">{{ state.summary().activeDevices }}/{{ state.summary().totalDevices }}</div>
              <div class="stat-label">Capteurs actifs</div>
            </mat-card-content>
          </mat-card>

          <mat-card class="stat-card danger" [class.pulse]="state.summary().offlineDevices > 0" routerLink="/devices">
            <mat-card-content>
              <mat-icon class="stat-icon offline">wifi_off</mat-icon>
              <div class="stat-value">{{ state.summary().offlineDevices }}</div>
              <div class="stat-label">Capteurs hors-ligne</div>
            </mat-card-content>
          </mat-card>

          <mat-card class="stat-card critical clickable" routerLink="/alerts">
            <mat-card-content>
              <mat-icon class="stat-icon critical">error</mat-icon>
              <div class="stat-value">{{ state.summary().criticalAlerts }}</div>
              <div class="stat-label">Alertes critiques</div>
            </mat-card-content>
          </mat-card>

          <mat-card class="stat-card warning clickable" routerLink="/alerts">
            <mat-card-content>
              <mat-icon class="stat-icon warning">warning</mat-icon>
              <div class="stat-value">{{ state.summary().warningAlerts }}</div>
              <div class="stat-label">Alertes warning</div>
            </mat-card-content>
          </mat-card>

          <mat-card class="stat-card overdue clickable" routerLink="/interventions">
            <mat-card-content>
              <mat-icon class="stat-icon overdue">schedule</mat-icon>
              <div class="stat-value">{{ state.summary().overdueInterventions }}</div>
              <div class="stat-label">Interventions en retard</div>
            </mat-card-content>
          </mat-card>
        </div>

        <div class="bottom-row">
          <mat-card class="risk-card">
            <mat-card-header>
              <mat-card-title>Score de risque moyen</mat-card-title>
              <mat-card-subtitle>
                Tendance: {{ getTrendLabel(state.summary().riskTrend) }}
                <mat-icon class="trend-icon" [class]="'trend-' + state.summary().riskTrend.toLowerCase()">
                  {{ getTrendIcon(state.summary().riskTrend) }}
                </mat-icon>
              </mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              <app-risk-gauge [score]="state.summary().avgRiskScore" />
              <div class="risk-info">
                <span class="buildings-at-risk">{{ state.summary().buildingsAtRisk }} batiment(s) a risque</span>
              </div>
            </mat-card-content>
          </mat-card>

          <mat-card class="chart-card">
            <mat-card-header>
              <mat-card-title>Evolution du risque (30 jours)</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <div class="chart-container">
                <canvas #riskChart role="img" aria-label="Graphique d'evolution du score de risque sur 30 jours"></canvas>
              </div>
            </mat-card-content>
          </mat-card>
        </div>
      }
    </div>
  `,
  styles: [`
    .dashboard-container { max-width: 1200px; }
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 16px;
      margin-top: 16px;
    }
    .stat-card mat-card-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 20px 16px;
    }
    .stat-card.clickable { cursor: pointer; transition: box-shadow 0.2s, transform 0.2s; }
    .stat-card.clickable:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.15); transform: translateY(-2px); }
    .stat-icon { font-size: 36px; width: 36px; height: 36px; margin-bottom: 8px; }
    .stat-icon.buildings { color: #5c6bc0; }
    .stat-icon.devices { color: #1976d2; }
    .stat-icon.offline { color: #9e9e9e; }
    .stat-icon.critical { color: #d32f2f; }
    .stat-icon.warning { color: #f57c00; }
    .stat-icon.overdue { color: #e65100; }
    .stat-value { font-size: 28px; font-weight: 600; margin: 4px 0; }
    .stat-label { color: rgba(0,0,0,0.6); font-size: 12px; text-align: center; }
    .stat-card.critical { border-left: 4px solid #d32f2f; }
    .stat-card.warning { border-left: 4px solid #f57c00; }
    .stat-card.danger { border-left: 4px solid #9e9e9e; }
    .stat-card.overdue { border-left: 4px solid #e65100; }
    .stat-card.pulse .stat-value { animation: pulse 2s infinite; }
    @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.6; } }

    .bottom-row { display: grid; grid-template-columns: 280px 1fr; gap: 16px; margin-top: 24px; }
    .risk-card mat-card-content { display: flex; flex-direction: column; align-items: center; padding: 16px; }
    .risk-info { margin-top: 12px; text-align: center; }
    .buildings-at-risk { font-size: 13px; color: #e65100; font-weight: 500; }
    .trend-icon { font-size: 18px; width: 18px; height: 18px; vertical-align: middle; }
    .trend-improving { color: #2e7d32; }
    .trend-stable { color: #1976d2; }
    .trend-degrading { color: #f57c00; }
    .trend-critical { color: #d32f2f; }

    .chart-card mat-card-content { padding: 16px; }
    .chart-container { position: relative; height: 220px; width: 100%; }

    @media (max-width: 960px) {
      .bottom-row { grid-template-columns: 1fr; }
      .stats-grid { grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); }
    }
    @media (max-width: 600px) {
      .stats-grid { grid-template-columns: repeat(2, 1fr); }
      .bottom-row { grid-template-columns: 1fr; }
    }
    @media (prefers-reduced-motion: reduce) {
      .stat-card.pulse .stat-value { animation: none; }
      .stat-card.clickable { transition: none; }
    }
  `],
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('riskChart') chartRef!: ElementRef<HTMLCanvasElement>;
  private chart: Chart | null = null;
  private refreshInterval: any;
  private destroy$ = new Subject<void>();

  constructor(public state: DashboardStateService, private ws: WebSocketService) {}

  ngOnInit(): void {
    this.state.load();
    this.refreshInterval = setInterval(() => this.state.load(), 60000);

    this.ws.onDashboard()
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.state.load());

    this.ws.onAlerts()
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.state.load());
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.buildChart(), 500);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.refreshInterval) clearInterval(this.refreshInterval);
    this.chart?.destroy();
  }

  getTrendLabel(trend: string): string {
    const labels: Record<string, string> = {
      IMPROVING: 'En amelioration', STABLE: 'Stable', DEGRADING: 'En degradation', CRITICAL: 'Critique',
    };
    return labels[trend] || trend;
  }

  getTrendIcon(trend: string): string {
    const icons: Record<string, string> = {
      IMPROVING: 'trending_down', STABLE: 'trending_flat', DEGRADING: 'trending_up', CRITICAL: 'priority_high',
    };
    return icons[trend] || 'trending_flat';
  }

  private buildChart(): void {
    const history = this.state.riskHistory();
    if (!history.length || !this.chartRef) return;

    const ctx = this.chartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    this.chart?.destroy();

    const labels = history.map(p => {
      const d = new Date(p.date);
      return `${d.getDate()}/${d.getMonth() + 1}`;
    });
    const data = history.map(p => p.score);

    this.chart = new Chart(ctx, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: 'Score de risque',
          data,
          borderColor: '#1976d2',
          backgroundColor: 'rgba(25, 118, 210, 0.1)',
          fill: true,
          tension: 0.3,
          pointRadius: 2,
          pointHoverRadius: 5,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: { mode: 'index', intersect: false },
        },
        scales: {
          y: {
            min: 0,
            max: 100,
            ticks: { stepSize: 25 },
            grid: { color: 'rgba(0,0,0,0.05)' },
          },
          x: {
            grid: { display: false },
            ticks: { maxTicksLimit: 10 },
          },
        },
      },
    });
  }
}
