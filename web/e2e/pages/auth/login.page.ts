import type { Page } from '@playwright/test';

export class LoginPage {
  constructor(private readonly page: Page) {}

  email() {
    return this.page.getByLabel('Correo');
  }

  password() {
    return this.page.getByLabel('Contraseña', { exact: true });
  }

  submit() {
    return this.page.getByRole('button', { name: 'Entrar' });
  }

  async fill(email: string, password: string): Promise<void> {
    await this.email().fill(email);
    await this.password().fill(password);
  }
}
