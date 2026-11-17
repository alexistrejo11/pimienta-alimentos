import { sedeCentro } from '../../data/headquarters';
import { managementUser, userResponse } from '../../data/users';
import { apiErrorBody, paged } from '../../mock-api/mock-api';
import { PromptDialog } from '../../pages/components/prompt-dialog.po';
import { AppRole } from '../../../src/app/core/model/account/enums';
import { expect, loginAs, test } from '../../fixtures/workspace';

const pending = managementUser();
const active = userResponse({
  id: 21,
  email: 'activo@pimienta.test',
  firstName: 'Ada',
  lastName: 'Activa',
  roles: [AppRole.MANAGER],
  accountStatus: 'ACTIVE',
  assignedHeadquarterIds: [10],
});

test.describe('Usuarios', () => {
  test('stats, tabla, aprobar, roles 400, sede, bloquear y no-op de roles vacíos', async ({
    page,
    mockApi,
  }) => {
    mockApi.stub('GET', '/users/management/statistics', {
      json: { totalUsers: 5, activeUsers: 3, bannedUsers: 1 },
    });
    mockApi.stub('GET', '/users/management', { json: paged([pending, active]) });
    mockApi.stub('POST', '/users/management/20/approve', { status: 204 });
    mockApi.stub('POST', '/users/management/21/roles', { json: active });
    mockApi.stub('POST', '/users/management/21/roles', {
      times: 1,
      status: 400,
      json: apiErrorBody('VALIDATION', 'Roles inválidos', [
        { field: 'roles', message: 'Rol no permitido' },
      ]),
    });
    mockApi.stub('POST', '/users/management/21/headquarters', { json: active });
    mockApi.stub('POST', '/users/management/21/ban', { status: 204 });

    await page.goto('/app/erp/usuarios');
    await expect(page.getByRole('heading', { name: 'Usuarios' })).toBeVisible();
    await expect(page.getByText('5', { exact: true }).first()).toBeVisible();
    await expect(page.getByText('Pedro Pendiente')).toBeVisible();
    await expect(page.getByText('Ada Activa')).toBeVisible();

    const approve = mockApi.nextCall('POST', '/users/management/20/approve');
    await page.getByRole('button', { name: 'Aprobar' }).click();
    await approve;

    const roles400 = mockApi.nextCall('POST', '/users/management/21/roles');
    await page.getByRole('button', { name: 'Guardar roles' }).click();
    await roles400;
    await expect(page.getByText('Roles inválidos')).toBeVisible();

    const hqPost = mockApi.nextCall('POST', '/users/management/21/headquarters');
    await page.getByRole('button', { name: 'Guardar sede' }).click();
    const hqCall = await hqPost;
    expect(hqCall.json).toEqual({ headquarterIds: [sedeCentro.id] });

    const prompt = PromptDialog.once(page, 'Conducta');
    const ban = mockApi.nextCall('POST', '/users/management/21/ban');
    await page.getByRole('button', { name: 'Bloquear' }).click();
    expect(await prompt).toMatch(/Motivo de bloqueo/);
    const banCall = await ban;
    expect(banCall.json).toEqual({ reason: 'Conducta' });

    const rolesBeforeEmpty = mockApi.callsMatching('POST', '/users/management/21/roles').length;
    const roleSelect = page.locator('select[multiple]');
    await roleSelect.focus();
    await page.keyboard.down('ControlOrMeta');
    await roleSelect.getByRole('option', { name: 'Gerente' }).click();
    await page.keyboard.up('ControlOrMeta');
    await page.getByRole('button', { name: 'Guardar roles' }).click();
    expect(mockApi.callsMatching('POST', '/users/management/21/roles').length).toBe(
      rolesBeforeEmpty,
    );
  });

  test('gerente no entra a usuarios ERP', async ({ page, mockApi }) => {
    await loginAs(mockApi, 'manager');
    await page.goto('/app/erp/usuarios');
    await expect(page).toHaveURL(/\/app\/ops\/dashboard/);
  });
});
