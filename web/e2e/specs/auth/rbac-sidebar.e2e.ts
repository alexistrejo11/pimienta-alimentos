import { WorkspaceSidebar } from '../../pages/components/workspace-sidebar.po';
import { expect, loginAs, test } from '../../fixtures/workspace';

test.describe('RBAC sidebar', () => {
  test('ADMIN ve Oficina, Ventas y Productos maestros', async ({ page, mockApi }) => {
    void mockApi;
    await page.goto('/app/ops/dashboard');
    const nav = new WorkspaceSidebar(page);
    await expect(nav.areaToggle()).toBeVisible();
    await nav.expandSection('Punto de venta');
    await expect(nav.link('Ventas')).toBeVisible();
    await nav.expandSection('Inventario');
    await expect(nav.link('Productos maestros')).toBeVisible();
    await expect(nav.link('Existencias')).toBeVisible();
  });

  test('DIRECTOR no toggle Oficina ni maestros, sí Ventas', async ({ page, mockApi }) => {
    await loginAs(mockApi, 'director');
    await page.goto('/app/ops/dashboard');
    const nav = new WorkspaceSidebar(page);
    await expect(nav.areaToggle()).toHaveCount(0);
    await expect(nav.link('Productos maestros')).toHaveCount(0);
    await nav.expandSection('Punto de venta');
    await expect(nav.link('Ventas')).toBeVisible();
  });

  test('MANAGER y EMPLOYEE no ven Ventas', async ({ page, mockApi }) => {
    await loginAs(mockApi, 'manager');
    await page.goto('/app/ops/dashboard');
    const nav = new WorkspaceSidebar(page);
    await nav.expandSection('Punto de venta');
    await expect(nav.link('Ventas')).toHaveCount(0);
    await nav.expandSection('Inventario');
    await expect(nav.link('Existencias')).toBeVisible();

    await loginAs(mockApi, 'employee');
    await page.goto('/app/ops/dashboard');
    await nav.expandSection('Punto de venta');
    await expect(nav.link('Ventas')).toHaveCount(0);
  });
});
