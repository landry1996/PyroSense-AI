import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MAT_SNACK_BAR_DATA, MatSnackBarRef } from '@angular/material/snack-bar';
import { RouterModule } from '@angular/router';

export interface LiveAlertData {
  alertId: string;
  severity: string;
  title: string;
  type: string;
}

@Component({
  selector: 'app-live-alert-toast',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, RouterModule],
  template: `
    <div class="toast-content" [class]="'severity-' + data.severity.toLowerCase()">
      <mat-icon class="toast-icon">{{ getIcon() }}</mat-icon>
      <div class="toast-text">
        <span class="toast-title">{{ data.title }}</span>
        <span class="toast-type">{{ data.type }}</span>
      </div>
      <a mat-icon-button [routerLink]="['/alerts', data.alertId]" (click)="dismiss()" aria-label="Voir l'alerte">
        <mat-icon>open_in_new</mat-icon>
      </a>
      <button mat-icon-button (click)="dismiss()" aria-label="Fermer">
        <mat-icon>close</mat-icon>
      </button>
    </div>
  `,
  styles: [`
    .toast-content {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 4px 0;
    }
    .toast-icon { font-size: 24px; width: 24px; height: 24px; }
    .toast-text { display: flex; flex-direction: column; flex: 1; }
    .toast-title { font-weight: 600; font-size: 14px; }
    .toast-type { font-size: 12px; opacity: 0.8; }
    .severity-critical .toast-icon { color: #d32f2f; }
    .severity-warning .toast-icon { color: #f57c00; }
    .severity-info .toast-icon { color: #1976d2; }
  `],
})
export class LiveAlertToastComponent {
  constructor(
    @Inject(MAT_SNACK_BAR_DATA) public data: LiveAlertData,
    private snackBarRef: MatSnackBarRef<LiveAlertToastComponent>,
  ) {}

  getIcon(): string {
    switch (this.data.severity.toUpperCase()) {
      case 'CRITICAL': return 'error';
      case 'WARNING': return 'warning';
      default: return 'info';
    }
  }

  dismiss(): void {
    this.snackBarRef.dismiss();
  }
}
