import { posEnrollment } from '../../data/pos';
import { expect, test } from '../../fixtures/workspace';

test.describe('Enrolamiento POS', () => {
  test('POST enrollment-codes con headquarterId', async ({ page, mockApi }) => {
    mockApi.stub('POST', '/pos/admin/enrollment-codes', { json: posEnrollment() });
    await page.goto('/app/ops/pos/enrolamiento');
    await expect(page.getByRole('heading', { name: 'Enrolamiento' })).toBeVisible();
    await page.getByRole('combobox', { name: 'Sede' }).selectOption({ label: 'Sede Centro' });
    const post = mockApi.nextCall('POST', '/pos/admin/enrollment-codes');
    await page.getByRole('button', { name: 'Emitir código' }).click();
    expect((await post).json).toEqual({ headquarterId: 10 });
    await expect(page.getByText('ENROLL-99')).toBeVisible();
  });
});
