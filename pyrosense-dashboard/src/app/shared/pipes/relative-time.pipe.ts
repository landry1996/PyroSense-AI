import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'relativeTime', standalone: true })
export class RelativeTimePipe implements PipeTransform {
  transform(value: string | null): string {
    if (!value) return '';
    const now = Date.now();
    const date = new Date(value).getTime();
    const diff = now - date;

    const minutes = Math.floor(diff / 60000);
    if (minutes < 1) return 'A l\'instant';
    if (minutes < 60) return `Il y a ${minutes} min`;

    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `Il y a ${hours}h`;

    const days = Math.floor(hours / 24);
    if (days < 7) return `Il y a ${days}j`;

    return new Date(value).toLocaleDateString('fr-FR');
  }
}
