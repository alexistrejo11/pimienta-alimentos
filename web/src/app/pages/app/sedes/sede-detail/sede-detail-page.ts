import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';

import { HeadquarterService } from '../../../../core/headquarters/headquarter.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { HeadQuarterResponse } from '../../../../core/model/headquarter/headquarter.dto';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';

/** Detalle de una sede. Bloque único de información, sin inner components (KISS). */
@Component({
  selector: 'app-sede-detail-page',
  
  imports: [PageHeaderComponent, DataStateComponent],
  templateUrl: './sede-detail-page.html',
})
export class SedeDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(HeadquarterService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly sede = signal<HeadQuarterResponse | null>(null);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.cargar(id);
  }

  cargar(id: number): void {
    this.error.set(null);
    this.loading.set(true);
    this.service
      .getById(id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (s) => this.sede.set(s),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  formatDateTime(iso: string | null): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('es-MX', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  get activa(): boolean {
    return this.sede()?.deletedAt === null;
  }
}
