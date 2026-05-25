import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-severity-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="badge" [class]="severity.toLowerCase()">{{ severity }}</span>
  `,
  styles: [`
    .badge {
      display: inline-block;
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
      text-transform: uppercase;
    }
    .critical { background: #ffcdd2; color: #c62828; }
    .warning { background: #fff3e0; color: #e65100; }
    .info { background: #e3f2fd; color: #1565c0; }
  `],
})
export class SeverityBadgeComponent {
  @Input() severity: 'CRITICAL' | 'WARNING' | 'INFO' = 'INFO';
}
