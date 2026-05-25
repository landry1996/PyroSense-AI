import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subject, takeUntil } from 'rxjs';
import { ApiService, InterventionResponse, KanbanResponse, InterventionStatistics } from '../../core/services/api.service';

@Component({
  selector: 'app-intervention-list',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatChipsModule, MatTabsModule, MatProgressSpinnerModule,
  ],
  template: `
    <div class="interventions-container">
      <h1>Interventions</h1>

      @if (statistics()) {
        <div class="stats-row">
          <mat-card class="stat-card">
            <mat-card-content>
              <div class="stat-value">{{ statistics()!.total }}</div>
              <div class="stat-label">Total</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card active">
            <mat-card-content>
              <div class="stat-value">{{ statistics()!.inProgress }}</div>
              <div class="stat-label">En cours</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card done">
            <mat-card-content>
              <div class="stat-value">{{ statistics()!.completed }}</div>
              <div class="stat-label">Terminees</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card fp">
            <mat-card-content>
              <div class="stat-value">{{ statistics()!.falsePositives }}</div>
              <div class="stat-label">Faux positifs</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="stat-card risk">
            <mat-card-content>
              <div class="stat-value">{{ statistics()!.averageRiskReduction }}%</div>
              <div class="stat-label">Reduction risque moy.</div>
            </mat-card-content>
          </mat-card>
        </div>
      }

      @if (loading()) {
        <mat-spinner diameter="40"></mat-spinner>
      } @else if (kanban()) {
        <div class="kanban-board">
          <div class="kanban-column">
            <div class="column-header created">
              <span>Creees</span>
              <span class="count">{{ kanban()!.created.length }}</span>
            </div>
            @for (item of kanban()!.created; track item.id) {
              <ng-container *ngTemplateOutlet="cardTpl; context: { $implicit: item }"></ng-container>
            }
          </div>

          <div class="kanban-column">
            <div class="column-header planned">
              <span>Planifiees</span>
              <span class="count">{{ kanban()!.planned.length }}</span>
            </div>
            @for (item of kanban()!.planned; track item.id) {
              <ng-container *ngTemplateOutlet="cardTpl; context: { $implicit: item }"></ng-container>
            }
          </div>

          <div class="kanban-column">
            <div class="column-header assigned">
              <span>Assignees</span>
              <span class="count">{{ kanban()!.assigned.length }}</span>
            </div>
            @for (item of kanban()!.assigned; track item.id) {
              <ng-container *ngTemplateOutlet="cardTpl; context: { $implicit: item }"></ng-container>
            }
          </div>

          <div class="kanban-column">
            <div class="column-header in-progress">
              <span>En cours</span>
              <span class="count">{{ kanban()!.inProgress.length }}</span>
            </div>
            @for (item of kanban()!.inProgress; track item.id) {
              <ng-container *ngTemplateOutlet="cardTpl; context: { $implicit: item }"></ng-container>
            }
          </div>

          <div class="kanban-column">
            <div class="column-header completed">
              <span>Terminees</span>
              <span class="count">{{ kanban()!.completed.length }}</span>
            </div>
            @for (item of kanban()!.completed; track item.id) {
              <ng-container *ngTemplateOutlet="cardTpl; context: { $implicit: item }"></ng-container>
            }
          </div>
        </div>
      }
    </div>

    <ng-template #cardTpl let-item>
      <mat-card class="intervention-card" [routerLink]="['/interventions', item.id]">
        <mat-card-content>
          <div class="card-header">
            <span class="priority-badge" [class]="'priority-' + item.priority.toLowerCase()">
              {{ getPriorityLabel(item.priority) }}
            </span>
            <span class="type-label">{{ getTypeLabel(item.type) }}</span>
          </div>
          <p class="card-description">{{ item.description | slice:0:80 }}{{ item.description.length > 80 ? '...' : '' }}</p>
          <div class="card-footer">
            @if (item.assignedElectricianId) {
              <mat-icon class="small-icon">person</mat-icon>
              <span class="assigned">{{ item.assignedElectricianId | slice:0:8 }}</span>
            }
            @if (item.scheduledAt) {
              <mat-icon class="small-icon">schedule</mat-icon>
              <span class="date">{{ item.scheduledAt | date:'dd/MM' }}</span>
            }
          </div>
        </mat-card-content>
      </mat-card>
    </ng-template>
  `,
  styles: [`
    .interventions-container { max-width: 1400px; }
    .stats-row { display: flex; gap: 16px; margin-bottom: 24px; flex-wrap: wrap; }
    .stat-card { flex: 1; min-width: 140px; text-align: center; }
    .stat-value { font-size: 26px; font-weight: 700; }
    .stat-label { font-size: 11px; color: #666; margin-top: 4px; }
    .stat-card.active .stat-value { color: #7b1fa2; }
    .stat-card.done .stat-value { color: #2e7d32; }
    .stat-card.fp .stat-value { color: #f57c00; }
    .stat-card.risk .stat-value { color: #1976d2; }

    .kanban-board {
      display: flex; gap: 16px; overflow-x: auto; padding-bottom: 16px;
    }
    .kanban-column {
      min-width: 250px; flex: 1;
      background: #f5f5f5; border-radius: 8px; padding: 12px;
    }
    .column-header {
      display: flex; justify-content: space-between; align-items: center;
      padding: 8px 12px; border-radius: 6px; margin-bottom: 12px;
      font-weight: 600; font-size: 13px;
    }
    .column-header.created { background: #e3f2fd; color: #1565c0; }
    .column-header.planned { background: #fff3e0; color: #e65100; }
    .column-header.assigned { background: #f3e5f5; color: #7b1fa2; }
    .column-header.in-progress { background: #e8f5e9; color: #2e7d32; }
    .column-header.completed { background: #e0e0e0; color: #424242; }
    .count {
      background: rgba(0,0,0,0.1); border-radius: 10px;
      padding: 2px 8px; font-size: 11px;
    }

    .intervention-card {
      margin-bottom: 8px; cursor: pointer;
      transition: box-shadow 0.2s;
    }
    .intervention-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
    .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
    .card-description { font-size: 13px; color: #333; margin: 0 0 8px 0; line-height: 1.4; }
    .card-footer { display: flex; align-items: center; gap: 4px; font-size: 11px; color: #666; }
    .small-icon { font-size: 14px; width: 14px; height: 14px; }

    .priority-badge {
      padding: 2px 6px; border-radius: 3px; font-size: 10px;
      font-weight: 600; text-transform: uppercase;
    }
    .priority-urgent { background: #ffcdd2; color: #b71c1c; }
    .priority-high { background: #fff3e0; color: #e65100; }
    .priority-medium { background: #e3f2fd; color: #1565c0; }
    .priority-low { background: #f5f5f5; color: #616161; }
    .type-label { font-size: 10px; color: #888; text-transform: uppercase; }
  `],
})
export class InterventionListComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  kanban = signal<KanbanResponse | null>(null);
  statistics = signal<InterventionStatistics | null>(null);
  loading = signal(true);

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.loadData();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadData() {
    this.api.getInterventionKanban()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => { this.kanban.set(data); this.loading.set(false); },
        error: () => this.loading.set(false),
      });
    this.api.getInterventionStatistics()
      .pipe(takeUntil(this.destroy$))
      .subscribe({ next: (stats) => this.statistics.set(stats) });
  }

  getPriorityLabel(priority: string): string {
    const labels: Record<string, string> = { URGENT: 'Urgent', HIGH: 'Haute', MEDIUM: 'Moyenne', LOW: 'Basse' };
    return labels[priority] || priority;
  }

  getTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      PREVENTIVE: 'Preventive', CORRECTIVE: 'Corrective',
      PREDICTIVE: 'Predictive', EMERGENCY: 'Urgence',
    };
    return labels[type] || type;
  }
}
