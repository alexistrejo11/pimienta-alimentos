import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { InventoryService } from '../../../../core/inventory/inventory.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { InventoryCountSessionSummaryResponse } from '../../../../core/model/inventory/inventory.dto';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';

@Component({
  selector: 'app-count-session-list-page',
  imports: [RouterLink, PageHeaderComponent, DataStateComponent, HeadquarterSelectComponent],
  templateUrl: './count-session-list-page.html',
})
export class CountSessionListPageComponent implements OnInit {
  private readonly inventory = inject(InventoryService);
  private readonly session = inject(SessionContextService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly sessions = signal<InventoryCountSessionSummaryResponse[]>([]);

  selectedHeadquarterId: number | null = null;

  ngOnInit(): void {
    if (!this.session.isAdmin()) {
      this.selectedHeadquarterId = this.session.activeHeadquarterId();
    }
    this.cargar();
  }

  onHeadquarterChange(id: number | number[] | null): void {
    this.selectedHeadquarterId = Array.isArray(id) ? (id[0] ?? null) : id;
    this.session.selectHeadquarter(this.selectedHeadquarterId);
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(null);
    this.inventory
      .searchCounts({
        headquarterId: this.selectedHeadquarterId ?? undefined,
        page: 0,
        size: 50,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.sessions.set(page.items),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
