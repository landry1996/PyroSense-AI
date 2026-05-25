import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { Subject, takeUntil } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';

interface UserResponse {
  id: string;
  email: string;
  fullName: string;
  status: string;
  createdAt: string;
  lastLoginAt: string | null;
}

interface AuditLogEntry {
  id: string;
  action: string;
  resourceType: string;
  resourceId: string | null;
  userId: string | null;
  ipAddress: string | null;
  details: string | null;
  timestamp: string;
}

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatTabsModule, MatTableModule, MatButtonModule, MatIconModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatChipsModule,
  ],
  template: `
    <div class="admin-container">
      <h1>Administration</h1>

      <mat-tab-group>
        <!-- Users Tab -->
        <mat-tab label="Utilisateurs">
          <div class="tab-content">
            @if (usersLoading()) {
              <mat-spinner diameter="40"></mat-spinner>
            } @else {
              <table mat-table [dataSource]="users()" class="users-table">
                <ng-container matColumnDef="fullName">
                  <th mat-header-cell *matHeaderCellDef>Nom</th>
                  <td mat-cell *matCellDef="let user">{{ user.fullName }}</td>
                </ng-container>

                <ng-container matColumnDef="email">
                  <th mat-header-cell *matHeaderCellDef>Email</th>
                  <td mat-cell *matCellDef="let user">{{ user.email }}</td>
                </ng-container>

                <ng-container matColumnDef="status">
                  <th mat-header-cell *matHeaderCellDef>Statut</th>
                  <td mat-cell *matCellDef="let user">
                    <span class="status-chip" [class]="'status-' + user.status.toLowerCase()">
                      {{ user.status === 'ACTIVE' ? 'Actif' : user.status }}
                    </span>
                  </td>
                </ng-container>

                <ng-container matColumnDef="createdAt">
                  <th mat-header-cell *matHeaderCellDef>Cree le</th>
                  <td mat-cell *matCellDef="let user">{{ user.createdAt | date:'dd/MM/yyyy' }}</td>
                </ng-container>

                <ng-container matColumnDef="lastLogin">
                  <th mat-header-cell *matHeaderCellDef>Derniere connexion</th>
                  <td mat-cell *matCellDef="let user">
                    {{ user.lastLoginAt ? (user.lastLoginAt | date:'dd/MM/yyyy HH:mm') : 'Jamais' }}
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="userColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: userColumns;"></tr>
              </table>

              @if (users().length === 0) {
                <p class="empty">Aucun utilisateur dans ce tenant</p>
              }
            }
          </div>
        </mat-tab>

        <!-- Audit Log Tab -->
        <mat-tab label="Journal d'audit">
          <div class="tab-content">
            @if (auditLoading()) {
              <mat-spinner diameter="40"></mat-spinner>
            } @else {
              <table mat-table [dataSource]="auditEntries()" class="audit-table">
                <ng-container matColumnDef="timestamp">
                  <th mat-header-cell *matHeaderCellDef>Date</th>
                  <td mat-cell *matCellDef="let entry">{{ entry.timestamp | date:'dd/MM/yyyy HH:mm:ss' }}</td>
                </ng-container>

                <ng-container matColumnDef="action">
                  <th mat-header-cell *matHeaderCellDef>Action</th>
                  <td mat-cell *matCellDef="let entry">
                    <span class="action-badge">{{ entry.action }}</span>
                  </td>
                </ng-container>

                <ng-container matColumnDef="resourceType">
                  <th mat-header-cell *matHeaderCellDef>Ressource</th>
                  <td mat-cell *matCellDef="let entry">{{ entry.resourceType }}</td>
                </ng-container>

                <ng-container matColumnDef="resourceId">
                  <th mat-header-cell *matHeaderCellDef>ID Ressource</th>
                  <td mat-cell *matCellDef="let entry">{{ entry.resourceId ? (entry.resourceId | slice:0:8) + '...' : '-' }}</td>
                </ng-container>

                <ng-container matColumnDef="userId">
                  <th mat-header-cell *matHeaderCellDef>Utilisateur</th>
                  <td mat-cell *matCellDef="let entry">{{ entry.userId ? (entry.userId | slice:0:8) : 'Systeme' }}</td>
                </ng-container>

                <ng-container matColumnDef="ipAddress">
                  <th mat-header-cell *matHeaderCellDef>IP</th>
                  <td mat-cell *matCellDef="let entry">{{ entry.ipAddress || '-' }}</td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="auditColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: auditColumns;"></tr>
              </table>

              @if (auditEntries().length === 0) {
                <p class="empty">Aucune entree d'audit sur les 30 derniers jours</p>
              }
            }
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [`
    .admin-container { max-width: 1200px; }
    .tab-content { padding: 24px 0; }
    .users-table, .audit-table { width: 100%; }
    .status-chip { padding: 4px 8px; border-radius: 12px; font-size: 11px; font-weight: 500; }
    .status-active { background: #e8f5e9; color: #2e7d32; }
    .status-locked { background: #ffcdd2; color: #b71c1c; }
    .status-suspended { background: #fff3e0; color: #e65100; }
    .action-badge { font-family: monospace; font-size: 12px; padding: 2px 6px; background: #e3f2fd; border-radius: 3px; }
    .empty { color: #666; font-style: italic; text-align: center; padding: 32px; }
  `],
})
export class AdminComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private baseUrl = '/api/v1';

  users = signal<UserResponse[]>([]);
  auditEntries = signal<AuditLogEntry[]>([]);
  usersLoading = signal(true);
  auditLoading = signal(true);

  userColumns = ['fullName', 'email', 'status', 'createdAt', 'lastLogin'];
  auditColumns = ['timestamp', 'action', 'resourceType', 'resourceId', 'userId', 'ipAddress'];

  constructor(
    private http: HttpClient,
    private auth: AuthService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit() {
    this.loadUsers();
    this.loadAuditLog();
  }

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  private loadUsers() {
    const tenantId = this.auth.currentUser()?.tenantId;
    if (!tenantId) { this.usersLoading.set(false); return; }
    const params = new HttpParams().set('tenantId', tenantId).set('page', 0).set('size', 100);
    this.http.get<UserResponse[]>(`${this.baseUrl}/users`, { params })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => { this.users.set(data); this.usersLoading.set(false); },
        error: () => this.usersLoading.set(false),
      });
  }

  private loadAuditLog() {
    this.http.get<AuditLogEntry[]>(`${this.baseUrl}/audit-log`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => { this.auditEntries.set(data); this.auditLoading.set(false); },
        error: () => this.auditLoading.set(false),
      });
  }
}
