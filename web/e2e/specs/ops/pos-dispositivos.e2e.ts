import { posDevice } from '../../data/pos';
import { paged } from '../../mock-api/mock-api';
import { ConfirmDialog } from '../../pages/components/confirm-dialog.po';
import { expect, test } from '../../fixtures/workspace';

test.describe('Dispositivos POS', () => {
  test('lista, detalle Caja 1 y Revocar', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/pos/admin/devices', { json: paged([posDevice()]) });
    mockApi.stub('POST', '/pos/admin/devices/dev-1/revoke', {
      json: posDevice({ status: 'REVOKED' }),
    });

    await page.goto('/app/ops/pos/dispositivos');
    await expect(page.getByRole('heading', { name: 'Dispositivos POS' })).toBeVisible();
    await expect(page.getByRole('cell', { name: 'Caja 1' })).toBeVisible();

    await page.getByRole('link', { name: 'CAJA-1' }).click();
    await expect(page.getByRole('heading', { name: 'Caja 1' })).toBeVisible();

    await page.goto('/app/ops/pos/dispositivos');
    const confirm = ConfirmDialog.once(page, true);
    const revoke = mockApi.nextCall('POST', '/pos/admin/devices/dev-1/revoke');
    await page.getByRole('button', { name: 'Revocar' }).click();
    await confirm;
    await revoke;
  });
});
