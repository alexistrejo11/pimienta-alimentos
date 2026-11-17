import { warehouseItem } from '../../data/catalog';
import { paged } from '../../mock-api/mock-api';
import { ListSearchField } from '../../pages/components/list-search-field.po';
import { expect, test } from '../../fixtures/workspace';

test.describe('Bodega', () => {
  test('listado kind=WAREHOUSE, búsqueda y create payload', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/inventory/items', { json: paged([warehouseItem()]) });
    mockApi.stub('POST', '/inventory/items', { status: 201, json: warehouseItem({ id: 81, name: 'Vinagre' }) });

    await page.goto('/app/ops/bodega');
    await expect(page.getByRole('heading', { name: 'Artículos de bodega' })).toBeVisible();
    await expect(page.getByText('Aceite 20L')).toBeVisible();
    expect(mockApi.lastCall('GET', '/inventory/items')?.query.kind).toBe('WAREHOUSE');

    const search = mockApi.nextCall('GET', '/inventory/items');
    await new ListSearchField(page).search('aceite');
    const searchCall = await search;
    expect(searchCall.query.search).toBe('aceite');
    expect(searchCall.query.kind).toBe('WAREHOUSE');

    await page.getByRole('link', { name: 'Nuevo artículo' }).click();
    await expect(page.getByRole('heading', { name: 'Nuevo artículo de bodega' })).toBeVisible();
    await page.getByRole('textbox').first().fill('Vinagre');
    const post = mockApi.nextCall('POST', '/inventory/items');
    await page.getByRole('button', { name: 'Guardar' }).click();
    expect((await post).json).toMatchObject({ name: 'Vinagre', category: 'RAW_MATERIAL', unit: 'PIECE' });
    await expect(page).toHaveURL(/\/app\/ops\/bodega$/);
  });
});
