import { Injectable, signal, computed } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  roles: string[];
  tenantId: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly profile = signal<UserProfile | null>(null);

  readonly currentUser = computed(() => this.profile());
  readonly isAuthenticated = computed(() => this.profile() !== null);
  readonly userRoles = computed(() => this.profile()?.roles ?? []);

  constructor(private keycloak: KeycloakService) {}

  async loadProfile(): Promise<void> {
    if (!this.keycloak.isLoggedIn()) return;

    const tokenParsed = this.keycloak.getKeycloakInstance()?.tokenParsed;
    if (tokenParsed) {
      this.profile.set({
        id: tokenParsed['sub'] ?? '',
        email: tokenParsed['email'] ?? '',
        fullName: tokenParsed['name'] ?? '',
        roles: this.keycloak.getUserRoles(true),
        tenantId: tokenParsed['tenant_id'] as string ?? '',
      });
    }
  }

  hasRole(role: string): boolean {
    return this.userRoles().includes(role);
  }

  hasAnyRole(...roles: string[]): boolean {
    return roles.some((r) => this.userRoles().includes(r));
  }

  async logout(): Promise<void> {
    await this.keycloak.logout(window.location.origin);
  }
}
