import type { Page } from '@playwright/test';

export class HeadquarterSelect {
  constructor(private readonly page: Page) {}

  select() {
    return this.page.getByRole('combobox', { name: 'Sede' });
  }

  async choose(name: string): Promise<void> {
    await this.select().selectOption({ label: name });
  }
}
