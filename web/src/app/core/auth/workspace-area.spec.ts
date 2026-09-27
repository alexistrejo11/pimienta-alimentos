import { AppRole } from '../model/account/enums';
import { coerceWorkspaceUrl, postLoginPath } from './workspace-area';

describe('workspace area routing', () => {
  it('sends admins to the hub and managers to ops', () => {
    expect(postLoginPath({ roles: [AppRole.ADMIN], accountStatus: 'ACTIVE' })).toBe('/app');
    expect(postLoginPath({ roles: [AppRole.DIRECTOR], accountStatus: 'ACTIVE' })).toBe(
      '/app/ops/dashboard',
    );
    expect(postLoginPath({ roles: [AppRole.MANAGER], accountStatus: 'ACTIVE' })).toBe(
      '/app/ops/dashboard',
    );
    expect(postLoginPath({ roles: [AppRole.EMPLOYEE], accountStatus: 'ACTIVE' })).toBe(
      '/app/ops/dashboard',
    );
  });

  it('blocks managers from ERP return URLs', () => {
    expect(
      coerceWorkspaceUrl('/app/erp/empleados', {
        roles: [AppRole.MANAGER],
        accountStatus: 'ACTIVE',
      }),
    ).toBe('/app/ops/dashboard');
    expect(
      coerceWorkspaceUrl('/app/erp/dashboard', {
        roles: [AppRole.ADMIN],
        accountStatus: 'ACTIVE',
      }),
    ).toBe('/app/erp/dashboard');
  });
});
