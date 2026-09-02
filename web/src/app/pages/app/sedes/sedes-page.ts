import { Component, inject, OnInit, signal } from '@angular/core';
import { forkJoin, finalize } from 'rxjs';

import { HeadquarterService } from '../../../core/headquarters/headquarter.service';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import type { HeadQuarterResponse, HeadquarterStatisticsResponse } from '../../../core/model/headquarter/headquarter.dto';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../shared/ui/data-state/data-state';
import { SedeCardComponent } from './components/sede-card/sede-card';

/** Página de listado de sedes con estadísticas y grid de tarjetas. */
@Component({
  selector: 'app-sedes-page',
  
  imports: [PageHeaderComponent, DataStateComponent, SedeCardComponent],
  templateUrl: './sedes-page.html',
})
export class SedesPageComponent implements OnInit {
  private readonly service = inject(HeadquarterService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly sedes = signal<HeadQuarterResponse[]>([]);
  readonly stats = signal<HeadquarterStatisticsResponse | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.error.set(null);
    this.loading.set(true);
    forkJoin({
      page: this.service.list(),
      stats: this.service.statistics(),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ page, stats }) => {
          // La lista de sedes usa Spring Data Page (campo content)
          this.sedes.set(page.content);
          this.stats.set(stats);
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
