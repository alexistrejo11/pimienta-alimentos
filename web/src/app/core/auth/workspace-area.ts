import { AppRole, type AccountStatus } from '../model/account/enums';

export type WorkspaceArea = 'erp' | 'ops';

export const HUB_PATH = '/app';
export const ERP_PATH = '/app/erp';
export const OPS_PATH = '/app/ops';
export const ERP_HOME = '/app/erp/dashboard';
export const OPS_HOME = '/app/ops/dashboard';
export const PENDING_PATH = '/app/pendiente-aprobacion';
export const RESTRICTED_PATH = '/app/acceso-restringido';
export const EMPLOYEE_HOME = '/app/mi-asistencia';

const OPS_ROLES: readonly AppRole[] = [
  AppRole.ADMIN,
  AppRole.DIRECTOR,
  AppRole.MANAGER,
  AppRole.EMPLOYEE,
];

export interface WorkspaceIdentity {
  readonly roles: readonly AppRole[];
  readonly accountStatus: AccountStatus | null;
}

export function isPendingAccount(identity: WorkspaceIdentity): boolean {
  return (
    identity.accountStatus === 'PENDING_APPROVAL' ||
    (identity.roles.length === 1 && identity.roles[0] === AppRole.USER)
  );
}

export function hasOpsAccess(roles: readonly AppRole[]): boolean {
  return OPS_ROLES.some((role) => roles.includes(role));
}

export function hasErpAccess(roles: readonly AppRole[]): boolean {
  return roles.includes(AppRole.ADMIN);
}

export function isEmployeeOnly(roles: readonly AppRole[]): boolean {
  return roles.includes(AppRole.EMPLOYEE) && !hasOpsAccess(roles);
}

/** Destino tras login o al abrir `/app` sin elegir área. */
export function postLoginPath(identity: WorkspaceIdentity): string {
  if (isPendingAccount(identity)) {
    return PENDING_PATH;
  }
  if (hasErpAccess(identity.roles)) {
    return HUB_PATH;
  }
  if (hasOpsAccess(identity.roles)) {
    return OPS_HOME;
  }
  if (isEmployeeOnly(identity.roles)) {
    return EMPLOYEE_HOME;
  }
  return RESTRICTED_PATH;
}

export function isSafeAppUrl(url: string): boolean {
  return url.startsWith('/') && !url.startsWith('//') && !url.includes(':');
}

/**
 * Si el returnUrl apunta a un área que el rol no puede ver, cae al home de ese usuario.
 * Las rutas legacy (`/app/inventario`, …) las resuelven los redirects del router.
 */
export function coerceWorkspaceUrl(url: string | null | undefined, identity: WorkspaceIdentity): string {
  const home = postLoginPath(identity);
  if (!url || !isSafeAppUrl(url)) {
    return home;
  }
  if (isPendingAccount(identity)) {
    return PENDING_PATH;
  }
  if (url === HUB_PATH || url === `${HUB_PATH}/`) {
    return hasErpAccess(identity.roles) ? HUB_PATH : home;
  }
  if (url.startsWith(`${ERP_PATH}/`) || url === ERP_PATH) {
    return hasErpAccess(identity.roles) ? url : home;
  }
  if (url.startsWith(`${OPS_PATH}/`) || url === OPS_PATH) {
    return hasOpsAccess(identity.roles) ? url : home;
  }
  if (url.startsWith(EMPLOYEE_HOME)) {
    return isEmployeeOnly(identity.roles) || hasErpAccess(identity.roles) ? url : home;
  }
  return url;
}
