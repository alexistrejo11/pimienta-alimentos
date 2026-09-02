import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { CrmService } from '../../../../core/crm/crm.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { ProjectResponse } from '../../../../core/model/crm/project.dto';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';
import { ProyectoRowComponent } from './components/proyecto-row/proyecto-row';

/**
 * Página de listado de proyectos CRM.
 */
@Component({
  selector: 'app-proyectos-page',
  
  imports: [PageHeaderComponent, DataStateComponent, ProyectoRowComponent, RouterLink],
  templateUrl: './proyectos-page.html',
})
export class ProyectosPageComponent implements OnInit {
  private readonly service = inject(CrmService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly proyectos = signal<ProjectResponse[]>([]);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.error.set(null);
    this.loading.set(true);
    this.service
      .listProjects()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.proyectos.set(page.items),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
