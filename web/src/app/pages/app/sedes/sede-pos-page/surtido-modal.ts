import { Component, DestroyRef, HostListener, inject, input, OnInit, output, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { concatMap, finalize, of } from 'rxjs';

import { markFormPristine } from '../../../../core/forms/mark-form-pristine';
import { PosCatalogService } from '../../../../core/headquarters/pos-catalog.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import { stockPolicyLabel } from '../../../../core/i18n/enum-labels';
import type { ProductResponse } from '../../../../core/model/product/product.dto';
import type { HeadquarterPosCatalogItemResponse, PosSaleCategoryResponse } from '../../../../core/model/pos/pos.dto';
import type { StockPolicy } from '../../../../core/model/pos/pos.enums';
import { ProductService } from '../../../../core/product/product.service';
import { ProductSelectComponent } from '../../../../shared/ui/product-select/product-select';

type LookupState = 'idle' | 'checking' | 'in-catalog' | 'not-configured' | 'form';

@Component({
  selector: 'app-surtido-modal',
  imports: [ReactiveFormsModule, FormsModule, RouterLink, ProductSelectComponent],
  templateUrl: './surtido-modal.html',
})
export class SurtidoModalComponent implements OnInit {
  private readonly posCatalog = inject(PosCatalogService);
  private readonly products = inject(ProductService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly headquarterId = input.required<number>();
  readonly categories = input.required<PosSaleCategoryResponse[]>();
  readonly existing = input<HeadquarterPosCatalogItemResponse | null>(null);
  readonly intent = input<'create' | 'link'>('create');

  readonly cerrar = output<void>();
  readonly guardado = output<string>();

  readonly saving = signal(false);
  readonly checking = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly formMode = signal<'add' | 'edit' | 'link'>('add');
  readonly lookup = signal<LookupState>('idle');
  /** Link flow editing a row that is already in this sede. */
  readonly editingOffer = signal(false);
  readonly picked = signal<ProductResponse | null>(null);
  readonly catalogRow = signal<HeadquarterPosCatalogItemResponse | null>(null);

  readonly stockPolicies: StockPolicy[] = ['CONTROLLED', 'NOT_CONTROLLED'];
  readonly stockPolicyLabel = stockPolicyLabel;

  readonly productForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(300)]],
    posSaleCategoryId: [0, [Validators.required, Validators.min(1)]],
    barcode: [''],
    controlsStock: [false],
    salePrice: [0, [Validators.required, Validators.min(0.01)]],
  });

  readonly catalogForm = this.fb.nonNullable.group({
    saleCategory: ['', Validators.required],
    salePrice: [0, [Validators.required, Validators.min(0.01)]],
    available: [true],
    stockPolicy: ['NOT_CONTROLLED' as StockPolicy, Validators.required],
    negativeStockLimit: [null as number | null],
  });

  ngOnInit(): void {
    const row = this.existing();
    if (row) this.openEdit(row);
    else if (this.intent() === 'link') this.openLink();
    else this.openCreate();
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

  onProductPicked(product: ProductResponse | null): void {
    if (!product) return;
    this.picked.set(product);
    this.error.set(null);
    this.lookup.set('checking');
    this.checking.set(true);
    this.posCatalog
      .getCatalogItem(this.headquarterId(), product.id)
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

  startLinkForm(): void {
    const product = this.picked();
    if (!product) return;
    this.catalogForm.reset({
      saleCategory: this.categories()[0]?.name ?? '',
      salePrice: 0,
      available: true,
      stockPolicy: product.trackStock ? 'CONTROLLED' : 'NOT_CONTROLLED',
      negativeStockLimit: null,
    });
    markFormPristine(this.catalogForm);
    this.editingOffer.set(false);
    this.lookup.set('form');
  }

  startEditFromLookup(): void {
    const row = this.catalogRow();
    if (!row) return;
    this.catalogForm.reset({
      saleCategory: row.saleCategory,
      salePrice: row.salePrice,
      available: row.available,
      stockPolicy: row.stockPolicy,
      negativeStockLimit: row.negativeStockLimit,
    });
    markFormPristine(this.catalogForm);
    this.editingOffer.set(true);
    this.lookup.set('form');
  }

  guardar(): void {
    if (this.saving()) return;
    if (this.formMode() === 'link') {
      this.linkExisting();
      return;
    }
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      return;
    }
    if (this.formMode() === 'add') {
      this.createProduct();
      return;
    }
    this.updateProduct();
  }

  displayCategory(): string {
    return this.existing()?.saleCategory ?? this.catalogRow()?.saleCategory ?? '';
  }

  /** Active sede categories, plus the stored name when it is no longer in that list. */
  categoryChoices(): { id: number; name: string }[] {
    const rows = this.categories().map((category) => ({ id: category.id, name: category.name }));
    const current = this.displayCategory();
    if (current && !rows.some((row) => row.name === current)) {
      return [{ id: 0, name: current }, ...rows];
    }
    return rows;
  }

  productLabel(): string {
    const row = this.existing() ?? this.catalogRow();
    const product = this.picked();
    if (row?.productName) return `${row.productSku ? row.productSku + ' · ' : ''}${row.productName}`;
    if (product) return `${product.sku} · ${product.name}`;
    return 'Producto';
  }

  private openCreate(): void {
    this.formMode.set('add');
    this.lookup.set('form');
    this.productForm.reset({
      name: '',
      posSaleCategoryId: this.categories()[0]?.id ?? 0,
      barcode: '',
      controlsStock: false,
      salePrice: 0,
    });
    markFormPristine(this.productForm);
  }

  private openLink(): void {
    this.formMode.set('link');
    this.lookup.set('idle');
    this.picked.set(null);
    this.catalogRow.set(null);
    this.editingOffer.set(false);
  }

  private openEdit(row: HeadquarterPosCatalogItemResponse): void {
    this.formMode.set('edit');
    this.catalogRow.set(row);
    this.lookup.set('form');
    const matched = this.categories().find((category) => category.name === row.saleCategory);
    this.productForm.reset({
      name: row.productName,
      posSaleCategoryId: matched?.id ?? 0,
      barcode: row.productBarcode ?? '',
      controlsStock: row.stockPolicy === 'CONTROLLED',
      salePrice: row.salePrice,
    });
    markFormPristine(this.productForm);
  }

  private createProduct(): void {
    const v = this.productForm.getRawValue();
    const barcode = v.barcode.trim();
    this.saving.set(true);
    this.error.set(null);
    this.posCatalog
      .createPosProduct(this.headquarterId(), {
        name: v.name.trim(),
        barcode: barcode || undefined,
        salePrice: v.salePrice,
        posSaleCategoryId: v.posSaleCategoryId,
        stockPolicy: this.stockPolicy(v.controlsStock),
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => this.guardado.emit('Producto creado.'),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  private linkExisting(): void {
    if (this.catalogForm.invalid) {
      this.catalogForm.markAllAsTouched();
      return;
    }
    const productId = this.catalogRow()?.productId ?? this.picked()?.id;
    if (productId == null) return;
    const v = this.catalogForm.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.posCatalog
      .upsertCatalogItem(this.headquarterId(), productId, {
        saleCategory: v.saleCategory,
        salePrice: v.salePrice,
        available: v.available,
        stockPolicy: v.stockPolicy,
        negativeStockLimit: v.negativeStockLimit,
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () =>
          this.guardado.emit(this.editingOffer() ? 'Surtido actualizado.' : 'Producto agregado al surtido.'),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  private updateProduct(): void {
    const row = this.existing() ?? this.catalogRow();
    if (!row) return;
    const v = this.productForm.getRawValue();
    const nextName = v.name.trim();
    const nextBarcode = this.catalogBarcode(v.barcode, row.productSku);
    const nextPolicy = this.stockPolicy(v.controlsStock);
    const selected = this.categories().find((category) => category.id === v.posSaleCategoryId);
    const nextCategory = selected?.name ?? row.saleCategory;
    const previousBarcode = this.catalogBarcode(row.productBarcode ?? '', row.productSku);
    const rename = nextName !== row.productName.trim() || nextBarcode !== previousBarcode;
    const offer =
      v.salePrice !== row.salePrice || nextPolicy !== row.stockPolicy || nextCategory !== row.saleCategory;
    if (!rename && !offer) {
      this.cerrar.emit();
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.products
      .get(row.productId)
      .pipe(
        concatMap((product) => {
          const enableTrackStock = nextPolicy === 'CONTROLLED' && !product.trackStock;
          if (!rename && !enableTrackStock) {
            return of(product);
          }
          return this.products.update(row.productId, {
            name: nextName,
            description: product.description,
            unit: product.unit,
            barcode: nextBarcode ?? '',
            status: product.status,
            trackStock: enableTrackStock || product.trackStock,
          });
        }),
        concatMap(() => {
          if (!offer) return of(null);
          return this.posCatalog.upsertCatalogItem(this.headquarterId(), row.productId, {
            saleCategory: nextCategory,
            salePrice: v.salePrice,
            available: row.available,
            stockPolicy: nextPolicy,
            negativeStockLimit: row.negativeStockLimit,
          });
        }),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: () => this.guardado.emit('Producto actualizado.'),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  private stockPolicy(controlsStock: boolean): StockPolicy {
    return controlsStock ? 'CONTROLLED' : 'NOT_CONTROLLED';
  }

  /** Empty or SKU-equal barcode is stored as absent, same as the POS tablet. */
  private catalogBarcode(raw: string, sku: string): string | null {
    const trimmed = raw.trim();
    if (!trimmed) return null;
    if (trimmed.localeCompare(sku, undefined, { sensitivity: 'base' }) === 0) return null;
    return trimmed;
  }
}
