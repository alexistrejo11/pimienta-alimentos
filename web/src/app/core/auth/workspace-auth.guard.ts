import { isPlatformBrowser } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { SessionContextService } from './session-context.service';

/**
 * Exige access token en {@code sessionStorage} para rutas bajo {@code /app}.
 * En SSR deja pasar (el shell se hidrata; el cliente redirige si hace falta).
 */
export const workspaceAuthGuard: CanActivateFn = (_route, state) => {
  const platformId = inject(PLATFORM_ID);
  const router = inject(Router);
  const session = inject(SessionContextService);
  if (!isPlatformBrowser(platformId)) {
    return true;
  }
  const token = sessionStorage.getItem('accessToken');
  if (token) {
    return session.ensureLoaded().pipe(
      map((context) => {
        if (context.accountStatus === 'PENDING_APPROVAL' ||
            (context.roles.length === 1 && context.roles[0] === 'USER')) {
          return state.url === '/app/pendiente-aprobacion'
            ? true
            : router.createUrlTree(['/app/pendiente-aprobacion']);
        }
        return true;
      }),
      catchError(() => {
        session.clear();
        return of(router.createUrlTree(['/auth/login'], { queryParams: { returnUrl: state.url } }));
      }),
    );
  }
  return router.createUrlTree(['/auth/login'], {
    queryParams: { returnUrl: state.url },
  });
};
