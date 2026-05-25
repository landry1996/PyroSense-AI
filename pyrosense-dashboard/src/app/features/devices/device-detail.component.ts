import { Component, OnInit, OnDestroy, signal, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { Subject, takeUntil } from 'rxjs';
import { RiskGaugeComponent } from '../../shared/components/risk-gauge.component';
import { DeviceStatusBadgeComponent } from '../../shared/components/device-status-badge.component';
import { ApiService, DeviceResponse, TelemetryPoint, AnomalyResponse } from '../../core/services/api.service';
import Chart from 'chart.js/auto';

@Component({
  selector: 'app-device-detail',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatIconModule, MatButtonToggleModule,
    MatProgressSpinnerModule, MatTableModule,
    RiskGaugeComponent, DeviceStatusBadgeComponent,
  ],
  template: `
    <div class="device-detail-container">
      <div class="device-header">
        <h1>Capteur {{ device()?.serialNumber }}</h1>
        @if (device()) {
          <app-device-status-badge [status]="device()!.status" />
        }
      </div>

      @if (loading()) {
        <mat-spinner diameter="40" />
      } @else {
        <div class="device-info-cards">
          <mat-card>
            <mat-card-content>
              <app-risk-gauge [score]="riskScore()" />
              <div class="card-label">Score de risque</div>
            </mat-card-content>
          </mat-card>
          <mat-card>
            <mat-card-content>
              <mat-icon class="info-icon">memory</mat-icon>
              <div class="info-value">{{ device()?.firmwareVersion || '-' }}</div>
              <div class="card-label">Firmware</div>
            </mat-card-content>
          </mat-card>
          <mat-card>
            <mat-card-content>
              <mat-icon class="info-icon">wifi</mat-icon>
              <div class="info-value">{{ device()?.connectivityType }}</div>
              <div class="card-label">Connectivite</div>
            </mat-card-content>
          </mat-card>
          <mat-card>
            <mat-card-content>
              <mat-icon class="info-icon">schedule</mat-icon>
              <div class="info-value">{{ device()?.lastSeenAt | date:'dd/MM HH:mm' }}</div>
              <div class="card-label">Derniere communication</div>
            </mat-card-content>
          </mat-card>
        </div>

        <!-- Telemetry Charts -->
        <mat-card class="chart-card">
          <mat-card-header>
            <mat-card-title>Telemetrie</mat-card-title>
            <mat-button-toggle-group [value]="timeRange()" (change)="onTimeRangeChange($event.value)">
              <mat-button-toggle value="1h">1h</mat-button-toggle>
              <mat-button-toggle value="6h">6h</mat-button-toggle>
              <mat-button-toggle value="24h">24h</mat-button-toggle>
              <mat-button-toggle value="7d">7j</mat-button-toggle>
            </mat-button-toggle-group>
          </mat-card-header>
          <mat-card-content>
            <canvas #temperatureChart></canvas>
          </mat-card-content>
        </mat-card>

        <mat-card class="chart-card">
          <mat-card-header>
            <mat-card-title>Puissance & THD</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <canvas #powerChart></canvas>
          </mat-card-content>
        </mat-card>

        <!-- Anomalies Section -->
        <mat-card class="anomalies-card">
          <mat-card-header>
            <mat-card-title>Anomalies recentes</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            @if (anomalies().length === 0) {
              <div class="empty-anomalies">
                <mat-icon>check_circle</mat-icon>
                <p>Aucune anomalie detectee recemment</p>
              </div>
            } @else {
              <table mat-table [dataSource]="anomalies()" class="anomalies-table">
                <ng-container matColumnDef="severity">
                  <th mat-header-cell *matHeaderCellDef>Severite</th>
                  <td mat-cell *matCellDef="let a">
                    <mat-icon [class]="'anomaly-severity severity-' + a.severity.toLowerCase()">
                      {{ a.severity === 'CRITICAL' ? 'error' : a.severity === 'WARNING' ? 'warning' : 'info' }}
                    </mat-icon>
                  </td>
                </ng-container>
                <ng-container matColumnDef="type">
                  <th mat-header-cell *matHeaderCellDef>Type</th>
                  <td mat-cell *matCellDef="let a">
                    <span class="anomaly-type">{{ getAnomalyTypeLabel(a.type) }}</span>
                  </td>
                </ng-container>
                <ng-container matColumnDef="score">
                  <th mat-header-cell *matHeaderCellDef>Score</th>
                  <td mat-cell *matCellDef="let a">{{ a.score | number:'1.0-0' }}</td>
                </ng-container>
                <ng-container matColumnDef="detectedAt">
                  <th mat-header-cell *matHeaderCellDef>Date</th>
                  <td mat-cell *matCellDef="let a">{{ a.detectedAt | date:'dd/MM/yyyy HH:mm' }}</td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="anomalyColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: anomalyColumns;"></tr>
              </table>
            }
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .device-detail-container { max-width: 1100px; }
    .device-header { display: flex; align-items: center; gap: 16px; }
    .device-info-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin: 16px 0; }
    .device-info-cards mat-card-content { display: flex; flex-direction: column; align-items: center; padding: 24px; }
    .info-icon { font-size: 36px; width: 36px; height: 36px; color: #1976d2; }
    .info-value { font-size: 16px; font-weight: 500; margin: 8px 0; text-align: center; }
    .card-label { color: rgba(0,0,0,0.6); font-size: 12px; }
    .chart-card { margin-bottom: 16px; }
    .chart-card mat-card-header { display: flex; align-items: center; justify-content: space-between; }
    canvas { width: 100%; max-height: 280px; }
    .anomalies-card { margin-top: 16px; }
    .anomalies-table { width: 100%; }
    .anomaly-severity { font-size: 20px; }
    .severity-critical { color: #d32f2f; }
    .severity-warning { color: #f57c00; }
    .severity-info { color: #1976d2; }
    .anomaly-type { font-family: monospace; font-size: 12px; padding: 2px 6px; background: #f5f5f5; border-radius: 3px; }
    .empty-anomalies { text-align: center; padding: 24px; color: #388e3c; }
    .empty-anomalies mat-icon { font-size: 36px; width: 36px; height: 36px; }
    .empty-anomalies p { margin-top: 8px; color: #666; }

    @media (max-width: 600px) {
      .device-info-cards { grid-template-columns: repeat(2, 1fr); }
    }
  `],
})
export class DeviceDetailComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('temperatureChart') tempChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('powerChart') powerChartRef!: ElementRef<HTMLCanvasElement>;

  private destroy$ = new Subject<void>();
  private tempChart: Chart | null = null;
  private powerChart: Chart | null = null;
  private pollingInterval: any;
  private deviceId = '';

  loading = signal(true);
  device = signal<DeviceResponse | null>(null);
  riskScore = signal(0);
  timeRange = signal('24h');
  anomalies = signal<AnomalyResponse[]>([]);

  anomalyColumns = ['severity', 'type', 'score', 'detectedAt'];

  constructor(
    private route: ActivatedRoute,
    private api: ApiService,
  ) {}

  ngOnInit(): void {
    this.deviceId = this.route.snapshot.paramMap.get('id')!;
    this.loadDevice();
    this.loadAnomalies();
    this.pollingInterval = setInterval(() => this.loadTelemetry(), 30000);
  }

  ngAfterViewInit(): void {
    this.loadTelemetry();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.pollingInterval) clearInterval(this.pollingInterval);
    this.tempChart?.destroy();
    this.powerChart?.destroy();
  }

  onTimeRangeChange(range: string): void {
    this.timeRange.set(range);
    this.loadTelemetry();
  }

  getAnomalyTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      MICRO_ARC: 'Micro-arc', TEMPERATURE_SPIKE: 'Pic temperature',
      THD_DRIFT: 'Derive THD', Z_SCORE: 'Z-Score', HF_NOISE: 'Bruit HF',
      INSULATION_DEGRADATION: 'Degradation isolation', OVERLOAD: 'Surcharge',
    };
    return labels[type] || type;
  }

  private loadDevice(): void {
    this.api.getDevice(this.deviceId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (device) => { this.device.set(device); this.loading.set(false); },
        error: () => this.loading.set(false),
      });

    this.api.getDeviceRiskScore(this.deviceId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (risk) => this.riskScore.set(risk?.score ?? 0),
      });
  }

  private loadAnomalies(): void {
    this.api.getDeviceAnomalies(this.deviceId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({ next: (data) => this.anomalies.set(data.slice(0, 10)) });
  }

  private loadTelemetry(): void {
    if (!this.tempChartRef) return;

    const now = new Date();
    const hoursMap: Record<string, number> = { '1h': 1, '6h': 6, '24h': 24, '7d': 168 };
    const hours = hoursMap[this.timeRange()] || 24;
    const from = new Date(now.getTime() - hours * 3600000).toISOString();
    const to = now.toISOString();
    const granularity = hours <= 6 ? '1min' : hours <= 24 ? '15min' : '1hour';

    this.api.getDeviceTelemetry(this.deviceId, from, to, granularity)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (points) => this.renderCharts(points),
        error: () => {},
      });
  }

  private renderCharts(points: TelemetryPoint[]): void {
    if (!points.length) return;
    const labels = points.map(p => new Date(p.bucket).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }));

    this.tempChart?.destroy();
    this.tempChart = new Chart(this.tempChartRef.nativeElement, {
      type: 'line',
      data: {
        labels,
        datasets: [
          { label: 'Temperature (C)', data: points.map(p => p.avgTemperature), borderColor: '#f44336', tension: 0.3, fill: false },
          { label: 'Max Temperature', data: points.map(p => p.maxTemperature), borderColor: '#ff9800', borderDash: [5, 5], tension: 0.3, fill: false },
        ],
      },
      options: { responsive: true, plugins: { legend: { position: 'top' } }, scales: { y: { beginAtZero: false } } },
    });

    this.powerChart?.destroy();
    this.powerChart = new Chart(this.powerChartRef.nativeElement, {
      type: 'line',
      data: {
        labels,
        datasets: [
          { label: 'Puissance (W)', data: points.map(p => p.avgActivePower), borderColor: '#2196f3', tension: 0.3, yAxisID: 'y', fill: false },
          { label: 'THD (%)', data: points.map(p => p.avgThd), borderColor: '#9c27b0', tension: 0.3, yAxisID: 'y1', fill: false },
        ],
      },
      options: {
        responsive: true,
        plugins: { legend: { position: 'top' } },
        scales: {
          y: { type: 'linear', position: 'left', title: { display: true, text: 'Puissance (W)' } },
          y1: { type: 'linear', position: 'right', title: { display: true, text: 'THD (%)' }, grid: { drawOnChartArea: false } },
        },
      },
    });
  }
}
