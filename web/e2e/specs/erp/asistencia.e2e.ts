import { paged } from '../../mock-api/mock-api';
import { expect, test } from '../../fixtures/workspace';

test.describe('Asistencia', () => {
  test('lista de hoy sin query de sede y columna id crudo', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/employees/attendances/for-today', {
      json: paged([
        {
          id: 1,
          employeeId: 7,
          employeeFullName: 'Luis Hernández',
          headquarterId: 10,
          workDate: '2026-11-17',
          checkInTime: '2026-11-17T08:00:00Z',
          checkOutTime: null,
          status: 'CHECKED_IN',
          checkInEvidencePhotoUrl: '',
          checkOutEvidencePhotoUrl: '',
        },
      ]),
    });
    await page.goto('/app/erp/asistencia');
    await expect(page.getByRole('heading', { name: 'Asistencia' })).toBeVisible();
    await expect(page.getByText('Luis Hernández')).toBeVisible();
    await expect(page.getByRole('cell', { name: '10', exact: true })).toBeVisible();
    const call = mockApi.lastCall('GET', '/employees/attendances/for-today');
    expect(call?.query.headquarterId).toBeUndefined();
  });
});
