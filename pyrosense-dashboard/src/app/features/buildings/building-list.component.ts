import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RiskGaugeComponent } from '../../shared/components/risk-gauge.component';

interface BuildingCard {
  id: string;
  name: string;
  address: string;
  deviceCount: number;
  activeDevices: number;
  avgRiskScore: number;
  criticalAlerts: number;
}

@Component({
  selector: 'app-building-list',
  standalone: true,
  imports: [CommonModule, RouterModule, MatCardModule, MatIconModule, MatButtonModule, MatProgressSpinnerModule, RiskGaugeComponent],
  template: `
    <h1>Batiments</h1>
    @if (loading()) {
      <mat-spinner diameter="40" />
    } @else {
      <div class="building-grid">
        @for (building of buildings(); track building.id) {
          <mat-card class="building-card" [routerLink]="['/buildings', building.id]">
            <mat-card-header>
              <mat-icon mat-card-avatar>apartment</mat-icon>
              <mat-card-title>{{ building.name }}</mat-card-title>
              <mat-card-subtitle>{{ building.address }}</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              <div class="building-stats">
                <div class="stat">
                  <span class="stat-value">{{ building.activeDevices }}/{{ building.deviceCount }}</span>
                  <span class="stat-label">Capteurs</span>
                </div>
                <div class="stat">
                  <app-risk-gauge [score]="building.avgRiskScore" />
                </div>
                @if (building.criticalAlerts > 0) {
                  <div class="stat critical">
                    <span class="stat-value">{{ building.criticalAlerts }}</span>
                    <span class="stat-label">Alertes critiques</span>
                  </div>
                }
              </div>
            </mat-card-content>
          </mat-card>
        }
      </div>
    }
  `,
  styles: [`
    .building-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 16px;
      margin-top: 16px;
    }
    .building-card { cursor: pointer; transition: box-shadow 0.2s; }
    .building-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
    .building-stats { display: flex; align-items: center; gap: 24px; padding: 16px 0; }
    .stat { display: flex; flex-direction: column; align-items: center; }
    .stat-value { font-size: 24px; font-weight: 500; }
    .stat-label { font-size: 12px; color: rgba(0,0,0,0.6); }
    .stat.critical .stat-value { color: #d32f2f; }
  `],
})
export class BuildingListComponent implements OnInit {
  loading = signal(true);
  buildings = signal<BuildingCard[]>([]);

  ngOnInit(): void {
    // TODO: Replace with real API (buildings endpoint needed in future sprint)
    setTimeout(() => {
      this.buildings.set([
        { id: 'b1', name: 'Tour Montparnasse', address: '33 Av. du Maine, Paris', deviceCount: 24, activeDevices: 22, avgRiskScore: 28, criticalAlerts: 0 },
        { id: 'b2', name: 'Batiment A - Site Industriel', address: '12 Rue de l\'Usine, Lyon', deviceCount: 16, activeDevices: 14, avgRiskScore: 62, criticalAlerts: 2 },
        { id: 'b3', name: 'Centre Commercial Riviera', address: '45 Bd des Capucines, Nice', deviceCount: 8, activeDevices: 8, avgRiskScore: 15, criticalAlerts: 0 },
      ]);
      this.loading.set(false);
    }, 300);
  }
}
