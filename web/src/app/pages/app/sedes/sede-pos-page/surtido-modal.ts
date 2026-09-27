import { Component, DestroyRef, HostListener, inject, input, OnInit, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { markFormPristine } from '../../../../core/forms/mark-form-pristine';
import { PosCatalogService } from '../../../../core/headquarters/pos-catalog.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import { stockPolicyLabel } from '../../../../core/i18n/enum-labels';
import type { ItemResponse } from '../../../../core/model/inventory/inventory.dto';
import type {
  HeadquarterPosCatalogItemResponse,
  PosSaleCategoryResponse,
} from '../../../../core/model/pos/pos.dto';
import type { StockPolicy } from '../../../../core/model/pos/pos.enums';
import { ItemSelectComponent } from '../../../../shared/ui/item-select/item-select';

type LookupState = 'idle' | 'checking' | 'in-catalog' | 'not-configured' | 'form';

@Component({
  selector: 'app-surtido-modal',
  imports: [ReactiveFormsModule, RouterLink, ItemSelectComponent],
  templateUrl: './surtido-modal.html',
})
export class SurtidoModalComponent implements OnInit {
  private readonly posCatalog = inject(PosCatalogService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly headquarterId = input.required<number>();
  readonly categories = input.required<PosSaleCategoryResponse[]>();
  readonly existing = input<HeadquarterPosCatalogItemResponse | null>(null);

  readonly cerrar = output<void>();
  readonly guardado = output<string>();

  readonly lookup = signal<LookupState>('idle');
  readonly checking = signal(false);
  readonly saving = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly picked = signal<ItemResponse | null>(null);
  readonly catalogRow = signal<HeadquarterPosCatalogItemResponse | null>(null);
  readonly formMode = signal<'add' | 'edit'>('add');

  readonly stockPolicies: StockPolicy[] = ['CONTROLLED', 'NOT_CONTROLLED'];
  readonly stockPolicyLabel = stockPolicyLabel;

  readonly catalogForm = this.fb.nonNullable.group({
    saleCategory: ['', Validators.required],
    salePrice: [0, [Validators.required, Validators.min(0)]],
    available: [true],
    stockPolicy: ['CONTROLLED' as StockPolicy, Validators.required],
    negativeStockLimit: [null as number | null],
  });

  ngOnInit(): void {
    const row = this.existing();
    if (row) this.openEdit(row);
    const prevBody = document.body.style.overflow;
    const prevHtml = document.documentElement.style.overflow;
    document.body.style.overflow = 'hidden';
    document.documentElement.style.overflow = 'hidden';
    this.destroyRef.onDestroy(() => {
      document.body.style.overflow = prevBody;
      document.documentElement.style.overflow = prevHtml;
    });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (!this.saving()) this.cerrar.emit();
  }

  onItemChange(id: number | null): void {
    if (id != null) return;
    this.picked.set(null);
    this.catalogRow.set(null);
    this.error.set(null);
    this.lookup.set('idle');
  }

  onItemPicked(item: ItemResponse | null): void {
    if (!item) return;
    this.picked.set(item);
    this.error.set(null);
    this.lookup.set('checking');
    this.checking.set(true);
    this.posCatalog
      .getCatalogItem(this.headquarterId(), item.id)
      .pipe(finalize(() => this.checking.set(false)))
      .subscribe({
        next: (row) => {
          this.catalogRow.set(row);
          this.lookup.set('in-catalog');
        },
        error: (err: unknown) => {
          const parsed = parseApiError(err);
          if (parsed.httpStatus === 404) {
            this.catalogRow.set(null);
            this.lookup.set('not-configured');
            return;
          }
          this.lookup.set('idle');
          this.error.set(parsed);
        },
      });
  }

  startAdd(): void {
    const item = this.picked();
    if (!item) return;
    this.formMode.set('add');
    this.catalogForm.reset({
      saleCategory: this.categories()[0]?.name ?? '',
      salePrice: 0,
      available: true,
      stockPolicy: 'CONTROLLED',
      negativeStockLimit: null,
    });
    markFormPristine(this.catalogForm);
    this.lookup.set('form');
  }

  startEditFromLookup(): void {
    const row = this.catalogRow();
    if (!row) return;
    this.openEdit(row);
  }

  guardar(): void {
    if (this.saving() || this.catalogForm.invalid) {
      this.catalogForm.markAllAsTouched();
      return;
    }
    const itemId = this.existing()?.itemId ?? this.catalogRow()?.itemId ?? this.picked()?.id;
    if (itemId == null) return;
    const v = this.catalogForm.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.posCatalog
      .upsertCatalogItem(this.headquarterId(), itemId, {
        saleCategory: v.saleCategory,
        salePrice: v.salePrice,
        available: v.available,
        stockPolicy: v.stockPolicy,
        negativeStockLimit: v.negativeStockLimit,
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () =>
          this.guardado.emit(
            this.formMode() === 'add' ? 'Producto agregado al surtido.' : 'Surtido actualizado.',
          ),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  productLabel(): string {
    const row = this.existing() ?? this.catalogRow();
    const item = this.picked();
    if (row?.itemName) return `${row.itemSku ? row.itemSku + ' · ' : ''}${row.itemName}`;
    if (item) return `${item.sku} · ${item.name}`;
    return 'Producto';
  }

  private openEdit(row: HeadquarterPosCatalogItemResponse): void {
    this.formMode.set('edit');
    this.catalogRow.set(row);
    this.catalogForm.reset({
      saleCategory: row.saleCategory,
      salePrice: row.salePrice,
      available: row.available,
      stockPolicy: row.stockPolicy,
      negativeStockLimit: row.negativeStockLimit,
    });
    markFormPristine(this.catalogForm);
    this.lookup.set('form');
  }
}
