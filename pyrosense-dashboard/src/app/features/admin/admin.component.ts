import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule],
  template: `<h1>Administration</h1><p>Gestion utilisateurs et paramètres (Sprint 6)</p>`,
})
export class AdminComponent {}
