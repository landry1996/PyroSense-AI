import { Component, OnInit, OnDestroy, signal, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RiskGaugeComponent } from '../../shared/components/risk-gauge.component';
import { DeviceStatusBadgeComponent } from '../../shared/components/device-status-badge.component';
import { ApiService, DeviceResponse, TelemetryPoint } from '../../core/services/api.service';
import Chart from 'chart.js/auto';

@Component({
  selector: 'app-device-detail',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatIconModule, MatButtonToggleModule,
    MatProgressSpinnerModule, RiskGaugeComponent, DeviceStatusBadgeComponent,
  ],
  template: `
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
            <div class="info-value">{{ device()?.firmwareVersion }}</div>
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
            <div class="info-value">{{ device()?.lastSeenAt | date:'medium' }}</div>
            <div class="card-label">Derniere activite</div>
          </mat-card-content>
        </mat-card>
      </div>

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
    }
  `,
  styles: [`
    .device-header { display: flex; align-items: center; gap: 16px; }
    .device-info-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin: 16px 0; }
    .device-info-cards mat-card-content { display: flex; flex-direction: column; align-items: center; padding: 24px; }
    .info-icon { font-size: 36px; width: 36px; height: 36px; color: #1976d2; }
    .info-value { font-size: 18px; font-weight: 500; margin: 8px 0; }
    .card-label { color: rgba(0,0,0,0.6); font-size: 12px; }
    .chart-card { margin-bottom: 16px; }
    .chart-card mat-card-header { display: flex; align-items: center; justify-content: space-between; }
    canvas { width: 100%; max-height: 300px; }
  `],
})
export class DeviceDetailComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('temperatureChart') tempChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('powerChart') powerChartRef!: ElementRef<HTMLCanvasElement>;

  loading = signal(true);
  device = signal<DeviceResponse | null>(null);
  riskScore = signal(0);
  timeRange = signal('24h');

  private tempChart: Chart | null = null;
  private powerChart: Chart | null = null;
  private pollingInterval: any;
  private deviceId = '';

  constructor(
    private route: ActivatedRoute,
    private api: ApiService,
  ) {}

  ngOnInit(): void {
    this.deviceId = this.route.snapshot.paramMap.get('id')!;
    this.loadDevice();
    this.pollingInterval = setInterval(() => this.loadTelemetry(), 30000);
  }

  ngAfterViewInit(): void {
    this.loadTelemetry();
  }

  ngOnDestroy(): void {
    if (this.pollingInterval) clearInterval(this.pollingInterval);
    this.tempChart?.destroy();
    this.powerChart?.destroy();
  }

  onTimeRangeChange(range: string): void {
    this.timeRange.set(range);
    this.loadTelemetry();
  }

  private loadDevice(): void {
    this.api.getDevice(this.deviceId).subscribe({
      next: (device) => {
        this.device.set(device);
        this.loading.set(false);
      },
      error: () => {
        this.device.set({
          id: this.deviceId, serialNumber: 'PYR-001-A3F2', tenantId: 't1',
          buildingId: 'b1', panelId: 'p1', status: 'ACTIVE',
          firmwareVersion: '1.2.0', connectivityType: 'MQTT',
          lastSeenAt: new Date().toISOString(),
        });
        this.riskScore.set(34);
        this.loading.set(false);
      },
    });
  }

  private loadTelemetry(): void {
    if (!this.tempChartRef) return;

    const now = new Date();
    const hoursMap: Record<string, number> = { '1h': 1, '6h': 6, '24h': 24, '7d': 168 };
    const hours = hoursMap[this.timeRange()] || 24;
    const from = new Date(now.getTime() - hours * 3600000).toISOString();
    const to = now.toISOString();
    const granularity = hours <= 6 ? '1min' : hours <= 24 ? '15min' : '1hour';

    this.api.getDeviceTelemetry(this.deviceId, from, to, granularity).subscribe({
      next: (points) => this.renderCharts(points),
      error: () => this.renderCharts(this.generateMockTelemetry(hours)),
    });
  }

  private renderCharts(points: TelemetryPoint[]): void {
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

  private generateMockTelemetry(hours: number): TelemetryPoint[] {
    const points: TelemetryPoint[] = [];
    const now = Date.now();
    const step = hours <= 6 ? 60000 : hours <= 24 ? 900000 : 3600000;
    const count = Math.min(Math.floor(hours * 3600000 / step), 100);

    for (let i = count - 1; i >= 0; i--) {
      points.push({
        bucket: new Date(now - i * step).toISOString(),
        avgRmsCurrent: 12 + Math.random() * 3,
        avgRmsVoltage: 228 + Math.random() * 4,
        avgActivePower: 2700 + Math.random() * 500,
        avgPowerFactor: 0.92 + Math.random() * 0.06,
        avgThd: 3 + Math.random() * 2,
        avgTemperature: 35 + Math.random() * 10,
        avgHfNoise: 0.1 + Math.random() * 0.3,
        totalMicroArcs: Math.random() > 0.9 ? 1 : 0,
        totalTransients: Math.random() > 0.8 ? 1 : 0,
        maxTemperature: 40 + Math.random() * 10,
        sampleCount: 60,
      });
    }
    return points;
  }
}
