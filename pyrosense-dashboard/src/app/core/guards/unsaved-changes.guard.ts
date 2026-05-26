import { CanDeactivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { map } from 'rxjs';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog.component';

export interface HasUnsavedChanges {
  hasUnsavedChanges(): boolean;
}

export const unsavedChangesGuard: CanDeactivateFn<HasUnsavedChanges> = (component) => {
  if (component.hasUnsavedChanges && component.hasUnsavedChanges()) {
    const dialog = inject(MatDialog);
    return dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Modifications non sauvegardees',
        message: 'Vous avez des modifications non sauvegardees. Voulez-vous quitter cette page ?',
        confirmLabel: 'Quitter',
        cancelLabel: 'Rester',
        icon: 'warning',
        color: 'warn',
      },
    }).afterClosed().pipe(map(result => result === true));
  }
  return true;
};
