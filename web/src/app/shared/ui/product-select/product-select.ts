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

import type { ProductResponse, ProductStatus } from '../../../core/model/product/product.dto';
import { ProductService } from '../../../core/product/product.service';

@Component({
  selector: 'app-product-select',
  imports: [FormsModule],
  templateUrl: './product-select.html',
})
export class ProductSelectComponent implements OnInit {
  private readonly products = inject(ProductService);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly destroyRef = inject(DestroyRef);
  private readonly query$ = new Subject<string>();

  readonly label = input('Producto');
  readonly placeholder = input('Buscar por nombre, SKU o código');
  readonly allowNull = input(true);
  readonly disabled = input(false);
  readonly status = input<ProductStatus | undefined>('ACTIVE');
  readonly listLayout = input<'overlay' | 'inline'>('overlay');

  readonly value = input<number | null>(null);
  readonly valueChange = output<number | null>();
  readonly productChange = output<ProductResponse | null>();

  readonly query = signal('');
  readonly open = signal(false);
  readonly loading = signal(false);
  readonly options = signal<ProductResponse[]>([]);
  readonly selected = signal<ProductResponse | null>(null);

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
      this.products.get(id).subscribe({
        next: (product) => {
          this.selected.set(product);
          this.query.set(this.optionLabel(product));
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
          this.products
            .search({
              page: 0,
              size: 20,
              search: term.trim() || undefined,
              status: this.status(),
            })
            .pipe(
              catchError(() => of({ items: [] as ProductResponse[] })),
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
    if (event.key !== 'Enter') return;
    event.preventDefault();
    const needle = this.query().trim().toLowerCase();
    const exact = this.options().find(
      (product) =>
        product.sku.toLowerCase() === needle ||
        (product.barcode?.toLowerCase() ?? '') === needle,
    );
    if (exact) {
      this.select(exact);
      return;
    }
    const q = this.query().trim();
    if (!q) return;
    this.loading.set(true);
    this.products.lookup(q).subscribe({
      next: (product) => {
        this.loading.set(false);
        this.select(product);
      },
      error: () => this.loading.set(false),
    });
  }

  protected select(product: ProductResponse): void {
    this.selected.set(product);
    this.query.set(this.optionLabel(product));
    this.open.set(false);
    this.valueChange.emit(product.id);
    this.productChange.emit(product);
  }

  protected clear(): void {
    this.selected.set(null);
    this.query.set('');
    this.options.set([]);
    this.open.set(false);
    this.valueChange.emit(null);
    this.productChange.emit(null);
  }

  protected optionLabel(product: ProductResponse): string {
    return `${product.sku} · ${product.name}`;
  }

  private syncQueryToSelected(): void {
    const product = this.selected();
    this.query.set(product ? this.optionLabel(product) : '');
  }
}
