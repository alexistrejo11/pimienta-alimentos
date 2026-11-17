import { product } from '../../data/catalog';
import { apiErrorBody, paged } from '../../mock-api/mock-api';
import { ConfirmDialog } from '../../pages/components/confirm-dialog.po';
import { ListSearchField } from '../../pages/components/list-search-field.po';
import { expect, loginAs, test } from '../../fixtures/workspace';

test.describe('Productos maestros', () => {
  test('lista, buscar y alta sucia', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/products', { json: paged([product()]) });
    await page.goto('/app/ops/productos-maestros');
    await expect(page.getByRole('heading', { name: 'Productos maestros' })).toBeVisible();
    await expect(page.getByText('Salsa verde')).toBeVisible();

    const search = mockApi.nextCall('GET', '/products');
    await new ListSearchField(page).search('salsa');
    expect((await search).query.search).toBe('salsa');

    await page.getByRole('link', { name: 'Nuevo producto' }).click();
    await expect(page.getByRole('heading', { name: 'Nuevo producto maestro' })).toBeVisible();
    await page.getByRole('textbox').first().fill('Nuevo');
    const dialog = ConfirmDialog.once(page, false);
    await page.getByRole('link', { name: 'Cancelar' }).click();
    expect(await dialog).toMatch(/cambios sin guardar/i);
  });

  test('gerente ve acceso restringido', async ({ page, mockApi }) => {
    await loginAs(mockApi, 'manager');
    await page.goto('/app/ops/productos-maestros');
    await expect(page).toHaveURL(/acceso-restringido/);
  });

  test('lista 500', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/products', { status: 500, json: apiErrorBody('INTERNAL', 'Catálogo caído') });
    await page.goto('/app/ops/productos-maestros');
    await expect(page.getByText('Catálogo caído')).toBeVisible();
  });
});
