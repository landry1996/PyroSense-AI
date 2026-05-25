import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subject, takeUntil } from 'rxjs';
import { ApiService, AlertDetailResponse, AlertStatistics } from '../../core/services/api.service';

@Component({
  selector: 'app-alert-list',
  standalone: true,
  imports: [
    CommonModule, RouterModule, FormsModule,
    MatTableModule, MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule,
    MatPaginatorModule, MatCardModule, MatProgressSpinnerModule,
  ],
  template: `
    <div class="alerts-container">
      <h1>Alertes</h1>

      @if (statistics()) {
        <div class="stats-row">
          <mat-card class="stat-card critical">
            <mat-card-content>
              <div class="stat-value">{{ statistics()!.criticalOpen }}</div>
              <div class="stat-label">Critiques ouvertes</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card open">
            <mat-card-content>
              <div class="stat-value">{{ statistics()!.totalOpen }}</div>
              <div class="stat-label">Ouvertes</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card ack">
            <mat-card-content>
              <div class="stat-value">{{ statistics()!.totalAcknowledged }}</div>
              <div class="stat-label">Acquittees</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card progress">
            <mat-card-content>
              <div class="stat-value">{{ statistics()!.totalInProgress }}</div>
              <div class="stat-label">En cours</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card sla">
            <mat-card-content>
              <div class="stat-value">{{ statistics()!.slaBreached }}</div>
              <div class="stat-label">SLA depassees</div>
            </mat-card-content>
          </mat-card>
        </div>
      }

      <div class="filters-row">
        <mat-form-field appearance="outline">
          <mat-label>Statut</mat-label>
          <mat-select [(value)]="statusFilter" (selectionChange)="onFilterChange()">
            <mat-option value="">Tous</mat-option>
            <mat-option value="OPEN">Ouvert</mat-option>
            <mat-option value="ACKNOWLEDGED">Acquitte</mat-option>
            <mat-option value="IN_PROGRESS">En cours</mat-option>
            <mat-option value="RESOLVED">Resolu</mat-option>
            <mat-option value="FALSE_POSITIVE">Faux positif</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Severite</mat-label>
          <mat-select [(value)]="severityFilter" (selectionChange)="onFilterChange()">
            <mat-option value="">Toutes</mat-option>
            <mat-option value="CRITICAL">Critique</mat-option>
            <mat-option value="WARNING">Warning</mat-option>
            <mat-option value="INFO">Info</mat-option>
          </mat-select>
        </mat-form-field>
      </div>

      @if (loading()) {
        <mat-spinner diameter="40"></mat-spinner>
      } @else {
        <table mat-table [dataSource]="alerts()" class="alerts-table">
          <ng-container matColumnDef="severity">
            <th mat-header-cell *matHeaderCellDef>Severite</th>
            <td mat-cell *matCellDef="let alert">
              <span class="severity-badge" [class]="'severity-' + alert.severity.toLowerCase()">
                {{ getSeverityLabel(alert.severity) }}
              </span>
            </td>
          </ng-container>

          <ng-container matColumnDef="title">
            <th mat-header-cell *matHeaderCellDef>Titre</th>
            <td mat-cell *matCellDef="let alert">
              <a [routerLink]="['/alerts', alert.id]" class="alert-link">{{ alert.title }}</a>
            </td>
          </ng-container>

          <ng-container matColumnDef="type">
            <th mat-header-cell *matHeaderCellDef>Type</th>
            <td mat-cell *matCellDef="let alert">{{ getTypeLabel(alert.type) }}</td>
          </ng-container>

          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Statut</th>
            <td mat-cell *matCellDef="let alert">
              <span class="status-chip" [class]="'status-' + alert.status.toLowerCase()">
                {{ getStatusLabel(alert.status) }}
              </span>
            </td>
          </ng-container>

          <ng-container matColumnDef="createdAt">
            <th mat-header-cell *matHeaderCellDef>Date</th>
            <td mat-cell *matCellDef="let alert">{{ alert.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
          </ng-container>

          <ng-container matColumnDef="sla">
            <th mat-header-cell *matHeaderCellDef>SLA</th>
            <td mat-cell *matCellDef="let alert">
              @if (alert.slaBreached) {
                <mat-icon class="sla-breached">error</mat-icon>
              } @else {
                <mat-icon class="sla-ok">check_circle</mat-icon>
              }
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;" class="alert-row"></tr>
        </table>

        <mat-paginator
          [length]="totalAlerts()"
          [pageSize]="pageSize"
          [pageSizeOptions]="[25, 50, 100]"
          (page)="onPageChange($event)">
        </mat-paginator>
      }
    </div>
  `,
  styles: [`
    .alerts-container { max-width: 1200px; }
    .stats-row { display: flex; gap: 16px; margin-bottom: 24px; flex-wrap: wrap; }
    .stat-card { flex: 1; min-width: 150px; text-align: center; }
    .stat-value { font-size: 28px; font-weight: 700; }
    .stat-label { font-size: 12px; color: #666; margin-top: 4px; }
    .stat-card.critical .stat-value { color: #d32f2f; }
    .stat-card.open .stat-value { color: #f57c00; }
    .stat-card.ack .stat-value { color: #1976d2; }
    .stat-card.progress .stat-value { color: #7b1fa2; }
    .stat-card.sla .stat-value { color: #c62828; }
    .filters-row { display: flex; gap: 16px; margin-bottom: 16px; }
    .alerts-table { width: 100%; }
    .alert-link { color: #1976d2; text-decoration: none; font-weight: 500; }
    .alert-link:hover { text-decoration: underline; }
    .alert-row:hover { background: #f5f5f5; cursor: pointer; }
    .severity-badge {
      padding: 4px 8px; border-radius: 4px; font-size: 11px;
      font-weight: 600; text-transform: uppercase;
    }
    .severity-critical { background: #ffcdd2; color: #b71c1c; }
    .severity-warning { background: #fff3e0; color: #e65100; }
    .severity-info { background: #e3f2fd; color: #1565c0; }
    .status-chip {
      padding: 4px 8px; border-radius: 12px; font-size: 11px; font-weight: 500;
    }
    .status-open { background: #fff3e0; color: #e65100; }
    .status-acknowledged { background: #e3f2fd; color: #1565c0; }
    .status-in_progress { background: #f3e5f5; color: #7b1fa2; }
    .status-resolved { background: #e8f5e9; color: #2e7d32; }
    .status-false_positive { background: #fafafa; color: #616161; }
    .sla-breached { color: #d32f2f; }
    .sla-ok { color: #4caf50; }
  `],
})
export class AlertListComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  alerts = signal<AlertDetailResponse[]>([]);
  statistics = signal<AlertStatistics | null>(null);
  loading = signal(true);
  totalAlerts = signal(0);

  statusFilter = '';
  severityFilter = '';
  pageSize = 50;
  currentPage = 0;

  displayedColumns = ['severity', 'title', 'type', 'status', 'createdAt', 'sla'];

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.loadStatistics();
    this.loadAlerts();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onFilterChange() {
    this.currentPage = 0;
    this.loadAlerts();
  }

  onPageChange(event: PageEvent) {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadAlerts();
  }

  private loadAlerts() {
    this.loading.set(true);
    this.api.getAlertsList(this.currentPage, this.pageSize, this.statusFilter || undefined, this.severityFilter || undefined)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (alerts) => {
          this.alerts.set(alerts);
          this.totalAlerts.set(
            alerts.length < this.pageSize
              ? this.currentPage * this.pageSize + alerts.length
              : (this.currentPage + 2) * this.pageSize
          );
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  private loadStatistics() {
    this.api.getAlertStatistics()
      .pipe(takeUntil(this.destroy$))
      .subscribe({ next: (stats) => this.statistics.set(stats) });
  }

  getSeverityLabel(severity: string): string {
    const labels: Record<string, string> = { CRITICAL: 'Critique', WARNING: 'Warning', INFO: 'Info' };
    return labels[severity] || severity;
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      OPEN: 'Ouvert', ACKNOWLEDGED: 'Acquitte', IN_PROGRESS: 'En cours',
      RESOLVED: 'Resolu', FALSE_POSITIVE: 'Faux positif',
    };
    return labels[status] || status;
  }

  getTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      MICRO_ARC_DETECTED: 'Arc electrique',
      INSULATION_DEGRADATION: 'Isolation',
      LOOSE_CONNECTION: 'Connexion lache',
      OVERHEATING: 'Surchauffe',
      ABNORMAL_TRANSIENT: 'Transitoire',
      HARMONIC_DISTORTION: 'Harmonique',
      LOAD_IMBALANCE: 'Desequilibre',
      SENSOR_OFFLINE: 'Capteur offline',
      CRITICAL_RISK_SCORE: 'Risque critique',
      HIGH_RISK_SCORE: 'Risque eleve',
      BASELINE_DEVIATION: 'Deviation baseline',
    };
    return labels[type] || type;
  }
}
