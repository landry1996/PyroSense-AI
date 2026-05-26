export interface BuildingResponse {
  id: string;
  name: string;
  address: string;
  totalDevices: number;
  activeDevices: number;
  riskScore: number;
  status: string;
  lastAlertAt: string | null;
  highestAlertSeverity: string | null;
}
