import { Component, OnInit, OnDestroy, signal, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { Subject, takeUntil } from 'rxjs';
import { DeviceTechnicalApiService } from '../../core/services/device-technical-api.service';
import { TelemetryQualityReport, RejectionReason, OfflinePeriod } from '../../core/models/device-technical.model';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { SkeletonLoaderComponent } from '../../shared/components/skeleton-loader.component';
import Chart from 'chart.js/auto';

@Component({
  selector: 'app-device-telemetry-quality',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
    EmptyStateComponent,
    SkeletonLoaderComponent,
  ],
  template: `
    <div class="telemetry-quality-container">
      <h1>Qualite de la telemetrie</h1>

      @if (loading()) {
        <div class="loading-state">
          <mat-spinner diameter="40" />
          <p>Chargement du rapport qualite...</p>
        </div>
      } @else if (error()) {
        <div class="error-state">
          <mat-icon class="error-icon">error_outline</mat-icon>
          <h3>Erreur de chargement</h3>
          <p>{{ error() }}</p>
        </div>
      } @else if (report()) {
        <!-- Received vs Rejected per hour chart -->
        <mat-card class="chart-card">
          <mat-card-header>
            <mat-card-title>Telemetries recues vs rejetees par heure</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <canvas #barChart></canvas>
          </mat-card-content>
        </mat-card>

        <!-- Signal quality trend -->
        <mat-card class="chart-card">
          <mat-card-header>
            <mat-card-title>Tendance qualite du signal</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            @if (report()!.signalQualityTrend.length === 0) {
              <app-empty-state
                icon="show_chart"
                title="Aucune donnee de tendance"
                message="Les donnees de qualite du signal ne sont pas encore disponibles."
              />
            } @else {
              <canvas #lineChart></canvas>
            }
          </mat-card-content>
        </mat-card>

        <!-- Rejection reasons table -->
        <mat-card class="table-card">
          <mat-card-header>
            <mat-card-title>Raisons de rejet</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            @if (report()!.rejectionReasons.length === 0) {
              <app-empty-state
                icon="check_circle"
                title="Aucun rejet"
                message="Aucune telemetrie rejetee sur cette periode."
              />
            } @else {
              <table mat-table [dataSource]="report()!.rejectionReasons" class="full-width-table">
                <ng-container matColumnDef="reason">
                  <th mat-header-cell *matHeaderCellDef>Raison</th>
                  <td mat-cell *matCellDef="let row">{{ formatReason(row.reason) }}</td>
                </ng-container>
                <ng-container matColumnDef="count">
                  <th mat-header-cell *matHeaderCellDef>Nombre</th>
                  <td mat-cell *matCellDef="let row">
                    <span class="count-badge">{{ row.count }}</span>
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="rejectionColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: rejectionColumns;"></tr>
              </table>
            }
          </mat-card-content>
        </mat-card>

        <!-- Offline periods table -->
        <mat-card class="table-card">
          <mat-card-header>
            <mat-card-title>Periodes hors ligne</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            @if (report()!.offlinePeriods.length === 0) {
              <app-empty-state
                icon="wifi"
                title="Aucune interruption"
                message="Le capteur est reste connecte durant toute la periode."
              />
            } @else {
              <table mat-table [dataSource]="report()!.offlinePeriods" class="full-width-table">
                <ng-container matColumnDef="start">
                  <th mat-header-cell *matHeaderCellDef>Debut</th>
                  <td mat-cell *matCellDef="let row">{{ row.start | date:'dd/MM/yyyy HH:mm' }}</td>
                </ng-container>
                <ng-container matColumnDef="end">
                  <th mat-header-cell *matHeaderCellDef>Fin</th>
                  <td mat-cell *matCellDef="let row">{{ row.end | date:'dd/MM/yyyy HH:mm' }}</td>
                </ng-container>
                <ng-container matColumnDef="duration">
                  <th mat-header-cell *matHeaderCellDef>Duree</th>
                  <td mat-cell *matCellDef="let row">
                    <span class="duration-badge">{{ formatDuration(row.durationMinutes) }}</span>
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="offlineColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: offlineColumns;"></tr>
              </table>
            }
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .telemetry-quality-container { max-width: 1100px; padding: 16px; }
    h1 { margin: 0 0 24px; font-size: 24px; font-weight: 500; }
    .loading-state { display: flex; flex-direction: column; align-items: center; padding: 48px; gap: 16px; }
    .loading-state p { color: #757575; }
    .error-state { display: flex; flex-direction: column; align-items: center; padding: 48px; text-align: center; }
    .error-icon { font-size: 64px; width: 64px; height: 64px; color: #d32f2f; margin-bottom: 16px; }
    .error-state h3 { margin: 0 0 8px; color: #d32f2f; }
    .error-state p { color: #757575; }
    .chart-card { margin-bottom: 16px; }
    .chart-card canvas { width: 100%; max-height: 300px; }
    .table-card { margin-bottom: 16px; }
    .full-width-table { width: 100%; }
    .count-badge {
      display: inline-block;
      padding: 2px 10px;
      border-radius: 12px;
      background: #ffebee;
      color: #c62828;
      font-weight: 500;
      font-size: 13px;
    }
    .duration-badge {
      display: inline-block;
      padding: 2px 10px;
      border-radius: 12px;
      background: #fff3e0;
      color: #e65100;
      font-weight: 500;
      font-size: 13px;
    }

    @media (max-width: 600px) {
      .telemetry-quality-container { padding: 8px; }
    }
  `],
})
export class DeviceTelemetryQualityComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('barChart') barChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('lineChart') lineChartRef!: ElementRef<HTMLCanvasElement>;

  private destroy$ = new Subject<void>();
  private barChart: Chart | null = null;
  private lineChart: Chart | null = null;
  private deviceId = '';
  private chartsReady = false;

  loading = signal(true);
  error = signal<string | null>(null);
  report = signal<TelemetryQualityReport | null>(null);

  rejectionColumns = ['reason', 'count'];
  offlineColumns = ['start', 'end', 'duration'];

  constructor(
    private route: ActivatedRoute,
    private api: DeviceTechnicalApiService,
  ) {}

  ngOnInit(): void {
    this.deviceId = this.route.snapshot.paramMap.get('id')!;
    this.loadReport();
  }

  ngAfterViewInit(): void {
    this.chartsReady = true;
    if (this.report()) {
      this.renderCharts();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.barChart?.destroy();
    this.lineChart?.destroy();
  }

  private loadReport(): void {
    const now = new Date();
    const from = new Date(now.getTime() - 24 * 3600000).toISOString();
    const to = now.toISOString();

    this.api.getTelemetryQuality(this.deviceId, from, to)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.report.set(data);
          this.loading.set(false);
          if (this.chartsReady) {
            setTimeout(() => this.renderCharts(), 0);
          }
        },
        error: () => {
          this.error.set('Impossible de charger le rapport de qualite de la telemetrie.');
          this.loading.set(false);
        },
      });
  }

  private renderCharts(): void {
    const data = this.report();
    if (!data) return;

    // Bar chart: received vs rejected per hour
    if (this.barChartRef) {
      const labels = data.receivedPerHour.map(h =>
        new Date(h.hour).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
      );

      this.barChart?.destroy();
      this.barChart = new Chart(this.barChartRef.nativeElement, {
        type: 'bar',
        data: {
          labels,
          datasets: [
            {
              label: 'Recues',
              data: data.receivedPerHour.map(h => h.count),
              backgroundColor: '#4caf50',
            },
            {
              label: 'Rejetees',
              data: data.rejectedPerHour.map(h => h.count),
              backgroundColor: '#f44336',
            },
          ],
        },
        options: {
          responsive: true,
          plugins: { legend: { position: 'top' } },
          scales: {
            x: { title: { display: true, text: 'Heure' } },
            y: { beginAtZero: true, title: { display: true, text: 'Nombre' } },
          },
        },
      });
    }

    // Line chart: signal quality trend
    if (this.lineChartRef && data.signalQualityTrend.length > 0) {
      const trendLabels = data.signalQualityTrend.map(p =>
        new Date(p.timestamp).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
      );

      this.lineChart?.destroy();
      this.lineChart = new Chart(this.lineChartRef.nativeElement, {
        type: 'line',
        data: {
          labels: trendLabels,
          datasets: [
            {
              label: 'Qualite du signal (%)',
              data: data.signalQualityTrend.map(p => p.quality),
              borderColor: '#1976d2',
              backgroundColor: 'rgba(25, 118, 210, 0.1)',
              tension: 0.3,
              fill: true,
            },
          ],
        },
        options: {
          responsive: true,
          plugins: { legend: { position: 'top' } },
          scales: {
            y: { beginAtZero: true, max: 100, title: { display: true, text: 'Qualite (%)' } },
          },
        },
      });
    }
  }

  formatReason(reason: string): string {
    const labels: Record<string, string> = {
      INVALID_CHECKSUM: 'Checksum invalide',
      OUT_OF_RANGE: 'Valeur hors plage',
      DUPLICATE: 'Doublon',
      EXPIRED_TIMESTAMP: 'Horodatage expire',
      MALFORMED_PAYLOAD: 'Payload malformee',
      SEQUENCE_ERROR: 'Erreur de sequence',
      AUTHENTICATION_FAILED: 'Echec authentification',
    };
    return labels[reason] || reason;
  }

  formatDuration(minutes: number): string {
    if (minutes < 60) return `${minutes} min`;
    const hours = Math.floor(minutes / 60);
    const remainingMin = minutes % 60;
    if (hours < 24) return `${hours}h ${remainingMin}min`;
    const days = Math.floor(hours / 24);
    return `${days}j ${hours % 24}h`;
  }
}
