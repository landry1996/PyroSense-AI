import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-device-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `<span class="badge" [class]="status.toLowerCase()">{{ label }}</span>`,
  styles: [`
    .badge {
      display: inline-block;
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
      text-transform: uppercase;
    }
    .active { background: #e8f5e9; color: #2e7d32; }
    .offline { background: #fce4ec; color: #c62828; }
    .registered { background: #e3f2fd; color: #1565c0; }
    .provisioned { background: #fff3e0; color: #e65100; }
    .maintenance { background: #f3e5f5; color: #6a1b9a; }
    .revoked { background: #efebe9; color: #4e342e; }
  `],
})
export class DeviceStatusBadgeComponent {
  @Input() status: string = 'REGISTERED';

  get label(): string {
    const labels: Record<string, string> = {
      ACTIVE: 'Actif',
      OFFLINE: 'Hors ligne',
      REGISTERED: 'Enregistre',
      PROVISIONED: 'Provisionne',
      MAINTENANCE: 'Maintenance',
      REVOKED: 'Revoque',
    };
    return labels[this.status] || this.status;
  }
}
