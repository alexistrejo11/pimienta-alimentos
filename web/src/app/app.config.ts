import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, ErrorHandler, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import localeEsMx from '@angular/common/locales/es-MX';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { provideRouter, withInMemoryScrolling } from '@angular/router';

import { routes } from './app.routes';
import { apiLoggingInterceptor } from './core/http/api-logging.interceptor';
import { authSessionInterceptor } from './core/http/auth-session.interceptor';
import { authTokenInterceptor } from './core/http/auth-token.interceptor';
import { CentralErrorHandler } from './core/telemetry/web-telemetry.service';

registerLocaleData(localeEsMx);

export const appConfig: ApplicationConfig = {
  providers: [
    { provide: LOCALE_ID, useValue: 'es-MX' },
    provideBrowserGlobalErrorListeners(),
    { provide: ErrorHandler, useClass: CentralErrorHandler },
    provideHttpClient(
      withFetch(),
      withInterceptors([authSessionInterceptor, authTokenInterceptor, apiLoggingInterceptor]),
    ),
    provideRouter(
      routes,
      withInMemoryScrolling({
        scrollPositionRestoration: 'enabled',
        anchorScrolling: 'enabled',
      }),
    ),
    provideClientHydration(withEventReplay()),
  ],
};
