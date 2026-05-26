export interface InterventionResponse {
  id: string;
  tenantId: string;
  alertId: string;
  deviceId: string;
  type: string;
  priority: string;
  status: string;
  description: string;
  assignedElectricianId: string | null;
  scheduledAt: string | null;
  startedAt: string | null;
  completedAt: string | null;
  result: string | null;
  riskImpact: RiskImpactResponse | null;
  diagnostic: DiagnosticResponse | null;
  createdAt: string;
  updatedAt: string;
}

export interface RiskImpactResponse {
  riskScoreBefore: number;
  riskScoreAfter: number;
  avoidedIncidentEstimateDays: number | null;
  riskReduction: number;
}

export interface DiagnosticResponse {
  observations: string;
  measurementsTaken: string | null;
  recommendations: string | null;
  diagnosticBy: string;
  recordedAt: string;
}

export interface KanbanResponse {
  created: InterventionResponse[];
  planned: InterventionResponse[];
  assigned: InterventionResponse[];
  inProgress: InterventionResponse[];
  completed: InterventionResponse[];
  cancelled: InterventionResponse[];
}

export interface InterventionStatistics {
  total: number;
  inProgress: number;
  completed: number;
  falsePositives: number;
  averageRiskReduction: number;
}

export interface InterventionOverdueResponse {
  count: number;
}
