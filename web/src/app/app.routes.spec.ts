import { routes } from './app.routes';
import { AppRole } from './core/model/account/enums';

describe('POS routes and permissions', () => {
  const app = routes.find((route) => route.path === 'app');
  const children = app?.children ?? [];

  it('keeps /app/pos/sedes as a compatibility redirect', () => {
    expect(children.find((route) => route.path === 'pos/sedes')).toEqual({
      path: 'pos/sedes',
      redirectTo: 'pos/configuracion',
      pathMatch: 'full',
    });
  });

  it('allows POS operators to read the catalog but not edit settings', () => {
    const catalog = children.find((route) => route.path === 'pos/catalogo');
    const settings = children.find((route) => route.path === 'pos/configuracion');
    expect(catalog?.data?.['access']?.['roles']).toContain(AppRole.POS_OPERATOR);
    expect(settings?.data?.['access']?.['roles']).toEqual([AppRole.ADMIN, AppRole.MANAGER]);
  });
});
