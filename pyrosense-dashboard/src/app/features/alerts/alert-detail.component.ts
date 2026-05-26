import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { HttpClient } from '@angular/common/http';
import { ApiService, AlertDetailResponse } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-alert-detail',
  standalone: true,
  imports: [
    CommonModule, RouterModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatChipsModule, MatDividerModule, MatFormFieldModule,
    MatInputModule, MatDialogModule, MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  template: `
    @if (loading()) {
      <mat-spinner diameter="40"></mat-spinner>
    } @else if (alert()) {
      <div class="alert-detail">
        <div class="header">
          <button mat-icon-button routerLink="/alerts">
            <mat-icon>arrow_back</mat-icon>
          </button>
          <h1>{{ alert()!.title }}</h1>
        </div>

        <div class="detail-grid">
          <!-- Main info card -->
          <mat-card class="info-card">
            <mat-card-header>
              <mat-card-title>Informations</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <div class="info-row">
                <span class="label">Severite</span>
                <span class="severity-badge" [class]="'severity-' + alert()!.severity.toLowerCase()">
                  {{ getSeverityLabel(alert()!.severity) }}
                </span>
              </div>
              <div class="info-row">
                <span class="label">Statut</span>
                <span class="status-chip" [class]="'status-' + alert()!.status.toLowerCase()">
                  {{ getStatusLabel(alert()!.status) }}
                </span>
              </div>
              <div class="info-row">
                <span class="label">Type</span>
                <span>{{ getTypeLabel(alert()!.type) }}</span>
              </div>
              <div class="info-row">
                <span class="label">Appareil</span>
                <a [routerLink]="['/devices', alert()!.deviceId]">{{ alert()!.deviceId | slice:0:8 }}...</a>
              </div>
              <div class="info-row">
                <span class="label">Cree le</span>
                <span>{{ alert()!.createdAt | date:'dd/MM/yyyy HH:mm:ss' }}</span>
              </div>
              <div class="info-row">
                <span class="label">SLA</span>
                <span>
                  {{ alert()!.slaDeadline | date:'dd/MM/yyyy HH:mm' }}
                  @if (alert()!.slaBreached) {
                    <mat-icon class="sla-breached inline-icon">error</mat-icon>
                    <span class="sla-text">Depassee</span>
                  }
                </span>
              </div>
              <div class="info-row">
                <span class="label">Escalade</span>
                <span>{{ alert()!.escalationLevel }}</span>
              </div>
              <div class="info-row">
                <span class="label">Occurrences</span>
                <span>{{ alert()!.occurrenceCount }}</span>
              </div>
              @if (alert()!.acknowledgedAt) {
                <div class="info-row">
                  <span class="label">Acquitte le</span>
                  <span>{{ alert()!.acknowledgedAt | date:'dd/MM/yyyy HH:mm' }}</span>
                </div>
              }
              @if (alert()!.resolvedAt) {
                <div class="info-row">
                  <span class="label">Resolu le</span>
                  <span>{{ alert()!.resolvedAt | date:'dd/MM/yyyy HH:mm' }}</span>
                </div>
                <div class="info-row">
                  <span class="label">Note</span>
                  <span>{{ alert()!.resolutionNote }}</span>
                </div>
              }
            </mat-card-content>
          </mat-card>

          <!-- Actions card -->
          <mat-card class="actions-card">
            <mat-card-header>
              <mat-card-title>Actions</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <p class="description">{{ alert()!.description }}</p>

              <mat-divider></mat-divider>

              @if (!isTerminal()) {
                <div class="action-buttons">
                  @if (alert()!.status === 'OPEN') {
                    <button mat-raised-button color="primary" (click)="acknowledge()">
                      <mat-icon>check</mat-icon> Acquitter
                    </button>
                  }
                  @if (alert()!.status === 'OPEN' || alert()!.status === 'ACKNOWLEDGED') {
                    <button mat-stroked-button color="primary" (click)="showAssignForm = true">
                      <mat-icon>person_add</mat-icon> Assigner
                    </button>
                  }
                  @if (alert()!.status !== 'RESOLVED' && alert()!.status !== 'FALSE_POSITIVE') {
                    <button mat-raised-button color="accent" (click)="showResolveForm = true">
                      <mat-icon>done_all</mat-icon> Resoudre
                    </button>
                    <button mat-stroked-button (click)="showFalsePositiveForm = true">
                      <mat-icon>cancel</mat-icon> Faux positif
                    </button>
                    <button mat-raised-button color="warn" (click)="createIntervention()">
                      <mat-icon>build</mat-icon> Creer intervention
                    </button>
                  }
                </div>

                @if (showResolveForm) {
                  <div class="action-form">
                    <mat-form-field appearance="outline" class="full-width">
                      <mat-label>Note de resolution</mat-label>
                      <textarea matInput [(ngModel)]="resolutionNote" rows="3"
                                placeholder="Decrivez la resolution..."></textarea>
                    </mat-form-field>
                    <div class="form-actions">
                      <button mat-raised-button color="primary" (click)="resolve()"
                              [disabled]="!resolutionNote.trim()">Confirmer</button>
                      <button mat-button (click)="showResolveForm = false">Annuler</button>
                    </div>
                  </div>
                }

                @if (showFalsePositiveForm) {
                  <div class="action-form">
                    <mat-form-field appearance="outline" class="full-width">
                      <mat-label>Raison du faux positif</mat-label>
                      <textarea matInput [(ngModel)]="falsePositiveReason" rows="3"
                                placeholder="Expliquez pourquoi c'est un faux positif..."></textarea>
                    </mat-form-field>
                    <div class="form-actions">
                      <button mat-raised-button color="warn" (click)="markFalsePositive()"
                              [disabled]="!falsePositiveReason.trim()">Confirmer</button>
                      <button mat-button (click)="showFalsePositiveForm = false">Annuler</button>
                    </div>
                  </div>
                }

                @if (showAssignForm) {
                  <div class="action-form">
                    <mat-form-field appearance="outline" class="full-width">
                      <mat-label>Utilisateur a assigner</mat-label>
                      <input matInput [(ngModel)]="assignUserId" placeholder="ID ou email de l'utilisateur">
                    </mat-form-field>
                    <div class="form-actions">
                      <button mat-raised-button color="primary" (click)="assign()"
                              [disabled]="!assignUserId.trim()">Assigner</button>
                      <button mat-button (click)="showAssignForm = false">Annuler</button>
                    </div>
                  </div>
                }
              }
            </mat-card-content>
          </mat-card>

          <!-- Comments card -->
          <mat-card class="comments-card">
            <mat-card-header>
              <mat-card-title>Commentaires ({{ alert()!.comments.length }})</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              @for (comment of alert()!.comments; track comment.id) {
                <div class="comment">
                  <div class="comment-header">
                    <mat-icon>person</mat-icon>
                    <span class="comment-author">{{ comment.authorId | slice:0:8 }}</span>
                    <span class="comment-date">{{ comment.createdAt | date:'dd/MM HH:mm' }}</span>
                  </div>
                  <p class="comment-content">{{ comment.content }}</p>
                </div>
              } @empty {
                <p class="no-comments">Aucun commentaire</p>
              }

              @if (!isTerminal()) {
                <mat-divider></mat-divider>
                <div class="add-comment">
                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Ajouter un commentaire</mat-label>
                    <textarea matInput [(ngModel)]="newComment" rows="2"></textarea>
                  </mat-form-field>
                  <button mat-raised-button color="primary" (click)="addComment()"
                          [disabled]="!newComment.trim()">
                    <mat-icon>send</mat-icon> Envoyer
                  </button>
                </div>
              }
            </mat-card-content>
          </mat-card>
        </div>
      </div>
    } @else {
      <p>Alerte introuvable.</p>
    }
  `,
  styles: [`
    .header { display: flex; align-items: center; gap: 8px; margin-bottom: 24px; }
    .header h1 { margin: 0; }
    .detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
    .comments-card { grid-column: 1 / -1; }
    .info-row { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #eee; }
    .label { font-weight: 500; color: #555; }
    .severity-badge { padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; text-transform: uppercase; }
    .severity-critical { background: #ffcdd2; color: #b71c1c; }
    .severity-warning { background: #fff3e0; color: #e65100; }
    .severity-info { background: #e3f2fd; color: #1565c0; }
    .status-chip { padding: 4px 8px; border-radius: 12px; font-size: 11px; font-weight: 500; }
    .status-open { background: #fff3e0; color: #e65100; }
    .status-acknowledged { background: #e3f2fd; color: #1565c0; }
    .status-in_progress { background: #f3e5f5; color: #7b1fa2; }
    .status-resolved { background: #e8f5e9; color: #2e7d32; }
    .status-false_positive { background: #fafafa; color: #616161; }
    .sla-breached { color: #d32f2f; font-size: 18px; vertical-align: middle; }
    .sla-text { color: #d32f2f; font-weight: 500; font-size: 12px; }
    .inline-icon { font-size: 16px; width: 16px; height: 16px; margin-left: 4px; }
    .description { margin: 16px 0; color: #333; line-height: 1.5; }
    .action-buttons { display: flex; gap: 12px; margin-top: 16px; flex-wrap: wrap; }
    .action-form { margin-top: 16px; }
    .full-width { width: 100%; }
    .form-actions { display: flex; gap: 8px; }
    .comment { padding: 12px 0; border-bottom: 1px solid #eee; }
    .comment-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
    .comment-author { font-weight: 500; font-size: 13px; }
    .comment-date { color: #999; font-size: 12px; margin-left: auto; }
    .comment-content { margin: 0; color: #333; }
    .no-comments { color: #999; font-style: italic; }
    .add-comment { margin-top: 16px; }
    @media (max-width: 768px) {
      .detail-grid { grid-template-columns: 1fr; }
    }
  `],
})
export class AlertDetailComponent implements OnInit {
  alert = signal<AlertDetailResponse | null>(null);
  loading = signal(true);

