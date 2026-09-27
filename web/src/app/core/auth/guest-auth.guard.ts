import { isPlatformBrowser } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { authTokenStorage } from './auth-token.storage';
import { SessionContextService } from './session-context.service';
import { isPendingAccount, postLoginPath, PENDING_PATH } from './workspace-area';

/**
 * Keeps login/register off-limits when a workspace JWT is present.
 * Invalid tokens are cleared so the user can sign in again.
 */
export const guestAuthGuard: CanActivateFn = () => {
  const platformId = inject(PLATFORM_ID);
  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  if (!authTokenStorage.getAccessToken()) {
    return true;
  }

  const session = inject(SessionContextService);
  const router = inject(Router);

  return session.ensureLoaded().pipe(
    map((context) => {
      const identity = { roles: context.roles, accountStatus: context.accountStatus };
      if (isPendingAccount(identity)) {
        return router.createUrlTree([PENDING_PATH]);
      }
      return router.createUrlTree([postLoginPath(identity)]);
    }),
    catchError(() => {
      authTokenStorage.clear();
      session.clear();
      return of(true);
    }),
  );
};
