import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import type { OpportunityResponse } from '../../../../../../core/model/crm/opportunity.dto';
import { StatusBadgeComponent } from '../../../../../../shared/ui/status-badge/status-badge';

/** Fila de la tabla de oportunidades. */
@Component({
  selector: '[app-oportunidad-row]',
  
  imports: [RouterLink, StatusBadgeComponent],
  templateUrl: './oportunidad-row.html',
})
export class OportunidadRowComponent {
  readonly oportunidad = input.required<OpportunityResponse>();

  formatMXN(value: number): string {
    return new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN', maximumFractionDigits: 0 }).format(value);
  }

  formatDate(iso: string | null): string {
    if (!iso) return '—';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }
}
