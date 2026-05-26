export interface ReportResponse {
  id: string;
  reportNumber: string;
  tenantId: string;
  buildingId: string;
  type: string;
  status: string;
  periodStart: string;
  periodEnd: string;
  fileName: string | null;
  signatureHash: string | null;
  metadata: ReportMetadata | null;
  createdAt: string;
  generatedAt: string | null;
}

export interface ReportMetadata {
  buildingName: string;
  buildingAddress: string;
  sensorCount: number;
  averageRiskScore: number;
  alertCount: number;
  criticalAlertCount: number;
  interventionCount: number;
  resolvedInterventionCount: number;
  recommendations: string[];
}

export interface DownloadTokenResponse {
  token: string;
  expiresAt: string;
}
