import { Component, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { Subject, takeUntil } from 'rxjs';
import { ApiService, BuildingResponse } from '../../core/services/api.service';
import { RiskGaugeComponent } from '../../shared/components/risk-gauge.component';

@Component({
  selector: 'app-building-list',
  standalone: true,
  imports: [
    CommonModule, RouterModule, FormsModule,
    MatCardModule, MatIconModule, MatButtonModule, MatFormFieldModule,
    MatInputModule, MatProgressSpinnerModule, MatChipsModule,
    RiskGaugeComponent,
  ],
  template: `
    <div class="buildings-container">
      <div class="header-row">
        <h1>Batiments</h1>
        <mat-form-field appearance="outline" class="search-field">
          <mat-icon matPrefix>search</mat-icon>
          <input matInput placeholder="Rechercher..." [ngModel]="searchTerm()" (ngModelChange)="searchTerm.set($event)">
        </mat-form-field>
      </div>

      <div class="filter-chips">
        <mat-chip-set>
          <mat-chip [highlighted]="sortBy() === 'name'" (click)="sortBy.set('name')">Nom</mat-chip>
          <mat-chip [highlighted]="sortBy() === 'risk'" (click)="sortBy.set('risk')">Risque</mat-chip>
          <mat-chip [highlighted]="sortBy() === 'devices'" (click)="sortBy.set('devices')">Capteurs</mat-chip>
        </mat-chip-set>
      </div>

      @if (loading()) {
        <div class="loading-container">
          <mat-spinner diameter="40" />
        </div>
      } @else if (filteredBuildings().length === 0) {
        <mat-card class="empty-state">
          <mat-card-content>
            <mat-icon>apartment</mat-icon>
            @if (searchTerm()) {
              <p>Aucun batiment ne correspond a "{{ searchTerm() }}"</p>
            } @else {
              <p>Aucun batiment enregistre</p>
            }
          </mat-card-content>
        </mat-card>
      } @else {
        <div class="building-grid">
          @for (building of filteredBuildings(); track building.id) {
            <mat-card class="building-card" [routerLink]="['/buildings', building.id]">
              <mat-card-header>
                <mat-icon mat-card-avatar class="building-avatar">apartment</mat-icon>
                <mat-card-title>{{ building.name }}</mat-card-title>
                <mat-card-subtitle>{{ building.address }}</mat-card-subtitle>
              </mat-card-header>
              <mat-card-content>
                <div class="building-stats">
                  <div class="stat">
                    <span class="stat-value">{{ building.activeDevices }}/{{ building.totalDevices }}</span>
                    <span class="stat-label">Capteurs</span>
                  </div>
                  <div class="stat">
                    <app-risk-gauge [score]="building.riskScore" />
                  </div>
                </div>
                <div class="status-row">
                  <span class="status-badge" [class]="'status-' + building.status.toLowerCase()">
                    {{ getStatusLabel(building.status) }}
                  </span>
                  @if (building.lastAlertAt) {
                    <span class="last-alert">Derniere alerte: {{ building.lastAlertAt | date:'dd/MM/yyyy' }}</span>
                  }
                </div>
              </mat-card-content>
            </mat-card>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .buildings-container { max-width: 1200px; }
    .header-row { display: flex; align-items: center; gap: 24px; flex-wrap: wrap; }
    .header-row h1 { margin: 0; }
    .search-field { flex: 0 1 300px; }
    .filter-chips { margin-bottom: 16px; }
    .loading-container { display: flex; justify-content: center; padding: 64px; }
    .building-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 16px;
    }
    .building-card { cursor: pointer; transition: box-shadow 0.2s, transform 0.2s; }
    .building-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.15); transform: translateY(-2px); }
    .building-avatar { color: #5c6bc0; }
    .building-stats { display: flex; align-items: center; gap: 24px; padding: 16px 0; }
    .stat { display: flex; flex-direction: column; align-items: center; }
    .stat-value { font-size: 22px; font-weight: 500; }
    .stat-label { font-size: 11px; color: rgba(0,0,0,0.6); }
    .status-row { display: flex; align-items: center; gap: 12px; padding-top: 8px; border-top: 1px solid #eee; }
    .status-badge {
      padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; text-transform: uppercase;
    }
    .status-ok { background: #e8f5e9; color: #2e7d32; }
    .status-watch { background: #fff8e1; color: #f9a825; }
    .status-at_risk { background: #fff3e0; color: #e65100; }
    .status-critical { background: #ffcdd2; color: #b71c1c; }
    .last-alert { font-size: 11px; color: #666; }
    .empty-state { text-align: center; padding: 48px; }
    .empty-state mat-icon { font-size: 48px; width: 48px; height: 48px; color: #bbb; }
    .empty-state p { color: #666; margin-top: 8px; }

    @media (max-width: 600px) {
      .building-grid { grid-template-columns: 1fr; }
      .header-row { flex-direction: column; align-items: stretch; }
      .search-field { flex: 1; }
    }
  `],
})
export class BuildingListComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  loading = signal(true);
  buildings = signal<BuildingResponse[]>([]);
  searchTerm = signal('');
  sortBy = signal<'name' | 'risk' | 'devices'>('name');

  filteredBuildings = computed(() => {
    let result = this.buildings();
    const term = this.searchTerm().toLowerCase();
    if (term) {
      result = result.filter(b =>
        b.name.toLowerCase().includes(term) || b.address.toLowerCase().includes(term)
      );
    }
    const sort = this.sortBy();
    return [...result].sort((a, b) => {
      if (sort === 'risk') return b.riskScore - a.riskScore;
      if (sort === 'devices') return b.totalDevices - a.totalDevices;
      return a.name.localeCompare(b.name);
    });
  });

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.getBuildings()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => { this.buildings.set(data); this.loading.set(false); },
        error: () => this.loading.set(false),
      });
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      OK: 'OK', WATCH: 'Surveillance', AT_RISK: 'A risque', CRITICAL: 'Critique',
    };
    return labels[status] || status;
  }
}
