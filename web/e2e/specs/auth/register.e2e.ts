import { apiErrorBody } from '../../mock-api/mock-api';
import { expect, guestTest as test } from '../../fixtures/workspace';

test.describe('Registro', () => {
  test('validación cliente, 201 pending y 400 sin tokens', async ({ page, mockApi }) => {
    await page.goto('/auth/register');
    await expect(page.getByRole('heading', { name: 'Crear cuenta' })).toBeVisible();
    await page.getByRole('button', { name: 'Registrarse' }).click();
    await expect(page.getByText('Requerido.').first()).toBeVisible();

    await page.getByLabel('Nombre').fill('Nona');
    await page.getByLabel('Apellidos').fill('Nueva');
    await page.getByLabel('Género').selectOption('FEMALE');
    await page.getByLabel('Correo').fill('no-es-correo');
    await page.getByLabel('Teléfono').fill('5551234567');
    await page.getByLabel('Contraseña', { exact: true }).fill('secret123');
    await page.getByLabel('Confirmar contraseña').fill('otra');
    await page.getByRole('button', { name: 'Registrarse' }).click();
    await expect(page.getByText(/no coinciden|Correo no válido/i).first()).toBeVisible();

    mockApi.stub('POST', '/auth/register', {
      times: 1,
      status: 400,
      json: apiErrorBody('VALIDATION', 'Correo en uso', [{ field: 'email', message: 'Ya registrado' }]),
    });
    await page.getByLabel('Correo').fill('nona@pimienta.test');
    await page.getByLabel('Confirmar contraseña').fill('secret123');
    await page.getByRole('button', { name: 'Registrarse' }).click();
    await expect(page.getByText('Ya registrado', { exact: true })).toBeVisible();
    expect(await page.evaluate(() => localStorage.getItem('accessToken'))).toBeNull();

    mockApi.stub('POST', '/auth/register', {
      status: 201,
      json: { message: 'Cuenta creada, espera aprobación', status: 'PENDING_APPROVAL' },
    });
    await page.getByRole('button', { name: 'Registrarse' }).click();
    await expect(page.getByRole('heading', { name: 'Solicitud registrada' })).toBeVisible();
    expect(await page.evaluate(() => localStorage.getItem('accessToken'))).toBeNull();
    expect(mockApi.callsMatching('POST', '/auth/login').length).toBe(0);
  });
});
