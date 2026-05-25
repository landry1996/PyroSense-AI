import { inject } from '@angular/core';
import { CanActivateFn, ActivatedRouteSnapshot, Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  const requiredRoles = route.data['roles'] as string[];
  if (!requiredRoles || requiredRoles.length === 0) {
    return true;
  }

  const userRoles = keycloak.getUserRoles(true);
  const hasRole = requiredRoles.some((role) => userRoles.includes(role));

  if (!hasRole) {
    router.navigate(['/dashboard']);
    return false;
  }

  return true;
};
