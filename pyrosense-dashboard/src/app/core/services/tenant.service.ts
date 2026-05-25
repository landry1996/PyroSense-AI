import { Injectable, signal, computed } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

@Injectable({ providedIn: 'root' })
export class TenantService {
  private readonly tenantId = signal<string | null>(null);

  readonly currentTenantId = computed(() => this.tenantId());

  constructor(private keycloak: KeycloakService) {
    this.extractTenantFromToken();
  }

  getCurrentTenantId(): string | null {
    return this.tenantId();
  }

  private extractTenantFromToken(): void {
    try {
      const token = this.keycloak.getKeycloakInstance()?.tokenParsed;
      if (token && token['tenant_id']) {
        this.tenantId.set(token['tenant_id'] as string);
      }
    } catch {
      // Token not yet available
    }
  }
}
