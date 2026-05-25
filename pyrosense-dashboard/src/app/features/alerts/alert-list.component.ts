import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-alert-list',
  standalone: true,
  imports: [CommonModule],
  template: `<h1>Alertes</h1><p>Liste des alertes (Sprint 3)</p>`,
})
export class AlertListComponent {}
