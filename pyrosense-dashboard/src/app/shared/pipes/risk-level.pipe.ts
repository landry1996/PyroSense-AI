import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'riskLevel', standalone: true })
export class RiskLevelPipe implements PipeTransform {
  transform(score: number): string {
    if (score >= 80) return 'CRITICAL';
    if (score >= 60) return 'HIGH';
    if (score >= 40) return 'MODERATE';
    return 'LOW';
  }
}

@Pipe({ name: 'riskColor', standalone: true })
export class RiskColorPipe implements PipeTransform {
  transform(score: number): string {
    if (score >= 80) return '#d32f2f';
    if (score >= 60) return '#f57c00';
    if (score >= 40) return '#fbc02d';
    return '#388e3c';
  }
}
