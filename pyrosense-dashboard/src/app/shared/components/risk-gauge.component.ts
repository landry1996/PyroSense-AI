import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-risk-gauge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="gauge" [style.--color]="color()">
      <div class="gauge-value">{{ score }}</div>
      <div class="gauge-label">/ 100</div>
    </div>
  `,
  styles: [`
    .gauge {
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    .gauge-value {
      font-size: 48px;
      font-weight: 700;
      color: var(--color);
    }
    .gauge-label {
      font-size: 14px;
      color: rgba(0, 0, 0, 0.5);
    }
  `],
})
export class RiskGaugeComponent {
  @Input() score = 0;

  color = computed(() => {
    if (this.score >= 80) return '#d32f2f';
    if (this.score >= 60) return '#f57c00';
    if (this.score >= 40) return '#fbc02d';
    return '#388e3c';
  });
}
