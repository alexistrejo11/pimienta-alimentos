import { WorkspaceSidebar } from '../../pages/components/workspace-sidebar.po';
import { expect, loginAs, test } from '../../fixtures/workspace';

test.describe('Workspace móvil', () => {
  test('gerente abre el drawer, va a Sedes y cierra la navegación', async ({ page, mockApi }) => {
    await loginAs(mockApi, 'manager');
    await page.goto('/app/ops/dashboard');

    const openNav = page.getByRole('button', { name: 'Abrir navegación' });
    const overlay = page.locator('button.fixed[aria-label="Cerrar navegación"]');
    const nav = new WorkspaceSidebar(page);

    await openNav.click();
    await expect(openNav).toHaveAttribute('aria-expanded', 'true');
    await expect(overlay).toBeVisible();
    await expect(nav.panel()).toHaveAttribute('aria-label', 'Panel de navegación');

    await nav.link('Sedes').click();
    await expect(page).toHaveURL(/\/app\/ops\/sedes/);

    if ((await openNav.getAttribute('aria-expanded')) !== 'true') {
      await openNav.click();
    }
    await expect(overlay).toBeVisible();
    // Click the dimmed edge, not the overlay center (that sits under the 16rem drawer).
    await overlay.click({ position: { x: 390, y: 80 } });
    await expect(openNav).toHaveAttribute('aria-expanded', 'false');
    await expect(overlay).toHaveAttribute('aria-hidden', 'true');
  });
});
