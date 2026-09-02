import { isPlatformBrowser } from '@angular/common';
import { HttpInterceptorFn } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';

function isPublicAuthApiUrl(url: string): boolean {
  return url.includes('/api/v1/auth/');
}

/**
 * Añade {@code Authorization: Bearer …} con el access token de {@code sessionStorage}
 * a las peticiones HTTP (excepto si el caller ya envió {@code Authorization}).
 */
export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {
  const platformId = inject(PLATFORM_ID);
  if (!isPlatformBrowser(platformId)) {
    return next(req);
  }
  if (isPublicAuthApiUrl(req.url)) {
    return next(req);
  }
  const token = sessionStorage.getItem('accessToken');
  if (!token || req.headers.has('Authorization')) {
    return next(req);
  }
  return next(
    req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    }),
  );
};
