import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { Subject, takeUntil } from 'rxjs';
import { ApiService, DeviceResponse, DeviceStatisticsResponse } from '../../core/services/api.service';
import { SkeletonLoaderComponent } from '../../shared/components/skeleton-loader.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

@Component({
  selector: 'app-device-list',
  standalone: true,
  imports: [
    CommonModule, RouterModule, FormsModule,
    MatTableModule, MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatPaginatorModule,
    SkeletonLoaderComponent, EmptyStateComponent,
  ],
  template: `
    <div class="devices-container">
      <h1>Capteurs</h1>

      @if (statistics()) {
        <div class="stats-row">
          <mat-card class="stat-card total">
            <mat-card-content>
              <div class="stat-value">{{ statistics()!.total }}</div>
              <div class="stat-label">Total</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card active">
            <mat-card-content>
              <div class="stat-value">{{ statistics()!.active }}</div>
              <div class="stat-label">Actifs</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card offline">
            <mat-card-content>
              <div class="stat-value">{{ statistics()!.offline }}</div>
              <div class="stat-label">Hors-ligne</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card provisioned">
            <mat-card-content>
              <div class="stat-value">{{ statistics()!.provisioned }}</div>
              <div class="stat-label">Provisionnes</div>
            </mat-card-content>
          </mat-card>
        </div>
      }

      <div class="filters-row">
        <mat-form-field appearance="outline">
          <mat-label>Rechercher</mat-label>
          <input matInput [(ngModel)]="searchTerm" (ngModelChange)="onFilterChange()" placeholder="Numero de serie...">
          <mat-icon matPrefix>search</mat-icon>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Statut</mat-label>
          <mat-select [(value)]="statusFilter" (selectionChange)="onFilterChange()">
            <mat-option value="">Tous</mat-option>
            <mat-option value="ACTIVE">Actif</mat-option>
            <mat-option value="OFFLINE">Hors-ligne</mat-option>
            <mat-option value="PROVISIONED">Provisionne</mat-option>
            <mat-option value="REVOKED">Revoque</mat-option>
          </mat-select>
        </mat-form-field>
      </div>

      @if (loading()) {
        <app-skeleton type="table" [count]="5" [columns]="5" />
      } @else if (filteredDevices().length === 0) {
        <app-empty-state icon="sensors_off" title="Aucun capteur" message="Aucun capteur ne correspond aux criteres de recherche." />
      } @else {
        <table mat-table [dataSource]="filteredDevices()" class="devices-table">
          <ng-container matColumnDef="serialNumber">
            <th mat-header-cell *matHeaderCellDef>Numero de serie</th>
            <td mat-cell *matCellDef="let device">
              <a [routerLink]="['/devices', device.id]" class="device-link">{{ device.serialNumber }}</a>
            </td>
          </ng-container>

          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Statut</th>
            <td mat-cell *matCellDef="let device">
              <span class="status-chip" [class]="'status-' + device.status.toLowerCase()">
                {{ getStatusLabel(device.status) }}
              </span>
            </td>
          </ng-container>

          <ng-container matColumnDef="connectivity">
            <th mat-header-cell *matHeaderCellDef>Connectivite</th>
            <td mat-cell *matCellDef="let device">{{ device.connectivityType }}</td>
          </ng-container>

          <ng-container matColumnDef="firmware">
            <th mat-header-cell *matHeaderCellDef>Firmware</th>
            <td mat-cell *matCellDef="let device">{{ device.firmwareVersion }}</td>
          </ng-container>

          <ng-container matColumnDef="lastSeen">
            <th mat-header-cell *matHeaderCellDef>Derniere comm.</th>
            <td mat-cell *matCellDef="let device">
              {{ device.lastSeenAt | date:'dd/MM/yyyy HH:mm' }}
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;" class="device-row"></tr>
        </table>

        <mat-paginator
          [length]="totalDevices()"
          [pageSize]="pageSize"
          [pageSizeOptions]="[25, 50, 100]"
          (page)="onPageChange($event)">
        </mat-paginator>
      }
    </div>
  `,
  styles: [`
    .devices-container { max-width: 1200px; }
    .stats-row { display: flex; gap: 16px; margin-bottom: 24px; flex-wrap: wrap; }
    .stat-card { flex: 1; min-width: 140px; text-align: center; }
    .stat-value { font-size: 28px; font-weight: 700; }
    .stat-label { font-size: 12px; color: #666; margin-top: 4px; }
    .stat-card.total .stat-value { color: #1976d2; }
    .stat-card.active .stat-value { color: #2e7d32; }
    .stat-card.offline .stat-value { color: #d32f2f; }
    .stat-card.provisioned .stat-value { color: #f57c00; }
    .filters-row { display: flex; gap: 16px; margin-bottom: 16px; align-items: center; }
    .devices-table { width: 100%; }
    .device-link { color: #1976d2; text-decoration: none; font-weight: 500; font-family: monospace; }
    .device-link:hover { text-decoration: underline; }
    .device-row:hover { background: #f5f5f5; cursor: pointer; }
    .status-chip {
      padding: 4px 8px; border-radius: 12px; font-size: 11px; font-weight: 500;
    }
    .status-active { background: #e8f5e9; color: #2e7d32; }
    .status-offline { background: #ffebee; color: #c62828; }
    .status-provisioned { background: #fff3e0; color: #e65100; }
    .status-revoked { background: #fafafa; color: #616161; }
    @media (max-width: 960px) {
      .filters-row { flex-wrap: wrap; }
      .stats-row { gap: 8px; }
    }
    @media (max-width: 600px) {
      .filters-row { flex-direction: column; }
      .stats-row { flex-direction: column; }
    }
  `],
})
export class DeviceListComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  devices = signal<DeviceResponse[]>([]);
  filteredDevices = signal<DeviceResponse[]>([]);
  statistics = signal<DeviceStatisticsResponse | null>(null);
  loading = signal(true);
  totalDevices = signal(0);

  searchTerm = '';
  statusFilter = '';
  pageSize = 50;
  currentPage = 0;

  displayedColumns = ['serialNumber', 'status', 'connectivity', 'firmware', 'lastSeen'];

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.loadStatistics();
    this.loadDevices();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.applyFilters();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadDevices();
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      ACTIVE: 'Actif', OFFLINE: 'Hors-ligne', PROVISIONED: 'Provisionne', REVOKED: 'Revoque',
    };
    return labels[status] || status;
  }

  private loadDevices(): void {
    this.loading.set(true);
    this.api.getDevicesByTenant(this.currentPage, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (devices) => {
          this.devices.set(devices);
          this.totalDevices.set(
            devices.length < this.pageSize
              ? this.currentPage * this.pageSize + devices.length
              : (this.currentPage + 2) * this.pageSize
          );
          this.applyFilters();
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  private loadStatistics(): void {
    this.api.getDeviceStatistics()
      .pipe(takeUntil(this.destroy$))
      .subscribe({ next: (stats) => this.statistics.set(stats) });
  }

  private applyFilters(): void {
    let result = this.devices();
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      result = result.filter(d => d.serialNumber.toLowerCase().includes(term));
    }
    if (this.statusFilter) {
      result = result.filter(d => d.status === this.statusFilter);
    }
    this.filteredDevices.set(result);
  }
}
