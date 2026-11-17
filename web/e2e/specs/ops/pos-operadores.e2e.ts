import { posOperator } from '../../data/pos';
import { corsHeaders, paged } from '../../mock-api/mock-api';
import { Pagination } from '../../pages/components/pagination.po';
import { expect, test } from '../../fixtures/workspace';

test.describe('Operadores POS', () => {
  test('paginación 20 e inline create', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/pos/admin/operators', async (route, request) => {
      const url = new URL(request.url());
      const pageNo = Number(url.searchParams.get('page') ?? '0');
      const items =
        pageNo === 0
          ? Array.from({ length: 20 }, (_, i) => posOperator({ id: i + 1, displayName: `Op ${i + 1}` }))
          : [posOperator({ id: 21, displayName: 'Op 21' })];
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        headers: corsHeaders(request),
        body: JSON.stringify(paged(items, pageNo, 20, 21)),
      });
    });
    mockApi.stub('POST', '/pos/admin/operators', {
      json: posOperator({ id: 99, displayName: 'Nueva caja' }),
    });

    await page.goto('/app/ops/pos/operadores');
    await expect(page.getByRole('heading', { name: 'Operadores POS' })).toBeVisible();
    await expect(page.getByRole('cell', { name: 'Op 1', exact: true })).toBeVisible();
    await new Pagination(page).next().click();
    await expect(page.getByRole('cell', { name: 'Op 21', exact: true })).toBeVisible();

    await page.getByRole('button', { name: 'Nuevo operador' }).click();
    await page.getByText('Nombre visible').locator('..').getByRole('textbox').fill('Nueva caja');
    await page.getByText('PIN', { exact: false }).locator('..').getByRole('textbox').fill('1234');
    await page.getByRole('checkbox', { name: 'Sede Centro' }).check();
    const post = mockApi.nextCall('POST', '/pos/admin/operators');
    await page.getByRole('button', { name: 'Crear operador' }).click();
    const body = (await post).json as { displayName: string; headquarterIds: number[]; pin: string };
    expect(body.displayName).toBe('Nueva caja');
    expect(body.headquarterIds).toEqual([10]);
    expect(body.pin).toBe('1234');
  });
});
