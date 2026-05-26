import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <div class="empty-state" role="status">
      <mat-icon class="empty-icon">{{ icon }}</mat-icon>
      <h3>{{ title }}</h3>
      <p>{{ message }}</p>
    </div>
  `,
  styles: [`
    .empty-state { display: flex; flex-direction: column; align-items: center; padding: 48px 24px; text-align: center; }
    .empty-icon { font-size: 64px; width: 64px; height: 64px; color: #bdbdbd; margin-bottom: 16px; }
    h3 { margin: 0 0 8px; color: #424242; font-weight: 500; }
    p { margin: 0; color: #757575; max-width: 400px; }
  `],
})
export class EmptyStateComponent {
  @Input() icon = 'inbox';
  @Input() title = 'Aucune donnee';
  @Input() message = '';
}
