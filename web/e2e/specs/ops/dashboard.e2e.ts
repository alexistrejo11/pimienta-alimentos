import { posSummaryRow } from '../../data/pos';
import { expect, loginAs, test } from '../../fixtures/workspace';

test.describe('Dashboard operaciones', () => {
  test('admin ve POS y gastos', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/pos/admin/reports/summary', { json: { rows: [posSummaryRow()] } });
    mockApi.stub('GET', '/operating-expenses/summary', {
      json: { totalAmount: 80, topCategory: null, byDay: [] },
    });
    mockApi.stub('GET', '/inventory/dashboard', {
      json: { skuCount: 4, lowStockCount: 1, outOfStockCount: 0, totalAvailableQuantity: 10, totalStockValue: 100 },
    });
    await page.goto('/app/ops/dashboard');
    await expect(page.getByRole('heading', { name: 'Operaciones' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Punto de venta — hoy' })).toBeVisible();
    await expect(page.getByRole('main').getByText('Gastos operativos', { exact: true })).toBeVisible();
    await expect(page.getByRole('main').getByRole('heading', { name: 'Inventario' })).toBeVisible();
  });

  test('gerente solo inventario, sin tiles de ventas', async ({ page, mockApi }) => {
    await loginAs(mockApi, 'manager');
    mockApi.stub('GET', '/inventory/dashboard', {
      json: { skuCount: 2, lowStockCount: 0, outOfStockCount: 0, totalAvailableQuantity: 5, totalStockValue: 20 },
    });
    await page.goto('/app/ops/dashboard');
    await expect(page.getByRole('main').getByRole('heading', { name: 'Inventario' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Punto de venta — hoy' })).toHaveCount(0);
    await expect(page.getByText('Ventas', { exact: true })).toHaveCount(0);
  });
});
