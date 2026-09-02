import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { CrmService } from '../../../../core/crm/crm.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { OpportunityResponse } from '../../../../core/model/crm/opportunity.dto';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';
import { OportunidadRowComponent } from './components/oportunidad-row/oportunidad-row';

/**
 * Página de listado de oportunidades CRM.
 * Muestra la lista paginada en formato tabla.
 */
@Component({
  selector: 'app-oportunidades-page',
  
  imports: [PageHeaderComponent, DataStateComponent, OportunidadRowComponent, RouterLink],
  templateUrl: './oportunidades-page.html',
})
export class OportunidadesPageComponent implements OnInit {
  private readonly service = inject(CrmService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly oportunidades = signal<OpportunityResponse[]>([]);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.error.set(null);
    this.loading.set(true);
    this.service
      .listOpportunities()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.oportunidades.set(page.items),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
