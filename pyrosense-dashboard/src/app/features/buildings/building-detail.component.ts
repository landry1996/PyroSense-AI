import { Component, OnInit, OnDestroy, signal, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subject, takeUntil } from 'rxjs';
import { RiskGaugeComponent } from '../../shared/components/risk-gauge.component';
import { DeviceStatusBadgeComponent } from '../../shared/components/device-status-badge.component';
import { ApiService, DeviceResponse, BuildingResponse, RiskHistoryPoint, AlertDetailResponse, InterventionResponse } from '../../core/services/api.service';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-building-detail',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatCardModule, MatIconModule, MatTableModule, MatTabsModule,
    MatChipsModule, MatProgressSpinnerModule,
    RiskGaugeComponent, DeviceStatusBadgeComponent,
  ],
  template: `
    <div class="building-detail-container">
      @if (loading()) {
        <mat-spinner diameter="40" />
      } @else {
        <div class="header-row">
          <div class="header-info">
            <h1>{{ building()?.name || 'Batiment' }}</h1>
            <p class="address">{{ building()?.address }}</p>
          </div>
          <div class="header-stats">
            <div class="header-stat">
              <app-risk-gauge [score]="building()?.riskScore || 0" />
            </div>
            <div class="header-stat">
              <span class="status-badge" [class]="'status-' + (building()?.status || 'ok').toLowerCase()">
                {{ getStatusLabel(building()?.status || 'OK') }}
              </span>
            </div>
          </div>
        </div>

        <mat-tab-group (selectedTabChange)="onTabChange($event.index)">
          <!-- Capteurs Tab -->
          <mat-tab label="Capteurs ({{ devices().length }})">
            <div class="tab-content">
              <div class="summary-cards">
                <mat-card>
                  <mat-card-content>
                    <div class="big-number">{{ devices().length }}</div>
                    <div class="card-label">Total</div>
                  </mat-card-content>
                </mat-card>
                <mat-card>
                  <mat-card-content>
                    <div class="big-number active">{{ activeCount() }}</div>
                    <div class="card-label">Actifs</div>
                  </mat-card-content>
                </mat-card>
                <mat-card>
                  <mat-card-content>
                    <div class="big-number offline">{{ devices().length - activeCount() }}</div>
                    <div class="card-label">Hors-ligne</div>
                  </mat-card-content>
                </mat-card>
              </div>

              <table mat-table [dataSource]="devices()" class="full-width-table">
                <ng-container matColumnDef="serialNumber">
                  <th mat-header-cell *matHeaderCellDef>Serial</th>
                  <td mat-cell *matCellDef="let d">
                    <a [routerLink]="['/devices', d.id]">{{ d.serialNumber }}</a>
                  </td>
                </ng-container>
                <ng-container matColumnDef="status">
                  <th mat-header-cell *matHeaderCellDef>Statut</th>
                  <td mat-cell *matCellDef="let d"><app-device-status-badge [status]="d.status" /></td>
                </ng-container>
                <ng-container matColumnDef="connectivity">
                  <th mat-header-cell *matHeaderCellDef>Connectivite</th>
                  <td mat-cell *matCellDef="let d">{{ d.connectivityType }}</td>
                </ng-container>
                <ng-container matColumnDef="lastSeen">
                  <th mat-header-cell *matHeaderCellDef>Derniere activite</th>
                  <td mat-cell *matCellDef="let d">{{ d.lastSeenAt | date:'dd/MM/yyyy HH:mm' }}</td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="deviceColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: deviceColumns;"></tr>
              </table>
            </div>
          </mat-tab>

          <!-- Risque Tab -->
          <mat-tab label="Risque">
            <div class="tab-content">
              @if (riskHistory().length > 0) {
                <mat-card>
                  <mat-card-header>
                    <mat-card-title>Evolution du score de risque</mat-card-title>
                  </mat-card-header>
                  <mat-card-content>
                    <div class="chart-container">
                      <canvas #riskChart></canvas>
                    </div>
                  </mat-card-content>
                </mat-card>
              } @else {
                <p class="empty">Aucune donnee de risque disponible</p>
              }
            </div>
          </mat-tab>

          <!-- Alertes Tab -->
          <mat-tab label="Alertes ({{ alerts().length }})">
            <div class="tab-content">
              @if (alerts().length === 0) {
                <p class="empty">Aucune alerte pour ce batiment</p>
              } @else {
                <table mat-table [dataSource]="alerts()" class="full-width-table">
                  <ng-container matColumnDef="severity">
                    <th mat-header-cell *matHeaderCellDef>Severite</th>
                    <td mat-cell *matCellDef="let a">
                      <span class="severity-badge" [class]="'severity-' + a.severity.toLowerCase()">{{ a.severity }}</span>
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="title">
                    <th mat-header-cell *matHeaderCellDef>Titre</th>
                    <td mat-cell *matCellDef="let a">
                      <a [routerLink]="['/alerts', a.id]">{{ a.title }}</a>
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="status">
                    <th mat-header-cell *matHeaderCellDef>Statut</th>
                    <td mat-cell *matCellDef="let a">{{ a.status }}</td>
                  </ng-container>
                  <ng-container matColumnDef="createdAt">
                    <th mat-header-cell *matHeaderCellDef>Date</th>
                    <td mat-cell *matCellDef="let a">{{ a.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="alertColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: alertColumns;"></tr>
                </table>
              }
            </div>
          </mat-tab>

          <!-- Interventions Tab -->
          <mat-tab label="Interventions ({{ interventions().length }})">
            <div class="tab-content">
              @if (interventions().length === 0) {
                <p class="empty">Aucune intervention pour ce batiment</p>
              } @else {
                <table mat-table [dataSource]="interventions()" class="full-width-table">
                  <ng-container matColumnDef="type">
                    <th mat-header-cell *matHeaderCellDef>Type</th>
                    <td mat-cell *matCellDef="let i">{{ i.type }}</td>
                  </ng-container>
                  <ng-container matColumnDef="priority">
                    <th mat-header-cell *matHeaderCellDef>Priorite</th>
                    <td mat-cell *matCellDef="let i">
                      <span class="priority-badge" [class]="'priority-' + i.priority.toLowerCase()">{{ i.priority }}</span>
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="status">
                    <th mat-header-cell *matHeaderCellDef>Statut</th>
                    <td mat-cell *matCellDef="let i">{{ i.status }}</td>
                  </ng-container>
                  <ng-container matColumnDef="createdAt">
                    <th mat-header-cell *matHeaderCellDef>Date</th>
                    <td mat-cell *matCellDef="let i">{{ i.createdAt | date:'dd/MM/yyyy' }}</td>
                  </ng-container>
                  <ng-container matColumnDef="actions">
                    <th mat-header-cell *matHeaderCellDef></th>
                    <td mat-cell *matCellDef="let i">
                      <a [routerLink]="['/interventions', i.id]">Voir</a>
                    </td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="interventionColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: interventionColumns;"></tr>
                </table>
              }
            </div>
          </mat-tab>
        </mat-tab-group>
      }
    </div>
  `,
  styles: [`
    .building-detail-container { max-width: 1200px; }
    .header-row { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
    .header-info h1 { margin-bottom: 4px; }
    .address { color: #666; margin: 0; }
    .header-stats { display: flex; align-items: center; gap: 16px; }
    .header-stat { display: flex; align-items: center; }
    .status-badge { padding: 6px 12px; border-radius: 16px; font-size: 12px; font-weight: 600; text-transform: uppercase; }
    .status-ok { background: #e8f5e9; color: #2e7d32; }
    .status-watch { background: #fff8e1; color: #f9a825; }
    .status-at_risk { background: #fff3e0; color: #e65100; }
    .status-critical { background: #ffcdd2; color: #b71c1c; }
    .tab-content { padding: 24px 0; }
    .summary-cards { display: flex; gap: 16px; margin-bottom: 24px; }
    .summary-cards mat-card { flex: 1; text-align: center; }
    .summary-cards mat-card-content { padding: 20px; }
    .big-number { font-size: 36px; font-weight: 600; }
    .big-number.active { color: #388e3c; }
    .big-number.offline { color: #9e9e9e; }
    .card-label { color: rgba(0,0,0,0.6); margin-top: 4px; font-size: 12px; }
    .full-width-table { width: 100%; }
    .chart-container { position: relative; height: 250px; width: 100%; padding: 16px; }
    .empty { color: #666; font-style: italic; text-align: center; padding: 32px; }
    a { color: #1976d2; text-decoration: none; }
    a:hover { text-decoration: underline; }
    .severity-badge { padding: 3px 8px; border-radius: 10px; font-size: 10px; font-weight: 600; }
    .severity-critical { background: #ffcdd2; color: #b71c1c; }
    .severity-warning { background: #fff3e0; color: #e65100; }
    .severity-info { background: #e3f2fd; color: #1565c0; }
    .priority-badge { padding: 3px 8px; border-radius: 10px; font-size: 10px; font-weight: 600; }
    .priority-critical { background: #ffcdd2; color: #b71c1c; }
    .priority-high { background: #fff3e0; color: #e65100; }
    .priority-medium { background: #fff8e1; color: #f9a825; }
    .priority-low { background: #e8f5e9; color: #2e7d32; }

    @media (max-width: 600px) {
      .header-row { flex-direction: column; }
      .summary-cards { flex-direction: column; }
    }
  `],
})
export class BuildingDetailComponent implements OnInit, OnDestroy {
  @ViewChild('riskChart') chartRef!: ElementRef<HTMLCanvasElement>;

