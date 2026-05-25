import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatButtonModule } from '@angular/material/button';
import { Subject, takeUntil } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { ApiService, NotificationResponse, NotificationStatistics } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

interface NotificationPreferences {
  userId: string;
  consentEmail: boolean;
  consentSms: boolean;
  consentPush: boolean;
}

@Component({
  selector: 'app-notification-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatTabsModule, MatIconModule,
    MatCardModule, MatProgressSpinnerModule, MatChipsModule,
    MatSlideToggleModule, MatSnackBarModule, MatButtonModule,
  ],
  template: `
    <div class="notifications-container">
      <h1>Notifications</h1>

      <mat-tab-group>
        <mat-tab label="Historique">
          <div class="tab-content">

      @if (statistics()) {
        <div class="stats-row">
          <mat-card class="stat-card sent">
            <mat-card-content>
              <div class="stat-value">{{ statistics()!.sent }}</div>
              <div class="stat-label">Envoyees</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card pending">
            <mat-card-content>
              <div class="stat-value">{{ statistics()!.pending }}</div>
              <div class="stat-label">En attente</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card retrying">
            <mat-card-content>
              <div class="stat-value">{{ statistics()!.retrying }}</div>
              <div class="stat-label">En reessai</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card failed">
            <mat-card-content>
              <div class="stat-value">{{ statistics()!.failed }}</div>
              <div class="stat-label">Echouees</div>
            </mat-card-content>
          </mat-card>
        </div>
      }

      @if (loading()) {
        <mat-spinner diameter="40"></mat-spinner>
      } @else {
        <table mat-table [dataSource]="notifications()" class="notifications-table">
          <ng-container matColumnDef="severity">
            <th mat-header-cell *matHeaderCellDef>Severite</th>
            <td mat-cell *matCellDef="let notif">
              <mat-icon [class]="'severity-icon severity-' + notif.severity.toLowerCase()">
                {{ getSeverityIcon(notif.severity) }}
              </mat-icon>
            </td>
          </ng-container>

          <ng-container matColumnDef="subject">
            <th mat-header-cell *matHeaderCellDef>Sujet</th>
            <td mat-cell *matCellDef="let notif">{{ notif.subject }}</td>
          </ng-container>

          <ng-container matColumnDef="channel">
            <th mat-header-cell *matHeaderCellDef>Canal</th>
            <td mat-cell *matCellDef="let notif">
              <span class="channel-badge">{{ getChannelLabel(notif.channel) }}</span>
            </td>
          </ng-container>

          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Statut</th>
            <td mat-cell *matCellDef="let notif">
              <span class="status-chip" [class]="'status-' + notif.status.toLowerCase()">
                {{ getStatusLabel(notif.status) }}
              </span>
            </td>
          </ng-container>

          <ng-container matColumnDef="createdAt">
            <th mat-header-cell *matHeaderCellDef>Date</th>
            <td mat-cell *matCellDef="let notif">{{ notif.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>

        @if (notifications().length === 0) {
          <mat-card class="empty-card">
            <mat-card-content>
              <mat-icon>notifications_none</mat-icon>
              <p>Aucune notification</p>
            </mat-card-content>
          </mat-card>
        }
      }

          </div>
        </mat-tab>

        <mat-tab label="Preferences">
          <div class="tab-content">
            <mat-card>
              <mat-card-header>
                <mat-card-title>Canaux de notification</mat-card-title>
                <mat-card-subtitle>Choisissez comment recevoir vos notifications</mat-card-subtitle>
              </mat-card-header>
              <mat-card-content>
                <div class="pref-list">
                  <div class="pref-item">
                    <div class="pref-info">
                      <mat-icon>email</mat-icon>
                      <div>
                        <div class="pref-label">Email</div>
                        <div class="pref-desc">Recevoir les notifications par email</div>
                      </div>
                    </div>
                    <mat-slide-toggle [(ngModel)]="preferences().consentEmail" (change)="savePreferences()"></mat-slide-toggle>
                  </div>
                  <div class="pref-item">
                    <div class="pref-info">
                      <mat-icon>sms</mat-icon>
                      <div>
                        <div class="pref-label">SMS</div>
                        <div class="pref-desc">Recevoir les alertes critiques par SMS</div>
                      </div>
                    </div>
                    <mat-slide-toggle [(ngModel)]="preferences().consentSms" (change)="savePreferences()"></mat-slide-toggle>
                  </div>
                  <div class="pref-item">
                    <div class="pref-info">
                      <mat-icon>notifications_active</mat-icon>
                      <div>
                        <div class="pref-label">Push</div>
                        <div class="pref-desc">Notifications push dans le navigateur</div>
                      </div>
                    </div>
                    <mat-slide-toggle [(ngModel)]="preferences().consentPush" (change)="savePreferences()"></mat-slide-toggle>
                  </div>
                </div>
              </mat-card-content>
            </mat-card>
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [`
    .notifications-container { max-width: 1100px; }
    .stats-row { display: flex; gap: 16px; margin-bottom: 24px; flex-wrap: wrap; }
    .stat-card { flex: 1; min-width: 140px; text-align: center; }
    .stat-value { font-size: 26px; font-weight: 700; }
    .stat-label { font-size: 11px; color: #666; margin-top: 4px; }
    .stat-card.sent .stat-value { color: #2e7d32; }
    .stat-card.pending .stat-value { color: #f57c00; }
    .stat-card.retrying .stat-value { color: #1976d2; }
    .stat-card.failed .stat-value { color: #d32f2f; }
    .notifications-table { width: 100%; }
    .severity-icon { font-size: 20px; }
    .severity-critical { color: #d32f2f; }
    .severity-warning { color: #f57c00; }
    .severity-info { color: #1976d2; }
    .channel-badge { padding: 2px 6px; border-radius: 3px; font-size: 10px; font-weight: 600; background: #e0e0e0; }
    .status-chip { padding: 4px 8px; border-radius: 12px; font-size: 11px; font-weight: 500; }
    .status-pending { background: #fff3e0; color: #e65100; }
    .status-sent { background: #e8f5e9; color: #2e7d32; }
    .status-failed { background: #ffcdd2; color: #b71c1c; }
    .status-retrying { background: #e3f2fd; color: #1565c0; }
    .empty-card { text-align: center; padding: 32px; }
    .empty-card mat-icon { font-size: 48px; width: 48px; height: 48px; color: #bbb; }
    .empty-card p { color: #666; margin-top: 8px; }
    .tab-content { padding: 24px 0; }
    .pref-list { display: flex; flex-direction: column; gap: 16px; padding: 16px 0; }
    .pref-item { display: flex; align-items: center; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #eee; }
    .pref-info { display: flex; align-items: center; gap: 12px; }
    .pref-info mat-icon { color: #5c6bc0; }
    .pref-label { font-weight: 500; }
    .pref-desc { font-size: 12px; color: #666; margin-top: 2px; }
  `],
})
export class NotificationListComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  notifications = signal<NotificationResponse[]>([]);
  statistics = signal<NotificationStatistics | null>(null);
  loading = signal(true);
  preferences = signal<NotificationPreferences>({ userId: '', consentEmail: true, consentSms: false, consentPush: true });

  displayedColumns = ['severity', 'subject', 'channel', 'status', 'createdAt'];

  constructor(private api: ApiService, private auth: AuthService, private http: HttpClient, private snackBar: MatSnackBar) {}

  ngOnInit() {
    this.loadNotifications();
    this.loadStatistics();
    this.loadPreferences();
  }

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  private loadNotifications() {
    this.api.getNotificationsByRecipient(this.auth.getUserId())
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => { this.notifications.set(data); this.loading.set(false); },
        error: () => this.loading.set(false),
      });
  }

  private loadStatistics() {
    this.api.getNotificationStatistics()
      .pipe(takeUntil(this.destroy$))
      .subscribe({ next: (stats) => this.statistics.set(stats) });
  }

  getSeverityIcon(severity: string): string {
    const icons: Record<string, string> = { CRITICAL: 'error', WARNING: 'warning', INFO: 'info' };
    return icons[severity] || 'info';
  }

  getChannelLabel(channel: string): string {
    const labels: Record<string, string> = {
      EMAIL: 'Email', SMS: 'SMS', PUSH: 'Push', WEBHOOK: 'Webhook', DASHBOARD: 'Dashboard',
    };
    return labels[channel] || channel;
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'En attente', SENT: 'Envoyee', FAILED: 'Echouee', RETRYING: 'Reessai',
    };
    return labels[status] || status;
  }

  savePreferences(): void {
    const userId = this.auth.getUserId();
    this.http.put(`/api/v1/notifications/preferences/${userId}`, this.preferences())
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.snackBar.open('Preferences sauvegardees', 'OK', { duration: 2000 }),
        error: () => this.snackBar.open('Erreur de sauvegarde', 'OK', { duration: 3000 }),
      });
  }

  private loadPreferences(): void {
    const userId = this.auth.getUserId();
    if (!userId) return;
    this.http.get<NotificationPreferences>(`/api/v1/notifications/preferences/${userId}`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({ next: (prefs) => this.preferences.set(prefs) });
  }
}
