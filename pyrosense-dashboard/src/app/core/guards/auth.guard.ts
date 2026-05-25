import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';

export const authGuard: CanActivateFn = async () => {
  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  const isLoggedIn = keycloak.isLoggedIn();

  if (!isLoggedIn) {
    await keycloak.login({ redirectUri: window.location.href });
    return false;
  }

  return true;
};