  private destroy$ = new Subject<void>();
  private chart: Chart | null = null;
  private pollingInterval: any;
  private buildingId = '';

  loading = signal(true);
  building = signal<BuildingResponse | null>(null);
  devices = signal<DeviceResponse[]>([]);
  activeCount = signal(0);
  riskHistory = signal<RiskHistoryPoint[]>([]);
  alerts = signal<AlertDetailResponse[]>([]);
  interventions = signal<InterventionResponse[]>([]);

  deviceColumns = ['serialNumber', 'status', 'connectivity', 'lastSeen'];
  alertColumns = ['severity', 'title', 'status', 'createdAt'];
  interventionColumns = ['type', 'priority', 'status', 'createdAt', 'actions'];

  constructor(
    private route: ActivatedRoute,
    private api: ApiService,
  ) {}

  ngOnInit(): void {
    this.buildingId = this.route.snapshot.paramMap.get('id')!;
    this.loadBuilding();
    this.loadDevices();
    this.pollingInterval = setInterval(() => this.loadDevices(), 30000);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.pollingInterval) clearInterval(this.pollingInterval);
    this.chart?.destroy();
  }

  onTabChange(index: number): void {
    if (index === 1 && this.riskHistory().length === 0) {
      this.loadRiskHistory();
    }
    if (index === 2 && this.alerts().length === 0) {
      this.loadAlerts();
    }
    if (index === 3 && this.interventions().length === 0) {
      this.loadInterventions();
    }
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = { OK: 'OK', WATCH: 'Surveillance', AT_RISK: 'A risque', CRITICAL: 'Critique' };
    return labels[status] || status;
  }

  private loadBuilding(): void {
    this.api.getBuildingById(this.buildingId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (b) => this.building.set(b),
        error: () => {},
      });
  }

  private loadDevices(): void {
    this.api.getDevicesByBuilding(this.buildingId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (devices) => {
          this.devices.set(devices);
          this.activeCount.set(devices.filter(d => d.status === 'ACTIVE').length);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  private loadRiskHistory(): void {
    this.api.getBuildingRiskHistory(this.buildingId, 30)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.riskHistory.set(data);
          setTimeout(() => this.buildChart(), 100);
        },
      });
  }

  private loadAlerts(): void {
    this.api.getAlertsByBuilding(this.buildingId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({ next: (data) => this.alerts.set(data) });
  }

  private loadInterventions(): void {
    this.api.getInterventionsByBuilding(this.buildingId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({ next: (data) => this.interventions.set(data) });
  }

  private buildChart(): void {
    const history = this.riskHistory();
    if (!history.length || !this.chartRef) return;

    const ctx = this.chartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    this.chart?.destroy();

    this.chart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: history.map(p => {
          const d = new Date(p.date);
          return `${d.getDate()}/${d.getMonth() + 1}`;
        }),
        datasets: [{
          label: 'Score de risque',
          data: history.map(p => p.score),
          borderColor: '#1976d2',
          backgroundColor: 'rgba(25, 118, 210, 0.1)',
          fill: true,
          tension: 0.3,
          pointRadius: 2,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          y: { min: 0, max: 100, ticks: { stepSize: 25 } },
          x: { ticks: { maxTicksLimit: 10 } },
        },
      },
    });
  }
}
