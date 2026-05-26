export interface AlertResponse {
  id: string;
  type: string;
  severity: string;
  status: string;
  deviceId: string;
  tenantId: string;
  title: string;
  description: string;
  createdAt: string;
}

export interface AlertDetailResponse {
  id: string;
  tenantId: string;
  deviceId: string;
  type: string;
  severity: string;
  title: string;
  description: string;
  status: string;
  assignedTo: string | null;
  escalationLevel: string;
  createdAt: string;
  slaDeadline: string;
  acknowledgedAt: string | null;
  acknowledgedBy: string | null;
  resolvedAt: string | null;
  resolvedBy: string | null;
  resolutionNote: string | null;
  occurrenceCount: number;
  lastOccurrenceAt: string | null;
  slaBreached: boolean;
  comments: AlertCommentResponse[];
}

export interface AlertCommentResponse {
  id: string;
  authorId: string;
  content: string;
  createdAt: string;
}

export interface AlertStatistics {
  totalOpen: number;
  totalAcknowledged: number;
  totalInProgress: number;
  totalResolved: number;
  criticalOpen: number;
  slaBreached: number;
}
