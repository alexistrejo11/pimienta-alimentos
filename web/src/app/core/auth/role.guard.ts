import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { SessionContextService } from './session-context.service';
import type { AppRole } from '../model/account/enums';

/**
 * Verifica que el usuario tenga al menos uno de los roles en `route.data['roles']`.
 * Redirige al panel si no cumple.
 */
export const roleGuard: CanActivateFn = (route, state) => {
  const session = inject(SessionContextService);
  const router = inject(Router);
  const required = route.data['access']?.['roles'] as AppRole[] | undefined;

  if (!required?.length) {
    return true;
  }

  return session.ensureLoaded().pipe(
    map(() => {
      const allowed = required.some((role) => session.roles().includes(role));
      return allowed
        ? true
        : router.createUrlTree(['/app/acceso-restringido'], {
            queryParams: { returnUrl: state.url },
          });
    }),
  );
};
