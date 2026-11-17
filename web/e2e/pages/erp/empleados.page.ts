import type { Page } from '@playwright/test';

import { DataState } from '../components/data-state.po';

export class EmpleadosPage {
  readonly dataState: DataState;

  constructor(private readonly page: Page) {
    this.dataState = new DataState(page);
  }

  heading() {
    return this.page.getByRole('heading', { name: 'Empleados', exact: true });
  }

  nuevo() {
    return this.page.getByRole('link', { name: 'Nuevo empleado' });
  }

  exportExcel() {
    return this.page.getByRole('button', { name: /Exportar Excel/ });
  }

  row(name: string) {
    return this.page.getByRole('row', { name: new RegExp(name) });
  }
}

export class EmpleadoFormPage {
  constructor(private readonly page: Page) {}

  headingNuevo() {
    return this.page.getByRole('heading', { name: 'Nuevo empleado' });
  }

  headingEditar() {
    return this.page.getByRole('heading', { name: 'Editar empleado' });
  }

  firstName() {
    return this.page.getByLabel(/Nombre/);
  }

  lastName() {
    return this.page.getByLabel(/Apellido/);
  }

  submit() {
    return this.page.getByRole('button', { name: /Registrar empleado|Guardar cambios|Guardando/ });
  }

  cancel() {
    return this.page.getByRole('link', { name: 'Cancelar' });
  }
}
