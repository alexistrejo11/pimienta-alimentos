import { ApplicationConfig, ErrorHandler, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideRouter, withInMemoryScrolling } from '@angular/router';

import { authSessionInterceptor } from './core/http/auth-session.interceptor';
import { authTokenInterceptor } from './core/http/auth-token.interceptor';
import { apiLoggingInterceptor } from './core/http/api-logging.interceptor';
import { CentralErrorHandler } from './core/telemetry/web-telemetry.service';
import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';

export const appConfig: ApplicationConfig = {
  providers: [
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
