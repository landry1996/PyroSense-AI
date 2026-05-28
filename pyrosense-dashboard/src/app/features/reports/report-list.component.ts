import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { Subject, takeUntil } from 'rxjs';
import { ApiService, ReportResponse, BuildingResponse } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { SkeletonLoaderComponent } from '../../shared/components/skeleton-loader.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

@Component({
  selector: 'app-report-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatTableModule, MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule, MatInputModule,
    MatDatepickerModule, MatNativeDateModule,
    MatCardModule, MatProgressSpinnerModule, MatSnackBarModule, MatChipsModule,
    SkeletonLoaderComponent, EmptyStateComponent,
  ],
  template: `
    <div class="reports-container" role="main" aria-labelledby="reports-title">
      <h1 id="reports-title">Rapports</h1>

      <div class="actions-row">
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
        <button mat-raised-button color="primary" (click)="showGenerateForm = !showGenerateForm" aria-label="Generer un nouveau rapport">
          <mat-icon>add</mat-icon> Generer un rapport
        </button>
      </div>

      @if (showGenerateForm) {
        <mat-card class="generate-form">
          <mat-card-header>
            <mat-card-title>Generer un nouveau rapport</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="form-row">
              <mat-form-field appearance="outline">
                <mat-label>Type de rapport</mat-label>
                <mat-select [(value)]="genType">
                  <mat-option value="MONTHLY_HEALTH">Mensuel</mat-option>
                  <mat-option value="CRITICAL_ALERT_REPORT">Alertes critiques</mat-option>
                  <mat-option value="INTERVENTION_REPORT">Interventions</mat-option>
                  <mat-option value="ROI_AVOIDED_INCIDENTS">ROI</mat-option>
                </mat-select>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Batiment</mat-label>
                <mat-select [(value)]="genBuildingId">
                  @for (b of buildings(); track b.id) {
                    <mat-option [value]="b.id">{{ b.name }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Debut</mat-label>
                <input matInput [matDatepicker]="genFrom" [(ngModel)]="genFromDate">
                <mat-datepicker-toggle matIconSuffix [for]="genFrom"></mat-datepicker-toggle>
                <mat-datepicker #genFrom></mat-datepicker>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Fin</mat-label>
                <input matInput [matDatepicker]="genTo" [(ngModel)]="genToDate">
                <mat-datepicker-toggle matIconSuffix [for]="genTo"></mat-datepicker-toggle>
                <mat-datepicker #genTo></mat-datepicker>
              </mat-form-field>
            </div>
          </mat-card-content>
          <mat-card-actions align="end">
            <button mat-button (click)="showGenerateForm = false">Annuler</button>
            <button mat-flat-button color="primary" (click)="generateReport()"
              [disabled]="!genType || !genBuildingId || !genFromDate || !genToDate || generating()">
              @if (generating()) {
                <mat-spinner diameter="18" class="inline-spinner"></mat-spinner>
              } @else {
                Generer
              }
            </button>
          </mat-card-actions>
        </mat-card>
      }

      @if (loading()) {
        <app-skeleton type="table" [count]="6" [columns]="6" />
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
                <button mat-icon-button color="primary" (click)="downloadReport(report)" [attr.aria-label]="'Telecharger rapport ' + report.reportNumber">
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
          <app-empty-state icon="description" title="Aucun rapport disponible" message="Generez un rapport pour commencer le suivi de votre parc." />
        }
      }
    </div>
  `,
  styles: [`
    .reports-container { max-width: 1100px; }
    .actions-row { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 16px; }
    .filters-row { display: flex; gap: 12px; }
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
    .generate-form { margin-bottom: 24px; }
    .form-row { display: flex; gap: 12px; flex-wrap: wrap; padding: 16px 0; }
    .form-row mat-form-field { flex: 1; min-width: 180px; }
    .inline-spinner { display: inline-block; }

    @media (max-width: 960px) {
      .actions-row { flex-direction: column; gap: 12px; }
      .form-row { flex-direction: column; }
      .form-row mat-form-field { min-width: unset; }
    }
    @media (max-width: 600px) {
      .reports-table { font-size: 12px; }
      .actions-row { align-items: stretch; }
    }
  `],
})
export class ReportListComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  reports = signal<ReportResponse[]>([]);
  buildings = signal<BuildingResponse[]>([]);
  loading = signal(true);
  generating = signal(false);
  typeFilter = '';

  showGenerateForm = false;
  genType = '';
  genBuildingId = '';
  genFromDate: Date | null = null;
  genToDate: Date | null = null;

  displayedColumns = ['reportNumber', 'type', 'period', 'status', 'createdAt', 'actions'];

  constructor(private api: ApiService, private auth: AuthService, private snackBar: MatSnackBar) {}

  ngOnInit() {
    this.loadReports();
    this.loadBuildings();
  }

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

  generateReport() {
    if (!this.genType || !this.genBuildingId || !this.genFromDate || !this.genToDate) return;
    const tenantId = this.auth.currentUser()?.tenantId || '';
    this.generating.set(true);
    this.api.generateReport(
      tenantId, this.genBuildingId, this.genType,
      this.genFromDate.toISOString(), this.genToDate.toISOString()
    ).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.generating.set(false);
          this.showGenerateForm = false;
          this.snackBar.open('Rapport en cours de generation', 'OK', { duration: 3000 });
          setTimeout(() => this.loadReports(), 2000);
        },
        error: () => {
          this.generating.set(false);
          this.snackBar.open('Erreur lors de la generation', 'OK', { duration: 3000 });
        },
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

  private loadBuildings() {
    this.api.getBuildings().pipe(takeUntil(this.destroy$))
      .subscribe({ next: (data) => this.buildings.set(data) });
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