  showResolveForm = false;
  showFalsePositiveForm = false;
  showAssignForm = false;
  resolutionNote = '';
  falsePositiveReason = '';
  assignUserId = '';
  newComment = '';

  private alertId = '';
  private userId = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private api: ApiService,
    private auth: AuthService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit() {
    this.alertId = this.route.snapshot.paramMap.get('id') || '';
    this.userId = this.auth.getUserId();
    this.loadAlert();
  }

  isTerminal(): boolean {
    if (this.auth.isReadOnly()) return true;
    const status = this.alert()?.status;
    return status === 'RESOLVED' || status === 'FALSE_POSITIVE';
  }

  acknowledge() {
    this.api.acknowledgeAlert(this.alertId, this.userId).subscribe({
      next: (updated) => {
        this.alert.set(updated);
        this.snackBar.open('Alerte acquittee', 'OK', { duration: 3000 });
      },
      error: () => this.snackBar.open('Erreur lors de l\'acquittement', 'OK', { duration: 3000 }),
    });
  }

  assign() {
    this.http.post<AlertDetailResponse>(`/api/v1/alerts/${this.alertId}/assign`, { userId: this.assignUserId }).subscribe({
      next: (updated) => {
        this.alert.set(updated);
        this.showAssignForm = false;
        this.assignUserId = '';
        this.snackBar.open('Alerte assignee', 'OK', { duration: 3000 });
      },
      error: () => this.snackBar.open('Erreur lors de l\'assignation', 'OK', { duration: 3000 }),
    });
  }

