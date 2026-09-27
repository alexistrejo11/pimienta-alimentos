import {
  Component,
  DestroyRef,
  ElementRef,
  HostListener,
  OnInit,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Subject, catchError, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';

import { InventoryService } from '../../../core/inventory/inventory.service';
import type { ItemResponse } from '../../../core/model/inventory/inventory.dto';
import type { CatalogRole, ItemStatus } from '../../../core/model/inventory/inventory.enums';

@Component({
  selector: 'app-item-select',
  imports: [FormsModule],
  templateUrl: './item-select.html',
})
export class ItemSelectComponent implements OnInit {
  private readonly inventory = inject(InventoryService);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly destroyRef = inject(DestroyRef);
  private readonly query$ = new Subject<string>();

  readonly label = input('Artículo');
  readonly placeholder = input('Buscar por SKU, nombre o código…');
  readonly allowNull = input(true);
  readonly disabled = input(false);
  readonly status = input<ItemStatus | undefined>('ACTIVE');
  readonly catalogRole = input<CatalogRole | undefined>(undefined);
  /** Inline list expands in document flow (better inside modals with overflow). */
  readonly listLayout = input<'overlay' | 'inline'>('overlay');

  readonly value = input<number | null>(null);
  readonly valueChange = output<number | null>();
  readonly itemChange = output<ItemResponse | null>();

  readonly query = signal('');
  readonly open = signal(false);
  readonly loading = signal(false);
  readonly options = signal<ItemResponse[]>([]);
  readonly selected = signal<ItemResponse | null>(null);

  constructor() {
    effect(() => {
      const id = this.value();
      const current = this.selected();
      if (id == null) {
        if (current != null) {
          this.selected.set(null);
          this.query.set('');
        }
        return;
      }
      if (current?.id === id) return;
      this.inventory.getItem(id).subscribe({
        next: (item) => {
          this.selected.set(item);
          this.query.set(this.optionLabel(item));
        },
        error: () => {},
      });
    });
  }

  ngOnInit(): void {
    this.query$
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        tap(() => this.loading.set(true)),
        switchMap((term) =>
          this.inventory
            .searchItems({
              page: 0,
              size: 20,
              search: term.trim() || undefined,
              status: this.status(),
              catalogRole: this.catalogRole(),
            })
            .pipe(
              catchError(() => of({ items: [] as ItemResponse[] })),
              tap(() => this.loading.set(false)),
            ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((page) => this.options.set(page.items));
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.open.set(false);
      this.syncQueryToSelected();
    }
  }

  protected onFocus(): void {
    if (this.disabled()) return;
    this.open.set(true);
    const selected = this.selected();
    // Si el input muestra la etiqueta completa, listar resultados generales (no buscar el label).
    if (selected && this.query() === this.optionLabel(selected)) {
      this.query$.next('');
    } else {
      this.query$.next(this.query());
    }
  }

  protected onQueryInput(value: string): void {
    this.query.set(value);
    this.open.set(true);
    this.query$.next(value);
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.open.set(false);
      this.syncQueryToSelected();
      return;
    }
    if (event.key === 'Enter') {
      event.preventDefault();
      const exact = this.options().find(
        (item) =>
          item.sku.toLowerCase() === this.query().trim().toLowerCase() ||
          (item.barcode?.toLowerCase() ?? '') === this.query().trim().toLowerCase(),
      );
      if (exact) {
        this.select(exact);
        return;
      }
      const q = this.query().trim();
      if (!q) return;
      this.loading.set(true);
      this.inventory.lookupItem(q).subscribe({
        next: (item) => {
          this.loading.set(false);
          this.select(item);
        },
        error: () => this.loading.set(false),
      });
    }
  }

  protected select(item: ItemResponse): void {
    this.selected.set(item);
    this.query.set(this.optionLabel(item));
    this.open.set(false);
    this.valueChange.emit(item.id);
    this.itemChange.emit(item);
  }

  protected clear(): void {
    this.selected.set(null);
    this.query.set('');
    this.options.set([]);
    this.open.set(false);
    this.valueChange.emit(null);
    this.itemChange.emit(null);
  }

  protected optionLabel(item: ItemResponse): string {
    return `${item.sku} · ${item.name}`;
  }

  private syncQueryToSelected(): void {
    const item = this.selected();
    this.query.set(item ? this.optionLabel(item) : '');
  }
}
