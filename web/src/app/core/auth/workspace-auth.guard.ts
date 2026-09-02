import { isPlatformBrowser } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

/**
 * Exige access token en {@code sessionStorage} para rutas bajo {@code /app}.
 * En SSR deja pasar (el shell se hidrata; el cliente redirige si hace falta).
 */
export const workspaceAuthGuard: CanActivateFn = (_route, state) => {
  const platformId = inject(PLATFORM_ID);
  const router = inject(Router);
  if (!isPlatformBrowser(platformId)) {
    return true;
  }
  const token = sessionStorage.getItem('accessToken');
  if (token) {
    return true;
  }
  return router.createUrlTree(['/auth/login'], {
    queryParams: { returnUrl: state.url },
  });
};
