import { payrollRecord, payrollSummary } from '../../data/erp';
import { corsHeaders, paged } from '../../mock-api/mock-api';
import { Pagination } from '../../pages/components/pagination.po';
import { expect, test } from '../../fixtures/workspace';

test.describe('Nómina', () => {
  test('summary, registros, paginación y Aplicar filtros', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/payroll/summary', { json: payrollSummary() });
    mockApi.stub('GET', '/payroll/records', async (route, request) => {
      const url = new URL(request.url());
      const pageNo = Number(url.searchParams.get('page') ?? '0');
      const items =
        pageNo === 0
          ? Array.from({ length: 20 }, (_, i) => payrollRecord({ id: i + 1 }))
          : [payrollRecord({ id: 21 })];
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        headers: corsHeaders(request),
        body: JSON.stringify(paged(items, pageNo, 20, 21)),
      });
    });

    await page.goto('/app/erp/nomina');
    await expect(page.getByRole('heading', { name: 'Nómina' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Aplicar filtros' })).toBeVisible();
    expect(mockApi.lastCall('GET', '/payroll/summary')).toBeTruthy();
    await expect(page.getByRole('button', { name: 'Siguiente' })).toBeVisible();
    await new Pagination(page).next().click();
    await expect.poll(() => mockApi.lastCall('GET', '/payroll/records')?.query.page).toBe('1');

    const filtered = mockApi.nextCall('GET', '/payroll/records');
    await page.getByRole('button', { name: 'Aplicar filtros' }).click();
    await filtered;
  });
});
