import { posOperator, posReconciliation, posShift } from '../../data/pos';
import { paged } from '../../mock-api/mock-api';
import { expect, loginAs, test } from '../../fixtures/workspace';

test.describe('Turnos POS', () => {
  test('admin ve historial y conciliación', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/pos/admin/operators', { json: paged([posOperator()], 0, 200, 1) });
    mockApi.stub('GET', '/pos/admin/shifts', { json: paged([posShift()]) });
    mockApi.stub('GET', '/pos/admin/shifts/shift-1/reconciliation', { json: posReconciliation() });

    await page.goto('/app/ops/pos/turnos');
    await expect(page.getByRole('heading', { name: 'Turnos POS' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Turnos activos' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Historial / Cortes Z' })).toBeVisible();
    await expect(page.getByText('Caja 1')).toBeVisible();

    await page.getByRole('button', { name: 'Historial / Cortes Z' }).click();
    mockApi.stub('GET', '/pos/admin/shifts', {
      json: paged([posShift({ status: 'CLOSED', closedAt: '2026-11-17T16:00:00Z' })]),
    });
    await page.getByRole('button', { name: 'Consultar' }).click();
    await page.getByText('Caja 1').click();
    const reco = mockApi.lastCall('GET', '/pos/admin/shifts/shift-1/reconciliation');
    expect(reco?.query.headquarterId).toBe('10');
    await expect(page.getByText('Detalle del turno')).toBeVisible();
  });

  test('gerente solo ve turnos activos', async ({ page, mockApi }) => {
    await loginAs(mockApi, 'manager');
    mockApi.stub('GET', '/pos/admin/operators', { json: paged([posOperator()], 0, 200, 1) });
    mockApi.stub('GET', '/pos/admin/shifts', { json: paged([posShift()]) });

    await page.goto('/app/ops/pos/turnos');
    await expect(page.getByRole('heading', { name: 'Turnos POS' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Turnos activos' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Historial / Cortes Z' })).toHaveCount(0);
  });

  test('cortes redirige al historial', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/pos/admin/operators', { json: paged([posOperator()], 0, 200, 1) });
    mockApi.stub('GET', '/pos/admin/shifts', { json: paged([]) });
    await page.goto('/app/ops/pos/cortes');
    await expect(page).toHaveURL(/\/app\/ops\/pos\/turnos\?tab=history/);
    await expect(page.getByRole('heading', { name: 'Turnos POS' })).toBeVisible();
  });
});
