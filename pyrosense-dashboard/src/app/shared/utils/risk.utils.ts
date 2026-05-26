export function getRiskLevel(score: number): 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL' {
  if (score >= 80) return 'CRITICAL';
  if (score >= 60) return 'HIGH';
  if (score >= 40) return 'MODERATE';
  return 'LOW';
}

export function getRiskColor(score: number): string {
  if (score >= 80) return '#d32f2f';
  if (score >= 60) return '#f57c00';
  if (score >= 40) return '#fbc02d';
  return '#388e3c';
}

export function getSeverityColor(severity: string): string {
  switch (severity.toUpperCase()) {
    case 'CRITICAL': return '#d32f2f';
    case 'WARNING': case 'HIGH': return '#f57c00';
    case 'INFO': case 'MODERATE': return '#1976d2';
    default: return '#388e3c';
  }
}
