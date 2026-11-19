import { isPlatformBrowser } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { authTokenStorage } from './auth-token.storage';
import { SERVICE_UNAVAILABLE_PATH, sessionAuthPolicy } from './session-auth.policy';
import { SessionContextService } from './session-context.service';

/**
 * Exige access token en {@code localStorage} para rutas bajo {@code /app}.
 * En SSR deja pasar (el shell se hidrata; el cliente redirige si hace falta).
 * Solo cierra sesión ante fallo de auth explícito; red/5xx van a recuperación sin borrar tokens.
 */
export const workspaceAuthGuard: CanActivateFn = (_route, state) => {
  const platformId = inject(PLATFORM_ID);
  const router = inject(Router);
  const session = inject(SessionContextService);
  if (!isPlatformBrowser(platformId)) {
    return true;
  }
  const token = authTokenStorage.getAccessToken();
  if (token) {
    return session.ensureLoaded().pipe(
      map((context) => {
        if (
          context.accountStatus === 'PENDING_APPROVAL' ||
          (context.roles.length === 1 && context.roles[0] === 'USER')
        ) {
          return state.url === '/app/pendiente-aprobacion'
            ? true
            : router.createUrlTree(['/app/pendiente-aprobacion']);
        }
        return true;
      }),
      catchError((err: unknown) => {
        if (sessionAuthPolicy.requiresSessionClear(err)) {
          authTokenStorage.clear();
          session.clear();
          return of(
            router.createUrlTree(['/auth/login'], { queryParams: { returnUrl: state.url } }),
          );
        }
        return of(
          router.createUrlTree([SERVICE_UNAVAILABLE_PATH], {
            queryParams: { returnUrl: state.url },
          }),
        );
      }),
    );
  }
  return router.createUrlTree(['/auth/login'], {
    queryParams: { returnUrl: state.url },
  });
};
