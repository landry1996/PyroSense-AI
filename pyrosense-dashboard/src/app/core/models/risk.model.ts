export interface RiskSummary {
  buildingId: string;
  averageScore: number;
  maxScore: number;
  criticalDevices: number;
  totalDevices: number;
}

export interface TenantRiskSummaryResponse {
  avgScore: number;
  trend: string;
  buildingsAtRisk: number;
}

export interface RiskHistoryPoint {
  date: string;
  score: number;
}
