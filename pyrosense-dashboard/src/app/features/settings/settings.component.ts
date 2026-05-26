import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSliderModule } from '@angular/material/slider';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';
import { Subject, takeUntil } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { HasUnsavedChanges } from '../../core/guards/unsaved-changes.guard';

interface AlertThresholds {
  temperatureMax: number;
  thdMax: number;
  riskScoreCritical: number;
  riskScoreWarning: number;
  microArcCountMax: number;
}

interface EmergencyContact {
  id?: string;
  name: string;
  phone: string;
  email: string;
  role: string;
  priority: number;
}

interface TenantSettings {
  tenantId: string;
  thresholds: AlertThresholds;
}

interface NotificationPreferences {
  emailEnabled: boolean;
  smsEnabled: boolean;
  pushEnabled: boolean;
  emailForCritical: boolean;
  emailForWarning: boolean;
  emailForInfo: boolean;
  smsForCritical: boolean;
  smsForWarning: boolean;
  quietHoursStart: string;
  quietHoursEnd: string;
  digestFrequency: string;
}

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatTabsModule, MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatSliderModule, MatSlideToggleModule,
    MatSelectModule, MatTableModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatDialogModule,
  ],
  template: `
    <div class="settings-container">
      <h1>Parametres</h1>

      <mat-tab-group>
        <!-- Seuils Tab -->
        <mat-tab label="Seuils d'alerte">
          <div class="tab-content">
            @if (loadingThresholds()) {
              <mat-spinner diameter="40"></mat-spinner>
            } @else {
              <mat-card>
                <mat-card-header>
                  <mat-card-title>Configuration des seuils de detection</mat-card-title>
                  <mat-card-subtitle>Modifiez les seuils pour ajuster la sensibilite des alertes</mat-card-subtitle>
                </mat-card-header>
                <mat-card-content>
                  <div class="thresholds-grid">
                    <mat-form-field appearance="outline">
                      <mat-label>Temperature max (C)</mat-label>
                      <input matInput type="number" [(ngModel)]="thresholds().temperatureMax"
                        (ngModelChange)="onThresholdChange('temperatureMax', $event)">
                      <mat-hint>Defaut: 85 C</mat-hint>
                    </mat-form-field>

                    <mat-form-field appearance="outline">
                      <mat-label>THD max (%)</mat-label>
                      <input matInput type="number" [(ngModel)]="thresholds().thdMax"
                        (ngModelChange)="onThresholdChange('thdMax', $event)">
                      <mat-hint>Defaut: 40%</mat-hint>
                    </mat-form-field>

                    <mat-form-field appearance="outline">
                      <mat-label>Score risque critique</mat-label>
                      <input matInput type="number" [(ngModel)]="thresholds().riskScoreCritical"
                        (ngModelChange)="onThresholdChange('riskScoreCritical', $event)">
                      <mat-hint>Defaut: 70</mat-hint>
                    </mat-form-field>

                    <mat-form-field appearance="outline">
                      <mat-label>Score risque warning</mat-label>
                      <input matInput type="number" [(ngModel)]="thresholds().riskScoreWarning"
                        (ngModelChange)="onThresholdChange('riskScoreWarning', $event)">
                      <mat-hint>Defaut: 50</mat-hint>
                    </mat-form-field>

                    <mat-form-field appearance="outline">
                      <mat-label>Micro-arcs max (par periode)</mat-label>
                      <input matInput type="number" [(ngModel)]="thresholds().microArcCountMax"
                        (ngModelChange)="onThresholdChange('microArcCountMax', $event)">
                      <mat-hint>Defaut: 3</mat-hint>
                    </mat-form-field>
                  </div>
                </mat-card-content>
                <mat-card-actions align="end">
                  <button mat-button (click)="resetThresholds()" [disabled]="auth.isReadOnly()">Reinitialiser</button>
                  <button mat-flat-button color="primary" (click)="saveThresholds()" [disabled]="!thresholdsDirty() || auth.isReadOnly()">
                    Sauvegarder
                  </button>
                </mat-card-actions>
              </mat-card>
            }
          </div>
        </mat-tab>

        <!-- Contacts d'urgence Tab -->
        <mat-tab label="Contacts d'urgence">
          <div class="tab-content">
            <mat-card>
              <mat-card-header>
                <mat-card-title>Contacts d'urgence</mat-card-title>
                <mat-card-subtitle>Personnes a contacter en cas d'alerte critique</mat-card-subtitle>
              </mat-card-header>
              <mat-card-content>
                @if (contacts().length === 0) {
                  <p class="empty">Aucun contact d'urgence configure</p>
                } @else {
                  <table mat-table [dataSource]="contacts()" class="contacts-table">
                    <ng-container matColumnDef="priority">
                      <th mat-header-cell *matHeaderCellDef>#</th>
                      <td mat-cell *matCellDef="let c">{{ c.priority }}</td>
                    </ng-container>
                    <ng-container matColumnDef="name">
                      <th mat-header-cell *matHeaderCellDef>Nom</th>
                      <td mat-cell *matCellDef="let c">{{ c.name }}</td>
                    </ng-container>
                    <ng-container matColumnDef="role">
                      <th mat-header-cell *matHeaderCellDef>Role</th>
                      <td mat-cell *matCellDef="let c">{{ getRoleLabel(c.role) }}</td>
                    </ng-container>
                    <ng-container matColumnDef="phone">
                      <th mat-header-cell *matHeaderCellDef>Telephone</th>
                      <td mat-cell *matCellDef="let c">{{ c.phone }}</td>
                    </ng-container>
                    <ng-container matColumnDef="email">
                      <th mat-header-cell *matHeaderCellDef>Email</th>
                      <td mat-cell *matCellDef="let c">{{ c.email }}</td>
                    </ng-container>
                    <ng-container matColumnDef="actions">
                      <th mat-header-cell *matHeaderCellDef></th>
                      <td mat-cell *matCellDef="let c">
                        <button mat-icon-button color="warn" (click)="deleteContact(c)">
                          <mat-icon>delete</mat-icon>
                        </button>
                      </td>
                    </ng-container>
                    <tr mat-header-row *matHeaderRowDef="contactColumns"></tr>
                    <tr mat-row *matRowDef="let row; columns: contactColumns;"></tr>
                  </table>
                }
              </mat-card-content>
              <mat-card-actions>
                <button mat-flat-button color="primary" (click)="showAddContact = true" [disabled]="auth.isReadOnly()">
                  <mat-icon>add</mat-icon> Ajouter un contact
                </button>
              </mat-card-actions>
            </mat-card>

            @if (showAddContact) {
              <mat-card class="add-contact-form">
                <mat-card-header>
                  <mat-card-title>Nouveau contact</mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <div class="form-grid">
                    <mat-form-field appearance="outline">
                      <mat-label>Nom</mat-label>
                      <input matInput [(ngModel)]="newContact.name" required>
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>Telephone</mat-label>
                      <input matInput [(ngModel)]="newContact.phone" required>
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>Email</mat-label>
                      <input matInput type="email" [(ngModel)]="newContact.email" required>
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>Role</mat-label>
                      <input matInput [(ngModel)]="newContact.role" placeholder="ELECTRICIAN, PROPERTY_MANAGER, FIRE_DEPT">
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>Priorite</mat-label>
                      <input matInput type="number" [(ngModel)]="newContact.priority" min="1" max="10">
                    </mat-form-field>
                  </div>
                </mat-card-content>
                <mat-card-actions align="end">
                  <button mat-button (click)="showAddContact = false">Annuler</button>
                  <button mat-flat-button color="primary" (click)="addContact()"
                    [disabled]="!newContact.name || !newContact.phone || !newContact.email">
                    Ajouter
                  </button>
                </mat-card-actions>
              </mat-card>
            }
          </div>
        </mat-tab>

        <!-- Notifications Tab -->
        <mat-tab label="Notifications">
          <div class="tab-content">
            <mat-card>
              <mat-card-header>
                <mat-card-title>Preferences de notification</mat-card-title>
                <mat-card-subtitle>Configurez comment et quand vous souhaitez etre notifie</mat-card-subtitle>
              </mat-card-header>
              <mat-card-content>
                <div class="notif-section">
                  <h3>Canaux</h3>
                  <div class="toggle-row">
                    <mat-slide-toggle [(ngModel)]="notifPrefs().emailEnabled"
                      (ngModelChange)="onNotifChange('emailEnabled', $event)">Email</mat-slide-toggle>
                    <mat-slide-toggle [(ngModel)]="notifPrefs().smsEnabled"
                      (ngModelChange)="onNotifChange('smsEnabled', $event)">SMS</mat-slide-toggle>
                    <mat-slide-toggle [(ngModel)]="notifPrefs().pushEnabled"
                      (ngModelChange)="onNotifChange('pushEnabled', $event)">Push</mat-slide-toggle>
                  </div>
                </div>

                <div class="notif-section">
                  <h3>Filtres par severite</h3>
                  <div class="toggle-row">
                    <mat-slide-toggle [(ngModel)]="notifPrefs().emailForCritical"
                      (ngModelChange)="onNotifChange('emailForCritical', $event)">Email critique</mat-slide-toggle>
                    <mat-slide-toggle [(ngModel)]="notifPrefs().emailForWarning"
                      (ngModelChange)="onNotifChange('emailForWarning', $event)">Email warning</mat-slide-toggle>
                    <mat-slide-toggle [(ngModel)]="notifPrefs().emailForInfo"
                      (ngModelChange)="onNotifChange('emailForInfo', $event)">Email info</mat-slide-toggle>
                  </div>
                  <div class="toggle-row">
                    <mat-slide-toggle [(ngModel)]="notifPrefs().smsForCritical"
                      (ngModelChange)="onNotifChange('smsForCritical', $event)">SMS critique</mat-slide-toggle>
                    <mat-slide-toggle [(ngModel)]="notifPrefs().smsForWarning"
                      (ngModelChange)="onNotifChange('smsForWarning', $event)">SMS warning</mat-slide-toggle>
                  </div>
                </div>

                <div class="notif-section">
                  <h3>Heures calmes</h3>
                  <div class="quiet-hours">
                    <mat-form-field appearance="outline">
                      <mat-label>De</mat-label>
                      <input matInput type="time" [(ngModel)]="notifPrefs().quietHoursStart"
                        (ngModelChange)="onNotifChange('quietHoursStart', $event)">
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>A</mat-label>
                      <input matInput type="time" [(ngModel)]="notifPrefs().quietHoursEnd"
                        (ngModelChange)="onNotifChange('quietHoursEnd', $event)">
                    </mat-form-field>
                  </div>
                </div>

                <div class="notif-section">
                  <h3>Resume</h3>
                  <mat-form-field appearance="outline">
                    <mat-label>Frequence du digest</mat-label>
                    <mat-select [(ngModel)]="notifPrefs().digestFrequency"
                      (ngModelChange)="onNotifChange('digestFrequency', $event)">
                      <mat-option value="NONE">Desactive</mat-option>
                      <mat-option value="DAILY">Quotidien</mat-option>
                      <mat-option value="WEEKLY">Hebdomadaire</mat-option>
                    </mat-select>
                  </mat-form-field>
                </div>
              </mat-card-content>
              <mat-card-actions align="end">
                <button mat-flat-button color="primary" (click)="saveNotifPrefs()" [disabled]="!notifPrefsDirty() || auth.isReadOnly()">
                  Sauvegarder
                </button>
              </mat-card-actions>
            </mat-card>
          </div>
        </mat-tab>

        <!-- Tenant Info Tab -->
        <mat-tab label="Informations">
          <div class="tab-content">
            <mat-card>
              <mat-card-header>
                <mat-card-title>Informations du tenant</mat-card-title>
              </mat-card-header>
              <mat-card-content>
                <div class="info-grid">
                  <div class="info-item">
                    <span class="info-label">Identifiant</span>
                    <span class="info-value">{{ auth.currentUser()?.tenantId }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">Administrateur</span>
                    <span class="info-value">{{ auth.currentUser()?.fullName }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">Email</span>
                    <span class="info-value">{{ auth.currentUser()?.email }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">Roles</span>
                    <span class="info-value">{{ auth.userRoles().join(', ') }}</span>
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
    .settings-container { max-width: 900px; }
    .tab-content { padding: 24px 0; }
    .thresholds-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; padding: 16px 0; }
    .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 12px; padding: 16px 0; }
    .contacts-table { width: 100%; }
    .add-contact-form { margin-top: 16px; }
    .empty { color: #666; font-style: italic; text-align: center; padding: 24px; }
    .info-grid { display: grid; gap: 16px; padding: 16px 0; }
    .info-item { display: flex; flex-direction: column; }
    .info-label { font-size: 12px; color: #666; margin-bottom: 4px; }
    .info-value { font-size: 15px; font-weight: 500; }
    .notif-section { margin-bottom: 24px; }
    .notif-section h3 { margin-bottom: 12px; color: #333; font-size: 14px; }
    .toggle-row { display: flex; gap: 24px; flex-wrap: wrap; margin-bottom: 12px; }
    .quiet-hours { display: flex; gap: 16px; }
  `],
})
export class SettingsComponent implements OnInit, OnDestroy, HasUnsavedChanges {
  private destroy$ = new Subject<void>();
  private baseUrl = '/api/v1';

