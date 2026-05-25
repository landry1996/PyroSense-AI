import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { KeycloakService } from 'keycloak-angular';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const snackBar = inject(MatSnackBar);
  const keycloak = inject(KeycloakService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      switch (error.status) {
        case 401:
          keycloak.login({ redirectUri: window.location.href });
          break;
        case 403:
          snackBar.open('Acces refuse — droits insuffisants', 'Fermer', { duration: 5000 });
          router.navigate(['/dashboard']);
          break;
        case 429:
          snackBar.open('Trop de requetes — reessayez dans quelques instants', 'Fermer', { duration: 5000 });
          break;
        case 0:
          snackBar.open('Probleme de connexion reseau', 'Fermer', { duration: 5000 });
          break;
        default:
          if (error.status >= 500) {
            const correlationId = error.headers?.get('X-Correlation-Id') || '';
            const msg = correlationId
              ? `Erreur serveur (ref: ${correlationId.slice(0, 8)})`
              : 'Erreur serveur — reessayez ulterieurement';
            snackBar.open(msg, 'Fermer', { duration: 5000 });
          }
      }
      return throwError(() => error);
    })
  );
};
