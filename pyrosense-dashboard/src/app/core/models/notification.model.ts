export interface NotificationResponse {
  id: string;
  tenantId: string;
  recipientId: string;
  channel: string;
  severity: string;
  subject: string;
  status: string;
  retryCount: number;
  createdAt: string;
  sentAt: string | null;
  failureReason: string | null;
}

export interface NotificationStatistics {
  sent: number;
  failed: number;
  pending: number;
  retrying: number;
}
