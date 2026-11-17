import type { Locator, Page } from '@playwright/test';

export class ListSearchField {
  constructor(
    private readonly page: Page,
    private readonly root?: Locator,
  ) {}

  private scope() {
    return this.root ?? this.page.locator('body');
  }

  input() {
    return this.scope().getByRole('searchbox');
  }

  searchButton() {
    return this.scope().getByRole('button', { name: 'Buscar' });
  }

  async search(term: string): Promise<void> {
    await this.input().fill(term);
    await this.searchButton().click();
  }
}
