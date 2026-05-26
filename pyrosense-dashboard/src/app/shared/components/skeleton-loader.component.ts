import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="skeleton-container" [attr.aria-label]="'Chargement en cours'" role="status">
      @switch (type) {
        @case ('card') {
          <div class="skeleton-card">
            @for (i of rows; track i) {
              <div class="skeleton-line" [class.title]="i === 0" [class.short]="i === rows.length - 1"></div>
            }
          </div>
        }
        @case ('table') {
          @for (i of rows; track i) {
            <div class="skeleton-row">
              @for (j of cols; track j) {
                <div class="skeleton-cell"></div>
              }
            </div>
          }
        }
        @case ('stat') {
          <div class="skeleton-stat">
            <div class="skeleton-circle"></div>
            <div class="skeleton-line short"></div>
            <div class="skeleton-line tiny"></div>
          </div>
        }
        @default {
          <div class="skeleton-line"></div>
        }
      }
    </div>
  `,
  styles: [`
    .skeleton-container { width: 100%; }
    .skeleton-line {
      height: 16px; border-radius: 4px; margin-bottom: 12px;
      background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
      background-size: 200% 100%;
      animation: shimmer 1.5s infinite;
    }
    .skeleton-line.title { height: 24px; width: 60%; }
    .skeleton-line.short { width: 40%; }
    .skeleton-line.tiny { width: 25%; }
    .skeleton-card { padding: 16px; border: 1px solid #f0f0f0; border-radius: 8px; }
    .skeleton-row { display: flex; gap: 16px; margin-bottom: 12px; }
    .skeleton-cell { flex: 1; height: 16px; border-radius: 4px; background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; }
    .skeleton-stat { display: flex; flex-direction: column; align-items: center; padding: 20px; }
    .skeleton-circle { width: 36px; height: 36px; border-radius: 50%; margin-bottom: 8px; background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; }
    @keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }
  `],
})
export class SkeletonLoaderComponent {
  @Input() type: 'card' | 'table' | 'stat' | 'line' = 'line';
  @Input() count = 3;
  @Input() columns = 4;

  get rows(): number[] { return Array.from({ length: this.count }, (_, i) => i); }
  get cols(): number[] { return Array.from({ length: this.columns }, (_, i) => i); }
}
