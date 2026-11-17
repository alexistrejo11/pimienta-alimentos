import type { Page } from '@playwright/test';

export class WorkspaceSidebar {
  constructor(private readonly page: Page) {}

  panel() {
    return this.page.getByRole('complementary', { name: 'Panel de navegación' }).or(
      this.page.locator('#workspace-navigation'),
    );
  }

  areaToggle() {
    return this.panel().getByRole('link', { name: 'Oficina', exact: true });
  }

  link(name: string) {
    return this.panel().getByRole('link', { name, exact: true });
  }

  sectionButton(name: string) {
    return this.panel().getByRole('button', { name, exact: true });
  }

  async expandSection(name: string): Promise<void> {
    const btn = this.sectionButton(name);
    if ((await btn.getAttribute('aria-expanded')) === 'false') {
      await btn.click();
    }
  }
}
