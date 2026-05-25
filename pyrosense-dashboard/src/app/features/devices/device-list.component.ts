import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-device-list',
  standalone: true,
  imports: [CommonModule],
  template: `<h1>Capteurs</h1><p>Liste des capteurs (Sprint 2)</p>`,
})
export class DeviceListComponent {}
