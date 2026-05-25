import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TenantService } from '../services/tenant.service';

export const tenantInterceptor: HttpInterceptorFn = (req, next) => {
  const tenantService = inject(TenantService);
  const tenantId = tenantService.getCurrentTenantId();

  if (tenantId) {
    const tenantReq = req.clone({
      setHeaders: { 'X-Tenant-Id': tenantId },
    });
    return next(tenantReq);
  }

  return next(req);
};
