import { Injectable, signal, computed } from '@angular/core';
import { forkJoin, catchError, of } from 'rxjs';
import { ApiService, DeviceStatisticsResponse, TenantRiskSummaryResponse, RiskHistoryPoint } from '../../core/services/api.service';

export interface DashboardSummary {
  totalBuildings: number;
  totalDevices: number;
  activeDevices: number;
  offlineDevices: number;
  avgRiskScore: number;
  riskTrend: string;
  criticalAlerts: number;
  warningAlerts: number;
  slaBreached: number;
  overdueInterventions: number;
  buildingsAtRisk: number;
}

@Injectable({ providedIn: 'root' })
export class DashboardStateService {
  private _summary = signal<DashboardSummary>({
    totalBuildings: 0,
    totalDevices: 0,
    activeDevices: 0,
    offlineDevices: 0,
    avgRiskScore: 0,
    riskTrend: 'STABLE',
    criticalAlerts: 0,
    warningAlerts: 0,
    slaBreached: 0,
    overdueInterventions: 0,
    buildingsAtRisk: 0,
  });
  private _riskHistory = signal<RiskHistoryPoint[]>([]);
  private _loading = signal(true);
  private _error = signal<string | null>(null);

  readonly summary = this._summary.asReadonly();
  readonly riskHistory = this._riskHistory.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  readonly riskLevel = computed(() => {
    const score = this._summary().avgRiskScore;
    if (score >= 70) return 'CRITICAL';
    if (score >= 50) return 'AT_RISK';
    if (score >= 30) return 'WATCH';
    return 'OK';
  });

  constructor(private api: ApiService) {}

  load(): void {
    this._loading.set(true);
    this._error.set(null);

    forkJoin({
      deviceStats: this.api.getDeviceStatistics().pipe(catchError(() => of({ total: 0, active: 0, offline: 0, provisioned: 0, revoked: 0 } as DeviceStatisticsResponse))),
      riskSummary: this.api.getTenantRiskSummary().pipe(catchError(() => of({ avgScore: 0, trend: 'STABLE', buildingsAtRisk: 0 } as TenantRiskSummaryResponse))),
      alertStats: this.api.getAlertStatistics().pipe(catchError(() => of({ totalOpen: 0, totalAcknowledged: 0, totalInProgress: 0, totalResolved: 0, criticalOpen: 0, slaBreached: 0 }))),
      buildings: this.api.getBuildings().pipe(catchError(() => of([]))),
      overdue: this.api.getInterventionsOverdue().pipe(catchError(() => of({ count: 0 }))),
      riskHistory: this.api.getTenantRiskHistory(30).pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ deviceStats, riskSummary, alertStats, buildings, overdue, riskHistory }) => {
        this._summary.set({
          totalBuildings: buildings.length,
          totalDevices: deviceStats.total,
          activeDevices: deviceStats.active,
          offlineDevices: deviceStats.offline,
          avgRiskScore: riskSummary.avgScore,
          riskTrend: riskSummary.trend,
          criticalAlerts: alertStats.criticalOpen,
          warningAlerts: alertStats.totalOpen - alertStats.criticalOpen,
          slaBreached: alertStats.slaBreached,
          overdueInterventions: overdue.count,
          buildingsAtRisk: riskSummary.buildingsAtRisk,
        });
        this._riskHistory.set(riskHistory);
        this._loading.set(false);
      },
      error: (err) => {
        this._error.set('Impossible de charger les donnees du dashboard');
        this._loading.set(false);
      },
    });
  }
}
