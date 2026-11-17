import { posOperator, posSale } from '../../data/pos';
import { corsHeaders, paged } from '../../mock-api/mock-api';
import { Pagination } from '../../pages/components/pagination.po';
import { expect, loginAs, test } from '../../fixtures/workspace';

test.describe('Ventas POS', () => {
  test('heading, sede, reporte y paginación', async ({ page, mockApi }) => {
    const sales = Array.from({ length: 20 }, (_, i) =>
      posSale({ saleId: `sale-${i + 1}`, folio: `F-${i + 1}` }),
    );
    mockApi.stub('GET', '/pos/admin/operators', { json: paged([posOperator()], 0, 200, 1) });
    mockApi.stub('GET', '/pos/admin/reports/sales', async (route, request) => {
      const url = new URL(request.url());
      const pageNo = Number(url.searchParams.get('page') ?? '0');
      const size = Number(url.searchParams.get('size') ?? '20');
      const items = pageNo === 0 ? sales : [posSale({ saleId: 'sale-21', folio: 'F-21' })];
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        headers: corsHeaders(request),
        body: JSON.stringify(paged(items, pageNo, size, 21)),
      });
    });

    await page.goto('/app/ops/pos/ventas');
    await expect(page.getByRole('heading', { name: 'Ventas POS' })).toBeVisible();
    await expect(page.getByRole('combobox', { name: 'Sede' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'F-1', exact: true })).toBeVisible();
    const salesCall = mockApi.lastCall('GET', '/pos/admin/reports/sales');
    expect(salesCall?.query.headquarterId).toBe('10');

    const pager = new Pagination(page);
    await expect(page.getByText('21 ventas')).toBeVisible();
    await pager.next().click();
    await expect(page.getByRole('button', { name: 'F-21', exact: true })).toBeVisible();
  });

  test('gerente ve acceso restringido', async ({ page, mockApi }) => {
    await loginAs(mockApi, 'manager');
    await page.goto('/app/ops/pos/ventas');
    await expect(page).toHaveURL(/\/app\/acceso-restringido/);
    await expect(page.getByRole('heading', { name: 'Acceso restringido' })).toBeVisible();
  });
});
