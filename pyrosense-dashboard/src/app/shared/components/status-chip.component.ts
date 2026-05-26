import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-status-chip',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="chip" [class]="status.toLowerCase()">{{ label || status }}</span>
  `,
  styles: [`
    .chip {
      display: inline-block;
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 11px;
      font-weight: 500;
      text-transform: uppercase;
      letter-spacing: 0.3px;
    }
    .open { background: #e3f2fd; color: #1565c0; }
    .acknowledged, .planned { background: #fff3e0; color: #e65100; }
    .in_progress, .in-progress, .assigned { background: #e8f5e9; color: #2e7d32; }
    .resolved, .completed { background: #f1f8e9; color: #33691e; }
    .closed, .cancelled { background: #f5f5f5; color: #616161; }
    .created { background: #ede7f6; color: #4527a0; }
    .active { background: #e8f5e9; color: #2e7d32; }
    .offline { background: #ffebee; color: #c62828; }
    .provisioned, .registered { background: #e3f2fd; color: #1565c0; }
    .revoked { background: #fce4ec; color: #880e4f; }
    .overdue { background: #fff3e0; color: #bf360c; }
  `],
})
export class StatusChipComponent {
  @Input() status = '';
  @Input() label = '';
}
