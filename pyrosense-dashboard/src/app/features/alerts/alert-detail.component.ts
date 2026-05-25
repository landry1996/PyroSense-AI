import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-alert-detail',
  standalone: true,
  imports: [CommonModule],
  template: `<h1>Détail Alerte</h1><p>Détail de l'alerte (Sprint 3)</p>`,
})
export class AlertDetailComponent {}
