import { posCatalogItem, posCategory, product } from '../../data/catalog';
import { corsHeaders, paged } from '../../mock-api/mock-api';
import { ListSearchField } from '../../pages/components/list-search-field.po';
import { expect, test } from '../../fixtures/workspace';

test.describe('Catálogo POS', () => {
  test('Productos, sede, búsqueda y diálogo Agregar producto', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/headquarters/10/pos-categories', { json: [posCategory()] });
    mockApi.stub('GET', '/headquarters/10/pos-catalog', { json: paged([posCatalogItem()]) });
    mockApi.stub('GET', '/products', { json: paged([product()]) });
    mockApi.stub('GET', '/headquarters/10/pos-catalog/50', async (route, request) => {
      await route.fulfill({
        status: 404,
        contentType: 'application/json',
        headers: corsHeaders(request),
        body: JSON.stringify({ errorCode: 'NOT_FOUND', message: 'No está', traceId: 'e2e' }),
      });
    });

    await page.goto('/app/ops/pos/catalogo');
    await expect(page.getByRole('heading', { name: 'Productos' })).toBeVisible();
    await page.getByRole('combobox', { name: 'Sede a configurar' }).selectOption({ label: 'Sede Centro' });
    await expect(page.getByText('Salsa verde')).toBeVisible();

    const search = mockApi.nextCall('GET', '/headquarters/10/pos-catalog');
    await new ListSearchField(page).search('salsa');
    expect((await search).query.search).toBe('salsa');

    await page.getByRole('button', { name: 'Agregar producto' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.getByRole('heading', { name: 'Agregar producto' })).toBeVisible();
  });
});
