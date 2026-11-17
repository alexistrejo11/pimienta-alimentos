import { taskDetail, taskListItem } from '../../data/erp';
import { paged } from '../../mock-api/mock-api';
import { ConfirmDialog } from '../../pages/components/confirm-dialog.po';
import { expect, test } from '../../fixtures/workspace';

test.describe('Tareas', () => {
  test('lista, nueva sucia, POST y toggle checklist', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/tasks', { json: paged([taskListItem()]) });
    mockApi.stub('GET', '/tasks/3', { json: taskDetail() });
    mockApi.stub('GET', '/tasks/9', { json: taskDetail({ id: 9, title: 'Revisar' }) });
    mockApi.stub('POST', '/tasks', { status: 201, json: taskDetail({ id: 9, title: 'Revisar' }) });
    mockApi.stub('PATCH', '/tasks/3/checklist/1/toggle', {
      json: taskDetail({
        checklist: [{ description: 'Contar aceite', completed: true, completedAt: '2026-11-17T12:00:00Z', displayOrder: 1 }],
      }),
    });

    await page.goto('/app/erp/tareas');
    await expect(page.getByRole('heading', { name: 'Tareas' })).toBeVisible();
    await expect(page.getByText('Inventario semanal')).toBeVisible();

    await page.getByRole('link', { name: 'Nueva tarea' }).click();
    await expect(page.getByRole('heading', { name: 'Nueva tarea' })).toBeVisible();
    await page.getByLabel('Titulo').fill('Revisar');
    const dialog = ConfirmDialog.once(page, true);
    await page.getByRole('link', { name: 'Cancelar' }).click();
    await dialog;

    await page.goto('/app/erp/tareas/nueva');
    await page.getByLabel('Titulo').fill('Revisar');
    const post = mockApi.nextCall('POST', '/tasks');
    await page.getByRole('button', { name: 'Crear tarea' }).click();
    expect((await post).json).toMatchObject({ title: 'Revisar', priority: 'MEDIUM' });

    await page.goto('/app/erp/tareas/3');
    await expect(page.getByText('Contar aceite')).toBeVisible();
    const patch = mockApi.nextCall('PATCH', '/tasks/3/checklist/1/toggle');
    await page.getByRole('button', { name: /Contar aceite/ }).click();
    await patch;
  });
});
