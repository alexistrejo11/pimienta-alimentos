import type { Page } from '@playwright/test';

export class DataState {
  constructor(private readonly page: Page) {}

  loading() {
    return this.page.getByRole('status', { name: /cargando información/i });
  }

  error() {
    return this.page.getByRole('alert').filter({ hasText: 'No se pudo cargar la información' });
  }

  retry() {
    return this.page.getByRole('button', { name: 'Reintentar' });
  }
}
