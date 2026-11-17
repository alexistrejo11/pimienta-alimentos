import { employeeDetail, employeeListItem, employeeStats } from '../../data/employees';
import { apiErrorBody, corsHeaders, paged } from '../../mock-api/mock-api';
import { ConfirmDialog } from '../../pages/components/confirm-dialog.po';
import { EmpleadoFormPage, EmpleadosPage } from '../../pages/erp/empleados.page';
import { expect, loginAs, test } from '../../fixtures/workspace';

test.describe('Empleados', () => {
  test('lista heading, stats y fila Luis Hernández', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/employees/statistics', { json: employeeStats() });
    mockApi.stub('GET', '/employees', { json: paged([employeeListItem()]) });

    await page.goto('/app/erp/empleados');
    const list = new EmpleadosPage(page);
    await expect(list.heading()).toBeVisible();
    await expect(page.getByText('Luis Hernández')).toBeVisible();
    await expect(page.getByText('4', { exact: true }).first()).toBeVisible();
    await expect(list.nuevo()).toBeVisible();
    await expect(list.exportExcel()).toBeVisible();
  });

  test('crear: vacío muestra Requerido y 400 fieldError', async ({ page, mockApi }) => {
    mockApi.stub('POST', '/employees', {
      status: 400,
      json: apiErrorBody('VALIDATION', 'Datos inválidos', [
        { field: 'firstName', message: 'Nombre demasiado corto' },
      ]),
    });

    await page.goto('/app/erp/empleados/nuevo');
    const form = new EmpleadoFormPage(page);
    await expect(form.headingNuevo()).toBeVisible();
    await form.submit().click();
    await expect(page.getByText('Requerido.').first()).toBeVisible();

    await form.firstName().fill('Nora');
    await form.lastName().fill('Nueva');
    const post400 = mockApi.nextCall('POST', '/employees');
    await form.submit().click();
    await post400;
    await expect(page.getByText('Nombre demasiado corto', { exact: true })).toBeVisible();
    await expect(page.getByText('Datos inválidos')).toBeVisible();
  });

  test('crear 201 multipart RegisterEmployeeRequest', async ({ page, mockApi }) => {
    mockApi.stub('GET', /^\/employees\/\d+$/, {
      json: employeeDetail({ id: 88, firstName: 'Nora', lastName: 'Nueva' }),
    });
    mockApi.stub('POST', '/employees', {
      status: 201,
      json: employeeDetail({ id: 88, firstName: 'Nora', lastName: 'Nueva' }),
    });

    await page.goto('/app/erp/empleados/nuevo');
    const form = new EmpleadoFormPage(page);
    await form.firstName().fill('Nora');
    await form.lastName().fill('Nueva');
    const post201 = mockApi.nextCall('POST', '/employees');
    await form.submit().click();
    const created = await post201;
    expect(created.multipartJson).toMatchObject({
      firstName: 'Nora',
      lastName: 'Nueva',
      onboardingPhase: 'DRAFT',
    });
    await expect(page).toHaveURL(/\/app\/erp\/empleados\/88/);
    await expect(page.getByRole('heading', { name: 'Nora Nueva', exact: true }).first()).toBeVisible();
  });

  test('Cancelar sucio pide confirmación', async ({ page, mockApi }) => {
    await page.goto('/app/erp/empleados/nuevo');
    const form = new EmpleadoFormPage(page);
    await form.firstName().fill('X');
    const dialog = ConfirmDialog.once(page, false);
    await form.cancel().click();
    expect(await dialog).toMatch(/cambios sin guardar/i);
    await expect(page).toHaveURL(/\/empleados\/nuevo/);
  });

  test('editar PUT multipart sin cambiar estatus', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/employees/7', { json: employeeDetail() });
    mockApi.stub('GET', /^\/employees\/\d+$/, { json: employeeDetail({ lastName: 'García' }) });
    mockApi.stub('PUT', '/employees/7', { json: employeeDetail({ lastName: 'García' }) });

    await page.goto('/app/erp/empleados/7/editar');
    const form = new EmpleadoFormPage(page);
    await expect(form.headingEditar()).toBeVisible();
    await form.lastName().fill('García');
    const put = mockApi.nextCall('PUT', '/employees/7');
    await form.submit().click();
    const call = await put;
    expect(call.multipartJson).toMatchObject({ firstName: 'Luis', lastName: 'García' });
    expect(mockApi.callsMatching('PATCH', /status/).length).toBe(0);
    await expect(page).toHaveURL(/\/app\/erp\/empleados\/7$/);
  });

  test('lista 403 FORBIDDEN', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/employees/statistics', { json: employeeStats() });
    mockApi.stub('GET', '/employees', {
      status: 403,
      json: apiErrorBody('FORBIDDEN', 'Sin permiso'),
    });

    await page.goto('/app/erp/empleados');
    const list = new EmpleadosPage(page);
    await expect(list.dataState.error()).toBeVisible();
    await expect(page.getByText('Sin permiso')).toBeVisible();
  });

  test('lista 500 y reintento', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/employees/statistics', { json: employeeStats() });
    let listCalls = 0;
    mockApi.stub('GET', '/employees', async (route, request) => {
      listCalls += 1;
      if (listCalls === 1) {
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          headers: corsHeaders(request),
          body: JSON.stringify(apiErrorBody('INTERNAL', 'Fallo interno')),
        });
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        headers: corsHeaders(request),
        body: JSON.stringify(paged([employeeListItem()])),
      });
    });

    await page.goto('/app/erp/empleados');
    const list = new EmpleadosPage(page);
    await expect(page.getByText('Fallo interno')).toBeVisible();
    await list.dataState.retry().click();
    await expect(page.getByText('Luis Hernández')).toBeVisible();
  });

  test('gerente en ERP empleados cae a operaciones', async ({ page, mockApi }) => {
    await loginAs(mockApi, 'manager');
    await page.goto('/app/erp/empleados');
    await expect(page).toHaveURL(/\/app\/ops\/dashboard/);
    await expect(page.getByRole('heading', { name: 'Operaciones' })).toBeVisible();
  });
});
