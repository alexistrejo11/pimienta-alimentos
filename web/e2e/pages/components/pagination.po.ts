import type { Page } from '@playwright/test';

export class Pagination {
  constructor(private readonly page: Page) {}

  next() {
    return this.page.getByRole('button', { name: 'Siguiente' });
  }

  previous() {
    return this.page.getByRole('button', { name: 'Anterior' });
  }
}
