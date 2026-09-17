import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, catchError, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';

import { InventoryService } from '../../../../core/inventory/inventory.service';
import { SessionContextService } from '../../../../core/auth/session-context.service';
import type { ItemResponse, StorageLocationResponse } from '../../../../core/model/inventory/inventory.dto';
import type { InventoryCountType } from '../../../../core/model/inventory/inventory.enums';

@Component({
  selector: 'app-count-session-create-page',
  imports: [FormsModule],
  templateUrl: './count-session-create-page.html',
})
export class CountSessionCreatePageComponent implements OnInit {
  private readonly inventory = inject(InventoryService);
  private readonly session = inject(SessionContextService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly itemSearch$ = new Subject<string>();

  readonly locations = signal<StorageLocationResponse[]>([]);
  readonly items = signal<ItemResponse[]>([]);
  readonly busy = signal(false);
  readonly loadingItems = signal(false);
  readonly error = signal('');
  readonly itemQuery = signal('');

  locationId: number | null = null;
  type: InventoryCountType = 'FULL';
  selected = new Set<number>();

  ngOnInit(): void {
    const hqId = this.session.isAdmin() ? undefined : (this.session.activeHeadquarterId() ?? undefined);
    this.inventory.searchLocations({ size: 100, headquarterId: hqId }).subscribe({
      next: (p) => this.locations.set(p.items),
      error: () => this.error.set('No se pudieron cargar las ubicaciones.'),
    });

    this.itemSearch$
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        tap(() => this.loadingItems.set(true)),
        switchMap((term) =>
          this.inventory
            .searchItems({ page: 0, size: 50, search: term.trim() || undefined, status: 'ACTIVE' })
            .pipe(
              catchError(() => of({ items: [] as ItemResponse[] })),
              tap(() => this.loadingItems.set(false)),
            ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((page) => this.items.set(page.items));

    this.itemSearch$.next('');
  }

  onItemQueryChange(value: string): void {
    this.itemQuery.set(value);
    this.itemSearch$.next(value);
  }

  toggle(id: number): void {
    if (this.selected.has(id)) this.selected.delete(id);
    else this.selected.add(id);
  }

  open(): void {
    if (this.locationId == null || (this.type === 'PARTIAL' && this.selected.size === 0)) return;
    this.busy.set(true);
    this.error.set('');
    this.inventory
      .openCount({
        locationId: this.locationId,
        type: this.type,
        itemIds: this.type === 'PARTIAL' ? [...this.selected] : undefined,
      })
      .subscribe({
        next: (s) => void this.router.navigate(['/app/inventario/conteos', s.id]),
        error: () => {
          this.error.set('No se pudo abrir el conteo.');
          this.busy.set(false);
        },
      });
  }
}
