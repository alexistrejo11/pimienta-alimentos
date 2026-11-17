import type { Page } from '@playwright/test';

export class SedeFormPage {
  constructor(private readonly page: Page) {}

  headingEditar() {
    return this.page.getByRole('heading', { name: 'Editar sede' });
  }

  name() {
    return this.page.getByRole('textbox').first();
  }

  save() {
    return this.page.getByRole('button', { name: 'Guardar cambios' });
  }
}
