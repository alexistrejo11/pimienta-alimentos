import { contract } from '../../data/erp';
import { paged } from '../../mock-api/mock-api';
import { expect, test } from '../../fixtures/workspace';

test.describe('Contratos', () => {
  test('lista e inline POST /contracts', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/contracts', { json: paged([contract()]) });
    mockApi.stub('POST', '/contracts', { status: 201, json: contract({ id: 5, name: 'Nuevo acuerdo' }) });

    await page.goto('/app/erp/contratos');
    await expect(page.getByRole('heading', { name: 'Contratos' })).toBeVisible();
    await expect(page.getByText('Contrato proveedor aceite')).toBeVisible();
    await page.getByRole('button', { name: 'Nuevo contrato' }).click();
    await page.getByLabel('Nombre del contrato').fill('Nuevo acuerdo');
    await page.getByLabel('URL del documento firmado').fill('https://files.test/n.pdf');
    await page.getByLabel('Inicio vigencia').fill('2026-01-01');
    const post = mockApi.nextCall('POST', '/contracts');
    await page.getByRole('button', { name: 'Guardar contrato' }).click();
    expect((await post).json).toMatchObject({ name: 'Nuevo acuerdo' });
  });
});
