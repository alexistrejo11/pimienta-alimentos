import { userDashboard } from '../../data/users';
import { expect, test } from '../../fixtures/workspace';

test.describe('Dashboard oficina', () => {
  test('tiles GET /users/me/dashboard', async ({ page, mockApi }) => {
    mockApi.stub('GET', '/users/me/dashboard', { json: userDashboard({ totalActiveEmployees: 12 }) });
    await page.goto('/app/erp/dashboard');
    await expect(page.getByRole('heading', { name: 'Oficina' })).toBeVisible();
    await expect(page.getByLabel('Empleados activos')).toHaveText('12');
  });
});