  resolve() {
    this.api.resolveAlert(this.alertId, this.userId, this.resolutionNote).subscribe({
      next: (updated) => {
        this.alert.set(updated);
        this.showResolveForm = false;
        this.resolutionNote = '';
        this.snackBar.open('Alerte resolue', 'OK', { duration: 3000 });
      },
      error: () => this.snackBar.open('Erreur lors de la resolution', 'OK', { duration: 3000 }),
    });
  }

  markFalsePositive() {
    this.api.markFalsePositive(this.alertId, this.userId, this.falsePositiveReason).subscribe({
      next: (updated) => {
        this.alert.set(updated);
        this.showFalsePositiveForm = false;
        this.falsePositiveReason = '';
        this.snackBar.open('Marque comme faux positif', 'OK', { duration: 3000 });
      },
      error: () => this.snackBar.open('Erreur', 'OK', { duration: 3000 }),
    });
  }

  addComment() {
    this.api.addAlertComment(this.alertId, this.userId, this.newComment).subscribe({
      next: (updated) => {
        this.alert.set(updated);
        this.newComment = '';
      },
      error: () => this.snackBar.open('Erreur lors de l\'ajout du commentaire', 'OK', { duration: 3000 }),
    });
  }

  createIntervention() {
    const a = this.alert();
    if (!a) return;
    this.http.post<any>('/api/v1/interventions', {
      alertId: a.id,
      deviceId: a.deviceId,
      tenantId: a.tenantId,
    }).subscribe({
      next: (intervention) => {
        this.snackBar.open('Intervention creee', 'Voir', { duration: 5000 }).onAction().subscribe(() => {
          this.router.navigate(['/interventions', intervention.id]);
        });
      },
      error: () => this.snackBar.open('Erreur lors de la creation', 'OK', { duration: 3000 }),
    });
  }

  private loadAlert() {
    this.api.getAlertById(this.alertId).subscribe({
      next: (alert) => {
        this.alert.set(alert);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
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
