import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { SessionContextService } from './session-context.service';

/**
 * Verifica que el usuario tenga al menos uno de los roles en `route.data['roles']`.
 * Redirige al panel si no cumple.
 */
export const roleGuard: CanActivateFn = (route) => {
  const session = inject(SessionContextService);
  const router = inject(Router);
  const required = route.data['roles'] as string[] | undefined;

  if (!required?.length) {
    return true;
  }

  const userRoles = session.roles();
  const allowed = required.some((role) => userRoles.includes(role));
  if (allowed) {
    return true;
  }

  return router.createUrlTree(['/app/dashboard']);
};
