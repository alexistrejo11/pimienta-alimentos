import { userResponse } from '../../data/users';
import { apiErrorBody } from '../../mock-api/mock-api';
import { SedeFormPage } from '../../pages/ops/sede-form.page';
import { AppRole } from '../../../src/app/core/model/account/enums';
import { expect, test } from '../../fixtures/workspace';

test.describe('Sedes listado y edición', () => {
  test('staff carga GET /headquarters/:id', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/users/me', {
      json: userResponse({
        id: 3,
        roles: [AppRole.MANAGER],
        assignedHeadquarterIds: [10],
        email: 'gerente@pimienta.test',
      }),
    });
    await page.goto('/app/ops/sedes');
    await expect(page.getByRole('heading', { name: 'Sedes' })).toBeVisible();
    await expect(page.getByText('Sede Centro')).toBeVisible();
    expect(mockApi.callsMatching('GET', '/headquarters/10').length).toBeGreaterThan(0);
  });

  test('sin sede asignada muestra aviso', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/users/me', {
      json: userResponse({
        id: 33,
        roles: [AppRole.MANAGER],
        assignedHeadquarterIds: [],
        email: 'sin@pimienta.test',
      }),
    });
    await page.goto('/app/ops/sedes');
    await expect(page.getByText(/No tienes sede asignada/)).toBeVisible();
  });

  test('admin edita sede PUT', async ({ page, mockApi }) => {
    mockApi.stub('PUT', '/headquarters/10', { json: { id: 10, name: 'Sede Centro Norte', address: 'Calle 1', description: 'Principal', createdAt: '', updatedAt: '', deletedAt: null, version: 1 } });
    await page.goto('/app/ops/sedes/10/editar');
    const form = new SedeFormPage(page);
    await expect(form.headingEditar()).toBeVisible();
    await form.name().fill('Sede Centro Norte');
    const put = mockApi.nextCall('PUT', '/headquarters/10');
    await form.save().click();
    expect((await put).json).toMatchObject({ name: 'Sede Centro Norte' });
  });

  test('detalle 404', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/headquarters/99', {
      status: 404,
      json: apiErrorBody('NOT_FOUND', 'No existe'),
    });
    await page.goto('/app/ops/sedes/99');
    await expect(page.getByText('No se pudo cargar la información')).toBeVisible();
    await expect(page.getByText('No existe')).toBeVisible();
  });
});
