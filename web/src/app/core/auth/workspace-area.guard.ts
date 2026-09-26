import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { SessionContextService } from './session-context.service';
import {
  EMPLOYEE_HOME,
  hasErpAccess,
  hasOpsAccess,
  isEmployeeOnly,
  isPendingAccount,
  OPS_HOME,
  PENDING_PATH,
  RESTRICTED_PATH,
} from './workspace-area';

function identityFrom(session: SessionContextService) {
  return {
    roles: session.roles(),
    accountStatus: session.accountStatus(),
  };
}

/** Hub `/app`: admin elige área; director, gerente y empleado van a operaciones. */
export const workspaceHubGuard: CanActivateFn = () => {
  const session = inject(SessionContextService);
  const router = inject(Router);
  return session.ensureLoaded().pipe(
    map(() => {
      const identity = identityFrom(session);
      if (isPendingAccount(identity)) {
        return router.createUrlTree([PENDING_PATH]);
      }
      if (hasErpAccess(identity.roles)) {
        return true;
      }
      if (hasOpsAccess(identity.roles)) {
        return router.createUrlTree([OPS_HOME]);
      }
      if (isEmployeeOnly(identity.roles)) {
        return router.createUrlTree([EMPLOYEE_HOME]);
      }
      return router.createUrlTree([RESTRICTED_PATH]);
    }),
  );
};

/** Oficina (ERP): solo admin. Director, gerente y empleado caen a operaciones. */
export const erpAreaGuard: CanActivateFn = () => {
  const session = inject(SessionContextService);
  const router = inject(Router);
  return session.ensureLoaded().pipe(
    map(() => {
      const identity = identityFrom(session);
      if (isPendingAccount(identity)) {
        return router.createUrlTree([PENDING_PATH]);
      }
      if (hasErpAccess(identity.roles)) {
        return true;
      }
      if (hasOpsAccess(identity.roles)) {
        return router.createUrlTree([OPS_HOME]);
      }
      if (isEmployeeOnly(identity.roles)) {
        return router.createUrlTree([EMPLOYEE_HOME]);
      }
      return router.createUrlTree([RESTRICTED_PATH]);
    }),
  );
};

/** Operaciones (inventario + POS): admin, director, gerente y empleado. */
export const opsAreaGuard: CanActivateFn = () => {
  const session = inject(SessionContextService);
  const router = inject(Router);
  return session.ensureLoaded().pipe(
    map(() => {
      const identity = identityFrom(session);
      if (isPendingAccount(identity)) {
        return router.createUrlTree([PENDING_PATH]);
      }
      if (hasOpsAccess(identity.roles)) {
        return true;
      }
      if (isEmployeeOnly(identity.roles)) {
        return router.createUrlTree([EMPLOYEE_HOME]);
      }
      return router.createUrlTree([RESTRICTED_PATH]);
    }),
  );
};
