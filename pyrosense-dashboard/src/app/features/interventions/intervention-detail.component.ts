import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatStepperModule } from '@angular/material/stepper';
import { ApiService, InterventionResponse } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-intervention-detail',
  standalone: true,
  imports: [
    CommonModule, RouterModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatDividerModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatProgressSpinnerModule, MatSnackBarModule,
    MatStepperModule,
  ],
  template: `
    @if (loading()) {
      <mat-spinner diameter="40"></mat-spinner>
    } @else if (intervention()) {
      <div class="detail-container">
        <div class="header">
          <button mat-icon-button routerLink="/interventions">
            <mat-icon>arrow_back</mat-icon>
          </button>
          <h1>Intervention</h1>
          <span class="priority-badge" [class]="'priority-' + intervention()!.priority.toLowerCase()">
            {{ getPriorityLabel(intervention()!.priority) }}
          </span>
          <span class="status-badge" [class]="'status-' + intervention()!.status.toLowerCase()">
            {{ getStatusLabel(intervention()!.status) }}
          </span>
        </div>

        <div class="detail-grid">
          <!-- Info Card -->
          <mat-card>
            <mat-card-header><mat-card-title>Informations</mat-card-title></mat-card-header>
            <mat-card-content>
              <div class="info-row"><span class="label">Type</span><span>{{ getTypeLabel(intervention()!.type) }}</span></div>
              <div class="info-row"><span class="label">Appareil</span>
                <a [routerLink]="['/devices', intervention()!.deviceId]">{{ intervention()!.deviceId | slice:0:8 }}...</a>
              </div>
              <div class="info-row"><span class="label">Alerte source</span>
                <a [routerLink]="['/alerts', intervention()!.alertId]">{{ intervention()!.alertId | slice:0:8 }}...</a>
              </div>
              @if (intervention()!.assignedElectricianId) {
                <div class="info-row"><span class="label">Electricien</span><span>{{ intervention()!.assignedElectricianId | slice:0:8 }}</span></div>
              }
              @if (intervention()!.scheduledAt) {
                <div class="info-row"><span class="label">Planifiee</span><span>{{ intervention()!.scheduledAt | date:'dd/MM/yyyy HH:mm' }}</span></div>
              }
              @if (intervention()!.startedAt) {
                <div class="info-row"><span class="label">Demarree</span><span>{{ intervention()!.startedAt | date:'dd/MM/yyyy HH:mm' }}</span></div>
              }
              @if (intervention()!.completedAt) {
                <div class="info-row"><span class="label">Terminee</span><span>{{ intervention()!.completedAt | date:'dd/MM/yyyy HH:mm' }}</span></div>
              }
              @if (intervention()!.result) {
                <div class="info-row"><span class="label">Resultat</span><span>{{ getResultLabel(intervention()!.result!) }}</span></div>
              }
              <div class="info-row"><span class="label">Cree le</span><span>{{ intervention()!.createdAt | date:'dd/MM/yyyy HH:mm' }}</span></div>
            </mat-card-content>
          </mat-card>

          <!-- Description + Actions -->
          <mat-card>
            <mat-card-header><mat-card-title>Description</mat-card-title></mat-card-header>
            <mat-card-content>
              <p class="description">{{ intervention()!.description }}</p>

              <mat-divider></mat-divider>

              @if (!isTerminal()) {
                <div class="actions">
                  <h3>Actions</h3>
                  @switch (intervention()!.status) {
                    @case ('CREATED') {
                      <button mat-raised-button color="primary" (click)="startIntervention()">
                        <mat-icon>play_arrow</mat-icon> Demarrer
                      </button>
                      <button mat-stroked-button color="warn" (click)="cancelIntervention()">
                        <mat-icon>cancel</mat-icon> Annuler
                      </button>
                    }
                    @case ('PLANNED') {
                      <button mat-raised-button color="primary" (click)="startIntervention()">
                        <mat-icon>play_arrow</mat-icon> Demarrer
                      </button>
                      <button mat-stroked-button color="warn" (click)="cancelIntervention()">
                        <mat-icon>cancel</mat-icon> Annuler
                      </button>
                    }
                    @case ('ASSIGNED') {
                      <button mat-raised-button color="primary" (click)="startIntervention()">
                        <mat-icon>play_arrow</mat-icon> Demarrer
                      </button>
                      <button mat-stroked-button color="warn" (click)="cancelIntervention()">
                        <mat-icon>cancel</mat-icon> Annuler
                      </button>
                    }
                    @case ('IN_PROGRESS') {
                      @if (!showDiagnosticForm && !intervention()!.diagnostic) {
                        <button mat-raised-button (click)="showDiagnosticForm = true">
                          <mat-icon>assignment</mat-icon> Saisir diagnostic
                        </button>
                      }
                      @if (!showCompleteForm) {
                        <button mat-raised-button color="accent" (click)="showCompleteForm = true">
                          <mat-icon>done_all</mat-icon> Terminer
                        </button>
                      }
                      <button mat-stroked-button color="warn" (click)="cancelIntervention()">
                        <mat-icon>cancel</mat-icon> Annuler
                      </button>
                    }
                  }
                </div>
              }

              <!-- Diagnostic Form -->
              @if (showDiagnosticForm) {
                <div class="form-section">
                  <h4>Diagnostic terrain</h4>
                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Observations</mat-label>
                    <textarea matInput [(ngModel)]="diagnosticObservations" rows="3" required></textarea>
                  </mat-form-field>
                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Mesures effectuees</mat-label>
                    <textarea matInput [(ngModel)]="diagnosticMeasurements" rows="2"></textarea>
                  </mat-form-field>
                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Recommandations</mat-label>
                    <textarea matInput [(ngModel)]="diagnosticRecommendations" rows="2"></textarea>
                  </mat-form-field>
                  <div class="form-actions">
                    <button mat-raised-button color="primary" (click)="submitDiagnostic()"
                            [disabled]="!diagnosticObservations.trim()">Enregistrer</button>
                    <button mat-button (click)="showDiagnosticForm = false">Annuler</button>
                  </div>
                </div>
              }

              <!-- Complete Form -->
              @if (showCompleteForm) {
                <div class="form-section">
                  <h4>Resultat de l'intervention</h4>
                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Resultat</mat-label>
                    <mat-select [(value)]="completionResult">
                      <mat-option value="CONFIRMED_DEFECT">Defaut confirme</mat-option>
                      <mat-option value="REPAIRED">Repare</mat-option>
                      <mat-option value="REPLACED_COMPONENT">Composant remplace</mat-option>
                      <mat-option value="NO_DEFECT_FOUND">Aucun defaut trouve</mat-option>
                      <mat-option value="NEEDS_FOLLOW_UP">Suivi necessaire</mat-option>
                    </mat-select>
                  </mat-form-field>
                  <div class="form-actions">
                    <button mat-raised-button color="primary" (click)="submitComplete()"
                            [disabled]="!completionResult">Terminer</button>
                    <button mat-button (click)="showCompleteForm = false">Annuler</button>
                  </div>
                </div>
              }
            </mat-card-content>
          </mat-card>

          <!-- Diagnostic Display -->
          @if (intervention()!.diagnostic) {
            <mat-card>
              <mat-card-header><mat-card-title>Diagnostic terrain</mat-card-title></mat-card-header>
              <mat-card-content>
                <div class="info-row"><span class="label">Observations</span><span>{{ intervention()!.diagnostic!.observations }}</span></div>
                @if (intervention()!.diagnostic!.measurementsTaken) {
                  <div class="info-row"><span class="label">Mesures</span><span>{{ intervention()!.diagnostic!.measurementsTaken }}</span></div>
                }
                @if (intervention()!.diagnostic!.recommendations) {
                  <div class="info-row"><span class="label">Recommandations</span><span>{{ intervention()!.diagnostic!.recommendations }}</span></div>
                }
                <div class="info-row"><span class="label">Par</span><span>{{ intervention()!.diagnostic!.diagnosticBy | slice:0:8 }}</span></div>
                <div class="info-row"><span class="label">Date</span><span>{{ intervention()!.diagnostic!.recordedAt | date:'dd/MM/yyyy HH:mm' }}</span></div>
              </mat-card-content>
            </mat-card>
          }

          <!-- Risk Impact -->
          @if (intervention()!.riskImpact) {
            <mat-card>
              <mat-card-header><mat-card-title>Impact risque</mat-card-title></mat-card-header>
              <mat-card-content>
                <div class="risk-display">
                  <div class="risk-before">
                    <span class="risk-value">{{ intervention()!.riskImpact!.riskScoreBefore }}</span>
                    <span class="risk-label">Avant</span>
                  </div>
                  <mat-icon class="arrow">trending_down</mat-icon>
                  <div class="risk-after">
                    <span class="risk-value">{{ intervention()!.riskImpact!.riskScoreAfter }}</span>
                    <span class="risk-label">Apres</span>
                  </div>
                  <div class="risk-reduction">
                    <span class="reduction-value">-{{ intervention()!.riskImpact!.riskReduction }}%</span>
                    <span class="risk-label">Reduction</span>
                  </div>
                </div>
              </mat-card-content>
            </mat-card>
          }
        </div>
      </div>
    } @else {
      <p>Intervention introuvable.</p>
    }
  `,
  styles: [`
    .header { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
    .header h1 { margin: 0; flex: 1; }
    .detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
    .info-row { display: flex; justify-content: space-between; align-items: flex-start; padding: 8px 0; border-bottom: 1px solid #eee; }
    .label { font-weight: 500; color: #555; min-width: 120px; }
    .description { color: #333; line-height: 1.6; margin: 16px 0; }
    .actions { margin-top: 16px; display: flex; gap: 12px; flex-wrap: wrap; }
    .form-section { margin-top: 16px; padding-top: 16px; border-top: 1px solid #eee; }
    .full-width { width: 100%; }
    .form-actions { display: flex; gap: 8px; }

    .priority-badge { padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; text-transform: uppercase; }
    .priority-urgent { background: #ffcdd2; color: #b71c1c; }
    .priority-high { background: #fff3e0; color: #e65100; }
    .priority-medium { background: #e3f2fd; color: #1565c0; }
    .priority-low { background: #f5f5f5; color: #616161; }

    .status-badge { padding: 4px 8px; border-radius: 12px; font-size: 11px; font-weight: 500; }
    .status-created { background: #e3f2fd; color: #1565c0; }
    .status-planned { background: #fff3e0; color: #e65100; }
    .status-assigned { background: #f3e5f5; color: #7b1fa2; }
    .status-in_progress { background: #e8f5e9; color: #2e7d32; }
    .status-completed { background: #e0e0e0; color: #424242; }
    .status-cancelled { background: #fafafa; color: #999; }

    .risk-display { display: flex; align-items: center; gap: 24px; justify-content: center; padding: 16px; }
    .risk-before, .risk-after, .risk-reduction { text-align: center; }
    .risk-value { font-size: 32px; font-weight: 700; display: block; }
    .risk-before .risk-value { color: #d32f2f; }
    .risk-after .risk-value { color: #2e7d32; }
    .reduction-value { font-size: 24px; font-weight: 700; color: #1976d2; display: block; }
    .risk-label { font-size: 11px; color: #666; }
    .arrow { font-size: 32px; width: 32px; height: 32px; color: #4caf50; }

    @media (max-width: 768px) { .detail-grid { grid-template-columns: 1fr; } }
  `],
})
export class InterventionDetailComponent implements OnInit {
  intervention = signal<InterventionResponse | null>(null);
  loading = signal(true);

