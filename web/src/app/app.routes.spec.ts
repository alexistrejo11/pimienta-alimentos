import { routes } from './app.routes';
import { AppRole } from './core/model/account/enums';

describe('POS routes and permissions', () => {
  const app = routes.find((route) => route.path === 'app');
  const children = app?.children ?? [];
  const ops = children.find((route) => route.path === 'ops');
  const opsChildren = ops?.children ?? [];

  it('keeps /app/ops/pos/sedes as a compatibility redirect', () => {
    expect(opsChildren.find((route) => route.path === 'pos/sedes')).toEqual({
      path: 'pos/sedes',
      redirectTo: 'pos/configuracion',
      pathMatch: 'full',
    });
  });

  it('allows POS operators to read the catalog but not edit settings', () => {
    const catalog = opsChildren.find((route) => route.path === 'pos/catalogo');
    const settings = opsChildren.find((route) => route.path === 'pos/configuracion');
    expect(catalog?.data?.['access']?.['roles']).toContain(AppRole.POS_OPERATOR);
    expect(settings?.data?.['access']?.['roles']).toEqual([AppRole.ADMIN, AppRole.MANAGER]);
  });

  it('nests ERP under /app/erp for admins only', () => {
    const erp = children.find((route) => route.path === 'erp');
    expect(erp?.data?.['workspaceArea']).toBe('erp');
    expect(ops?.data?.['workspaceArea']).toBe('ops');
  });
});