  loadingThresholds = signal(true);
  thresholds = signal<AlertThresholds>({
    temperatureMax: 85,
    thdMax: 40,
    riskScoreCritical: 70,
    riskScoreWarning: 50,
    microArcCountMax: 3,
  });
  thresholdsDirty = signal(false);
  contacts = signal<EmergencyContact[]>([]);
  showAddContact = false;
  newContact: EmergencyContact = { name: '', phone: '', email: '', role: 'ELECTRICIAN', priority: 1 };

  notifPrefs = signal<NotificationPreferences>({
    emailEnabled: true, smsEnabled: false, pushEnabled: true,
    emailForCritical: true, emailForWarning: true, emailForInfo: false,
    smsForCritical: true, smsForWarning: false,
    quietHoursStart: '22:00', quietHoursEnd: '07:00',
    digestFrequency: 'DAILY',
  });
  notifPrefsDirty = signal(false);

  contactColumns = ['priority', 'name', 'role', 'phone', 'email', 'actions'];

  constructor(
    private http: HttpClient,
    public auth: AuthService,
    private snackBar: MatSnackBar,
  ) {}

  hasUnsavedChanges(): boolean { return this.thresholdsDirty(); }

  ngOnInit(): void {
    this.loadSettings();
    this.loadContacts();
    this.loadNotifPrefs();
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  onThresholdChange(field: string, value: number): void {
    this.thresholds.update(t => ({ ...t, [field]: value }));
    this.thresholdsDirty.set(true);
  }

  resetThresholds(): void {
    this.thresholds.set({ temperatureMax: 85, thdMax: 40, riskScoreCritical: 70, riskScoreWarning: 50, microArcCountMax: 3 });
    this.thresholdsDirty.set(true);
  }

  saveThresholds(): void {
    const tenantId = this.auth.currentUser()?.tenantId;
    if (!tenantId) return;
    this.http.put(`${this.baseUrl}/tenants/${tenantId}/settings`, { thresholds: this.thresholds() })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.thresholdsDirty.set(false);
          this.snackBar.open('Seuils sauvegardes', 'OK', { duration: 3000 });
        },
        error: () => this.snackBar.open('Erreur lors de la sauvegarde', 'OK', { duration: 3000 }),
      });
  }

  addContact(): void {
    const tenantId = this.auth.currentUser()?.tenantId;
    if (!tenantId) return;
    this.http.post<EmergencyContact>(`${this.baseUrl}/tenants/${tenantId}/emergency-contacts`, this.newContact)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (contact) => {
          this.contacts.update(list => [...list, contact]);
          this.newContact = { name: '', phone: '', email: '', role: 'ELECTRICIAN', priority: 1 };
          this.showAddContact = false;
          this.snackBar.open('Contact ajoute', 'OK', { duration: 3000 });
        },
        error: () => this.snackBar.open('Erreur lors de l\'ajout', 'OK', { duration: 3000 }),
      });
  }

  deleteContact(contact: EmergencyContact): void {
    const tenantId = this.auth.currentUser()?.tenantId;
    if (!tenantId || !contact.id) return;
    this.http.delete(`${this.baseUrl}/tenants/${tenantId}/emergency-contacts/${contact.id}`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.contacts.update(list => list.filter(c => c.id !== contact.id));
          this.snackBar.open('Contact supprime', 'OK', { duration: 3000 });
        },
        error: () => this.snackBar.open('Erreur lors de la suppression', 'OK', { duration: 3000 }),
      });
  }

  onNotifChange(field: string, value: any): void {
    this.notifPrefs.update(p => ({ ...p, [field]: value }));
    this.notifPrefsDirty.set(true);
  }

  saveNotifPrefs(): void {
    const userId = this.auth.getUserId();
    if (!userId) return;
    this.http.put(`${this.baseUrl}/notifications/preferences/${userId}`, this.notifPrefs())
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.notifPrefsDirty.set(false);
          this.snackBar.open('Preferences sauvegardees', 'OK', { duration: 3000 });
        },
        error: () => this.snackBar.open('Erreur lors de la sauvegarde', 'OK', { duration: 3000 }),
      });
  }

  getRoleLabel(role: string): string {
    const labels: Record<string, string> = {
      ELECTRICIAN: 'Electricien', PROPERTY_MANAGER: 'Gestionnaire', FIRE_DEPT: 'Pompiers',
    };
    return labels[role] || role;
  }

  private loadSettings(): void {
    const tenantId = this.auth.currentUser()?.tenantId;
    if (!tenantId) { this.loadingThresholds.set(false); return; }
    this.http.get<TenantSettings>(`${this.baseUrl}/tenants/${tenantId}/settings`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => { this.thresholds.set(data.thresholds); this.loadingThresholds.set(false); },
        error: () => this.loadingThresholds.set(false),
      });
  }

  private loadContacts(): void {
    const tenantId = this.auth.currentUser()?.tenantId;
    if (!tenantId) return;
    this.http.get<EmergencyContact[]>(`${this.baseUrl}/tenants/${tenantId}/emergency-contacts`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({ next: (data) => this.contacts.set(data) });
  }

  private loadNotifPrefs(): void {
    const userId = this.auth.getUserId();
    if (!userId) return;
    this.http.get<NotificationPreferences>(`${this.baseUrl}/notifications/preferences/${userId}`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({ next: (data) => this.notifPrefs.set(data) });
  }
}