  showDiagnosticForm = false;
  showCompleteForm = false;
  diagnosticObservations = '';
  diagnosticMeasurements = '';
  diagnosticRecommendations = '';
  completionResult = '';

  private interventionId = '';

  constructor(
    private route: ActivatedRoute,
    private api: ApiService,
    private auth: AuthService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit() {
    this.interventionId = this.route.snapshot.paramMap.get('id') || '';
    this.loadIntervention();
  }

  isTerminal(): boolean {
    const s = this.intervention()?.status;
    return s === 'COMPLETED' || s === 'CANCELLED';
  }

  startIntervention() {
    this.api.startIntervention(this.interventionId).subscribe({
      next: (updated) => { this.intervention.set(updated); this.snackBar.open('Intervention demarree', 'OK', { duration: 3000 }); },
      error: () => this.snackBar.open('Erreur', 'OK', { duration: 3000 }),
    });
  }

  cancelIntervention() {
    this.api.cancelIntervention(this.interventionId).subscribe({
      next: (updated) => { this.intervention.set(updated); this.snackBar.open('Intervention annulee', 'OK', { duration: 3000 }); },
      error: () => this.snackBar.open('Erreur', 'OK', { duration: 3000 }),
    });
  }

  submitDiagnostic() {
    this.api.addDiagnostic(
      this.interventionId,
      this.diagnosticObservations,
      this.auth.getUserId(),
      this.diagnosticMeasurements || undefined,
      this.diagnosticRecommendations || undefined,
    ).subscribe({
      next: (updated) => {
        this.intervention.set(updated);
        this.showDiagnosticForm = false;
        this.snackBar.open('Diagnostic enregistre', 'OK', { duration: 3000 });
      },
      error: () => this.snackBar.open('Erreur', 'OK', { duration: 3000 }),
    });
  }

  submitComplete() {
    this.api.completeIntervention(this.interventionId, this.completionResult).subscribe({
      next: (updated) => {
        this.intervention.set(updated);
        this.showCompleteForm = false;
        this.snackBar.open('Intervention terminee', 'OK', { duration: 3000 });
      },
      error: () => this.snackBar.open('Erreur', 'OK', { duration: 3000 }),
    });
  }

  private loadIntervention() {
    this.api.getInterventionById(this.interventionId).subscribe({
      next: (data) => { this.intervention.set(data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  getPriorityLabel(p: string): string {
    const labels: Record<string, string> = { URGENT: 'Urgent', HIGH: 'Haute', MEDIUM: 'Moyenne', LOW: 'Basse' };
    return labels[p] || p;
  }

  getStatusLabel(s: string): string {
    const labels: Record<string, string> = {
      CREATED: 'Creee', PLANNED: 'Planifiee', ASSIGNED: 'Assignee',
      IN_PROGRESS: 'En cours', COMPLETED: 'Terminee', CANCELLED: 'Annulee',
    };
    return labels[s] || s;
  }

  getTypeLabel(t: string): string {
    const labels: Record<string, string> = {
      PREVENTIVE: 'Preventive', CORRECTIVE: 'Corrective',
      PREDICTIVE: 'Predictive', EMERGENCY: 'Urgence',
    };
    return labels[t] || t;
  }

  getResultLabel(r: string): string {
    const labels: Record<string, string> = {
      CONFIRMED_DEFECT: 'Defaut confirme', REPAIRED: 'Repare',
      REPLACED_COMPONENT: 'Composant remplace', NO_DEFECT_FOUND: 'Aucun defaut',
      NEEDS_FOLLOW_UP: 'Suivi necessaire',
    };
    return labels[r] || r;
  }
}
