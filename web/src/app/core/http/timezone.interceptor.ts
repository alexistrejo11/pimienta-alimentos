import { HttpInterceptorFn } from '@angular/common/http';

import { clientZone } from '../i18n/client-zone';

/** Sends the client IANA zone on every API call, including auth and server render. */
export const timezoneInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.includes('/api/')) {
    return next(req);
  }
  return next(
    req.clone({
      setHeaders: { 'X-Timezone': clientZone() },
    }),
  );
};
