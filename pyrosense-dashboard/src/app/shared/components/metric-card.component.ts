import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-metric-card',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, RouterModule],
  template: `
    <mat-card class="metric-card" [class.clickable]="link" [class]="variant"
              [routerLink]="link" [attr.role]="link ? 'link' : null"
              [attr.aria-label]="ariaLabel || (label + ': ' + value)">
      <mat-card-content>
        <mat-icon class="metric-icon" [style.color]="iconColor">{{ icon }}</mat-icon>
        <div class="metric-value">{{ value }}</div>
        <div class="metric-label">{{ label }}</div>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .metric-card mat-card-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 20px 16px;
    }
    .metric-card.clickable { cursor: pointer; transition: box-shadow 0.2s, transform 0.2s; }
    .metric-card.clickable:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.15); transform: translateY(-2px); }
    .metric-icon { font-size: 36px; width: 36px; height: 36px; margin-bottom: 8px; }
    .metric-value { font-size: 28px; font-weight: 600; margin: 4px 0; }
    .metric-label { color: rgba(0,0,0,0.6); font-size: 12px; text-align: center; }
    .critical { border-left: 4px solid #d32f2f; }
    .warning { border-left: 4px solid #f57c00; }
    .danger { border-left: 4px solid #9e9e9e; }
    .overdue { border-left: 4px solid #e65100; }
  `],
})
export class MetricCardComponent {
  @Input() icon = 'info';
  @Input() iconColor = '#5c6bc0';
  @Input() value: string | number = 0;
  @Input() label = '';
  @Input() link: string | null = null;
  @Input() variant: 'default' | 'critical' | 'warning' | 'danger' | 'overdue' = 'default';
  @Input() ariaLabel: string | null = null;
}
