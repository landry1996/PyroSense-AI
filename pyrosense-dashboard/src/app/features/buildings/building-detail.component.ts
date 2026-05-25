import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RiskGaugeComponent } from '../../shared/components/risk-gauge.component';
import { SeverityBadgeComponent } from '../../shared/components/severity-badge.component';
import { DeviceStatusBadgeComponent } from '../../shared/components/device-status-badge.component';
import { ApiService, DeviceResponse } from '../../core/services/api.service';

@Component({
  selector: 'app-building-detail',
  standalone: true,
  imports: [
    CommonModule, RouterModule, MatCardModule, MatIconModule, MatTableModule,
    MatChipsModule, MatProgressSpinnerModule, RiskGaugeComponent, SeverityBadgeComponent, DeviceStatusBadgeComponent,
  ],
  template: `
    <h1>{{ buildingName() }}</h1>
    @if (loading()) {
      <mat-spinner diameter="40" />
    } @else {
      <div class="summary-cards">
        <mat-card>
          <mat-card-content>
            <app-risk-gauge [score]="avgRiskScore()" />
            <div class="card-label">Score moyen</div>
          </mat-card-content>
        </mat-card>
        <mat-card>
          <mat-card-content>
            <div class="big-number">{{ devices().length }}</div>
            <div class="card-label">Capteurs</div>
          </mat-card-content>
        </mat-card>
        <mat-card>
          <mat-card-content>
            <div class="big-number active">{{ activeCount() }}</div>
            <div class="card-label">Actifs</div>
          </mat-card-content>
        </mat-card>
      </div>

      <h2>Capteurs</h2>
      <table mat-table [dataSource]="devices()" class="device-table">
        <ng-container matColumnDef="serialNumber">
          <th mat-header-cell *matHeaderCellDef>Serial</th>
          <td mat-cell *matCellDef="let d">
            <a [routerLink]="['/devices', d.id]">{{ d.serialNumber }}</a>
          </td>
        </ng-container>
        <ng-container matColumnDef="status">
          <th mat-header-cell *matHeaderCellDef>Statut</th>
          <td mat-cell *matCellDef="let d"><app-device-status-badge [status]="d.status" /></td>
        </ng-container>
        <ng-container matColumnDef="connectivity">
          <th mat-header-cell *matHeaderCellDef>Connectivite</th>
          <td mat-cell *matCellDef="let d">{{ d.connectivityType }}</td>
        </ng-container>
        <ng-container matColumnDef="lastSeen">
          <th mat-header-cell *matHeaderCellDef>Derniere activite</th>
          <td mat-cell *matCellDef="let d">{{ d.lastSeenAt | date:'short' }}</td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
        <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
      </table>
    }
  `,
  styles: [`
    .summary-cards { display: flex; gap: 16px; margin-bottom: 24px; }
    .summary-cards mat-card { flex: 1; text-align: center; }
    .summary-cards mat-card-content { padding: 24px; }
    .big-number { font-size: 48px; font-weight: 500; }
    .big-number.active { color: #388e3c; }
    .card-label { color: rgba(0,0,0,0.6); margin-top: 8px; }
    .device-table { width: 100%; }
    a { color: #1976d2; text-decoration: none; }
    a:hover { text-decoration: underline; }
  `],
})
export class BuildingDetailComponent implements OnInit, OnDestroy {
  loading = signal(true);
  buildingName = signal('');
  avgRiskScore = signal(0);
  devices = signal<DeviceResponse[]>([]);
  activeCount = signal(0);
  displayedColumns = ['serialNumber', 'status', 'connectivity', 'lastSeen'];

  private pollingInterval: any;

  constructor(
    private route: ActivatedRoute,
    private api: ApiService,
  ) {}

  ngOnInit(): void {
    const buildingId = this.route.snapshot.paramMap.get('id')!;
    this.buildingName.set('Batiment ' + buildingId);
    this.loadDevices(buildingId);
    this.pollingInterval = setInterval(() => this.loadDevices(buildingId), 30000);
  }

  ngOnDestroy(): void {
    if (this.pollingInterval) clearInterval(this.pollingInterval);
  }

  private loadDevices(buildingId: string): void {
    this.api.getDevicesByBuilding(buildingId).subscribe({
      next: (devices) => {
        this.devices.set(devices);
        this.activeCount.set(devices.filter(d => d.status === 'ACTIVE').length);
        this.loading.set(false);
      },
      error: () => {
        // Fallback mock data for development
        this.devices.set([
          { id: 'd1', serialNumber: 'PYR-001-A3F2', tenantId: 't1', buildingId: buildingId, panelId: 'p1', status: 'ACTIVE', firmwareVersion: '1.2.0', connectivityType: 'MQTT', lastSeenAt: new Date().toISOString() },
          { id: 'd2', serialNumber: 'PYR-002-B7C1', tenantId: 't1', buildingId: buildingId, panelId: 'p1', status: 'ACTIVE', firmwareVersion: '1.2.0', connectivityType: 'MQTT', lastSeenAt: new Date().toISOString() },
          { id: 'd3', serialNumber: 'PYR-003-D9E4', tenantId: 't1', buildingId: buildingId, panelId: 'p2', status: 'OFFLINE', firmwareVersion: '1.1.0', connectivityType: 'REST', lastSeenAt: new Date(Date.now() - 3600000).toISOString() },
        ]);
        this.activeCount.set(2);
        this.avgRiskScore.set(42);
        this.loading.set(false);
      },
    });
  }
}
