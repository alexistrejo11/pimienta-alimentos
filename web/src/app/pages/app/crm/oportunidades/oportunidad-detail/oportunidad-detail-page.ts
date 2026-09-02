import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, finalize } from 'rxjs';

import { CrmService } from '../../../../../core/crm/crm.service';
import { parseApiError, type ParsedApiError } from '../../../../../core/http/parse-api-error';
import type { OpportunityResponse, OpportunitySummaryResponse } from '../../../../../core/model/crm/opportunity.dto';
import { PageHeaderComponent } from '../../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../../shared/ui/data-state/data-state';
import { StatusBadgeComponent } from '../../../../../shared/ui/status-badge/status-badge';
import { OportunidadStatsCardComponent } from '../components/oportunidad-stats-card/oportunidad-stats-card';

/**
 * Detalle completo de una oportunidad CRM.
 * Carga en paralelo los datos de la oportunidad y su resumen estadístico.
 */
@Component({
  selector: 'app-oportunidad-detail-page',
  
  imports: [
    PageHeaderComponent,
    DataStateComponent,
    StatusBadgeComponent,
    OportunidadStatsCardComponent,
    RouterLink,
  ],
  templateUrl: './oportunidad-detail-page.html',
})
export class OportunidadDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(CrmService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly oportunidad = signal<OpportunityResponse | null>(null);
  readonly summary = signal<OpportunitySummaryResponse | null>(null);
  readonly deleting = signal(false);
  readonly deleteError = signal<ParsedApiError | null>(null);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.cargar(id);
  }

  cargar(id: number): void {
    this.error.set(null);
    this.loading.set(true);
    // forkJoin espera que AMBAS llamadas terminen antes de emitir
    forkJoin({
      oportunidad: this.service.getOpportunity(id),
      summary: this.service.getOpportunitySummary(id),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ oportunidad, summary }) => {
          this.oportunidad.set(oportunidad);
          this.summary.set(summary);
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  formatDate(iso: string | null): string {
    if (!iso) return '—';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }

  formatMXN(value: number): string {
    return new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN', maximumFractionDigits: 0 }).format(value);
  }

  eliminar(): void {
    const op = this.oportunidad();
    if (!op) return;
    if (
      !confirm(
        `¿Eliminar la oportunidad «${op.title}»? El servidor puede aplicar borrado lógico (204 sin cuerpo).`,
      )
    ) {
      return;
    }
    this.deleteError.set(null);
    this.deleting.set(true);
    this.service
      .deleteOpportunity(op.id)
      .pipe(finalize(() => this.deleting.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl('/app/crm/oportunidades'),
        error: (err: unknown) => this.deleteError.set(parseApiError(err)),
      });
  }

  /** Traduce la fuente de la oportunidad al español. */
  fuente(source: string): string {
    const map: Record<string, string> = {
      INBOUND: 'Entrante',
      OUTBOUND: 'Saliente',
      REFERRAL: 'Referido',
      SOCIAL_MEDIA: 'Redes sociales',
      EVENT: 'Evento',
      COLD_CALL: 'Llamada en frío',
      OTHER: 'Otro',
    };
    return map[source] ?? source;
  }
}
