import { posCategory } from '../../data/catalog';
import { apiErrorBody } from '../../mock-api/mock-api';
import { expect, test } from '../../fixtures/workspace';

test.describe('POS configuración', () => {
  test('carga vacía si no hay settings y PUT', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/headquarters/10/pos-categories', { json: [posCategory()] });
    mockApi.stub('GET', '/headquarters/10/pos-settings', {
      status: 404,
      json: apiErrorBody('HEADQUARTER_POS_SETTINGS_NOT_FOUND', 'Sin políticas'),
    });
    mockApi.stub('PUT', '/headquarters/10/pos-settings', {
      json: {
        id: 1,
        headquarterId: 10,
        currency: 'MXN',
        catalogStaleWarnHours: 24,
        catalogStaleBlockHours: 72,
        openAmountCategories: [],
        allowOpenProducts: false,
        stockless: false,
        highSaleWarningCentavos: 1000000,
        highChangeWarningCentavos: 100000,
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
        version: 0,
      },
    });

    await page.goto('/app/ops/pos/configuracion');
    await expect(page.getByRole('heading', { name: 'Configuración de monto abierto' })).toBeVisible();
    await page.getByRole('combobox', { name: 'Sede a configurar' }).selectOption({ label: 'Sede Centro' });
    await expect(page.getByRole('alert')).toHaveCount(0);
    const put = mockApi.nextCall('PUT', '/headquarters/10/pos-settings');
    await page.getByRole('button', { name: 'Guardar políticas' }).click();
    expect((await put).json).toMatchObject({ currency: 'MXN' });
  });
});
