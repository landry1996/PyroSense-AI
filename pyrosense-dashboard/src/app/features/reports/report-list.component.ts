import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { Subject, takeUntil } from 'rxjs';
import { ApiService, ReportResponse } from '../../core/services/api.service';

@Component({
  selector: 'app-report-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatTableModule, MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule, MatCardModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatChipsModule,
  ],
  template: `
    <div class="reports-container">
      <h1>Rapports</h1>

      <!-- Filter -->
      <div class="filters-row">
        <mat-form-field appearance="outline">
          <mat-label>Type</mat-label>
          <mat-select [(value)]="typeFilter" (selectionChange)="loadReports()">
            <mat-option value="">Tous</mat-option>
            <mat-option value="MONTHLY_HEALTH">Mensuel</mat-option>
            <mat-option value="CONTINUOUS_MONITORING_CERTIFICATE">Certificat surveillance</mat-option>
            <mat-option value="CRITICAL_ALERT_REPORT">Alertes critiques</mat-option>
            <mat-option value="INTERVENTION_REPORT">Interventions</mat-option>
            <mat-option value="ROI_AVOIDED_INCIDENTS">ROI incidents evites</mat-option>
            <mat-option value="INSURER_EXPORT">Export assureur</mat-option>
          </mat-select>
        </mat-form-field>
      </div>

      @if (loading()) {
        <mat-spinner diameter="40"></mat-spinner>
      } @else {
        <table mat-table [dataSource]="reports()" class="reports-table">
          <ng-container matColumnDef="reportNumber">
            <th mat-header-cell *matHeaderCellDef>N. Rapport</th>
            <td mat-cell *matCellDef="let report">
              <span class="report-number">{{ report.reportNumber }}</span>
            </td>
          </ng-container>

          <ng-container matColumnDef="type">
            <th mat-header-cell *matHeaderCellDef>Type</th>
            <td mat-cell *matCellDef="let report">{{ getTypeLabel(report.type) }}</td>
          </ng-container>

          <ng-container matColumnDef="period">
            <th mat-header-cell *matHeaderCellDef>Periode</th>
            <td mat-cell *matCellDef="let report">
              {{ report.periodStart | date:'dd/MM/yyyy' }} - {{ report.periodEnd | date:'dd/MM/yyyy' }}
            </td>
          </ng-container>

          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Statut</th>
            <td mat-cell *matCellDef="let report">
              <span class="status-chip" [class]="'status-' + report.status.toLowerCase()">
                {{ getStatusLabel(report.status) }}
              </span>
            </td>
          </ng-container>

          <ng-container matColumnDef="createdAt">
            <th mat-header-cell *matHeaderCellDef>Cree le</th>
            <td mat-cell *matCellDef="let report">{{ report.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
          </ng-container>

          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>Actions</th>
            <td mat-cell *matCellDef="let report">
              @if (report.status === 'GENERATED') {
                <button mat-icon-button color="primary" (click)="downloadReport(report)">
                  <mat-icon>download</mat-icon>
                </button>
              }
              @if (report.status === 'GENERATING') {
                <mat-spinner diameter="20"></mat-spinner>
              }
              @if (report.status === 'FAILED') {
                <mat-icon class="failed-icon">error_outline</mat-icon>
              }
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>

        @if (reports().length === 0) {
          <mat-card class="empty-card">
            <mat-card-content>
              <mat-icon>description</mat-icon>
              <p>Aucun rapport disponible</p>
            </mat-card-content>
          </mat-card>
        }
      }
    </div>
  `,
  styles: [`
    .reports-container { max-width: 1100px; }
    .filters-row { margin-bottom: 16px; }
    .reports-table { width: 100%; }
    .report-number { font-family: monospace; font-weight: 500; color: #1565c0; }
    .status-chip { padding: 4px 8px; border-radius: 12px; font-size: 11px; font-weight: 500; }
    .status-pending { background: #fff3e0; color: #e65100; }
    .status-generating { background: #e3f2fd; color: #1565c0; }
    .status-generated { background: #e8f5e9; color: #2e7d32; }
    .status-failed { background: #ffcdd2; color: #b71c1c; }
    .failed-icon { color: #d32f2f; }
    .empty-card { text-align: center; padding: 32px; }
    .empty-card mat-icon { font-size: 48px; width: 48px; height: 48px; color: #bbb; }
    .empty-card p { color: #666; margin-top: 8px; }
  `],
})
export class ReportListComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  reports = signal<ReportResponse[]>([]);
  loading = signal(true);
  typeFilter = '';

  displayedColumns = ['reportNumber', 'type', 'period', 'status', 'createdAt', 'actions'];

  constructor(private api: ApiService, private snackBar: MatSnackBar) {}

  ngOnInit() { this.loadReports(); }

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  loadReports() {
    this.loading.set(true);
    this.api.getReports(0, 100, this.typeFilter || undefined)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => { this.reports.set(data); this.loading.set(false); },
        error: () => this.loading.set(false),
      });
  }

  downloadReport(report: ReportResponse) {
    this.api.getReportDownloadToken(report.id).subscribe({
      next: (tokenResp) => {
        const url = this.api.getReportDownloadUrl(report.id, tokenResp.token);
        window.open(url, '_blank');
      },
      error: () => this.snackBar.open('Erreur lors du telechargement', 'OK', { duration: 3000 }),
    });
  }

  getTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      MONTHLY_HEALTH: 'Mensuel',
      CONTINUOUS_MONITORING_CERTIFICATE: 'Certificat surveillance',
      CRITICAL_ALERT_REPORT: 'Alertes critiques',
      INTERVENTION_REPORT: 'Interventions',
      ROI_AVOIDED_INCIDENTS: 'ROI',
      INSURER_EXPORT: 'Export assureur',
    };
    return labels[type] || type;
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'En attente', GENERATING: 'Generation...', GENERATED: 'Disponible', FAILED: 'Echoue',
    };
    return labels[status] || status;
  }
}
