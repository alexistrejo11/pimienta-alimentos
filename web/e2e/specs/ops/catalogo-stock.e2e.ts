import { warehouseItem } from '../../data/catalog';
import { paged } from '../../mock-api/mock-api';
import { expect, test } from '../../fixtures/workspace';

test.describe('Catálogo de stock', () => {
  test('lista y Configurar stock PUT', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/inventory/items', { json: paged([warehouseItem()]) });
    mockApi.stub('PUT', '/inventory/items/80/stock-settings', {
      json: warehouseItem({ costPrice: 150 }),
    });

    await page.goto('/app/ops/catalogo');
    await expect(page.getByRole('heading', { name: 'Catálogo de stock' })).toBeVisible();
    await expect(page.getByText('Aceite 20L')).toBeVisible();
    await page.getByRole('button', { name: 'Configurar stock' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.getByRole('heading', { name: 'Configurar stock' })).toBeVisible();
    await dialog.getByLabel('Precio de costo').fill('150');
    const put = mockApi.nextCall('PUT', '/inventory/items/80/stock-settings');
    await dialog.getByRole('button', { name: 'Guardar cambios' }).click();
    expect((await put).json).toMatchObject({ costPrice: 150 });
  });
});
