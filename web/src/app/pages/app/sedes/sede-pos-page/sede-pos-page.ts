import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EMPTY, expand, finalize, forkJoin, reduce } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { HeadquarterService } from '../../../../core/headquarters/headquarter.service';
import { PosCatalogService } from '../../../../core/headquarters/pos-catalog.service';
import { PosLabelPrintService } from '../../../../core/pos/pos-label-print.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import { stockPolicyLabel } from '../../../../core/i18n/enum-labels';
import type {
  HeadquarterPosCatalogItemResponse,
  PosSaleCategoryResponse,
} from '../../../../core/model/pos/pos.dto';
import type { StockPolicy } from '../../../../core/model/pos/pos.enums';
import type { PageMetadata } from '../../../../core/model/common/pagination';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';
import { ListSearchFieldComponent } from '../../../../shared/ui/list-search-field/list-search-field';
import { SurtidoModalComponent } from './surtido-modal';

@Component({
  selector: 'app-sede-pos-page',
  imports: [
    PageHeaderComponent,
    DataStateComponent,
    FormsModule,
    HeadquarterSelectComponent,
    ListSearchFieldComponent,
    SurtidoModalComponent,
    RouterLink,
  ],
  templateUrl: './sede-pos-page.html',
})
export class SedePosPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly session = inject(SessionContextService);
  private readonly hqService = inject(HeadquarterService);
  private readonly posCatalog = inject(PosCatalogService);
  private readonly labelPrint = inject(PosLabelPrintService);

  readonly headquarterId = signal(0);
  readonly globalMode = signal(false);
  readonly sedeName = signal('');
  readonly pageLoading = signal(true);
  readonly catalogLoading = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly catalog = signal<HeadquarterPosCatalogItemResponse[]>([]);
  readonly catalogMetadata = signal<PageMetadata | null>(null);
  readonly catalogPage = signal(0);
  readonly saleCategories = signal<PosSaleCategoryResponse[]>([]);
  readonly categoryNameDraft = signal('');
  readonly editingCategoryId = signal<number | null>(null);
  readonly editingCategoryName = signal('');
  readonly categoryError = signal<string | null>(null);
  readonly categorySaving = signal(false);
  readonly surtidoAbierto = signal(false);
  readonly surtidoEdit = signal<HeadquarterPosCatalogItemResponse | null>(null);
  readonly surtidoIntent = signal<'create' | 'link'>('create');
  readonly notice = signal('');
  readonly catalogSearchDraft = signal('');
  readonly catalogSearchApplied = signal('');
  readonly catalogCategory = signal('');
  readonly catalogAvailability = signal('');
  readonly catalogStockPolicy = signal('');
  readonly printingLabels = signal(false);
  readonly printNotice = signal<string | null>(null);

  readonly stockPolicies: StockPolicy[] = ['CONTROLLED', 'NOT_CONTROLLED'];
  readonly stockPolicyLabel = stockPolicyLabel;
  readonly canEditCatalog = computed(() => this.session.canOperateOps());
  readonly canAddProduct = computed(
    () => this.canEditCatalog() && this.activeCategories().length > 0,
  );
  readonly isAdmin = this.session.isAdmin;

  readonly activeCategories = computed(() =>
    this.saleCategories()
      .filter((category) => category.active)
      .sort((a, b) => a.name.localeCompare(b.name, 'es', { sensitivity: 'base' })),
  );

  ngOnInit(): void {
    const routeId = this.route.snapshot.paramMap.get('id');
    const id = routeId ? Number(routeId) : Number(this.session.activeHeadquarterId() ?? 0);
    this.globalMode.set(!routeId);
    this.headquarterId.set(id);
    if (id > 0) this.loadInitial(id);
    else this.pageLoading.set(false);
  }

  onHeadquarterChange(value: number | number[] | null): void {
    if (!this.globalMode() || typeof value !== 'number' || value === this.headquarterId()) return;
    this.catalogPage.set(0);
    this.catalogSearchDraft.set('');
    this.catalogSearchApplied.set('');
    this.cerrarSurtido();
    this.cancelCategoryEdit();
    this.headquarterId.set(value);
    this.loadInitial(value);
  }

  loadInitial(id: number): void {
    this.error.set(null);
    this.pageLoading.set(true);

    this.hqService.getById(id).subscribe({
      next: (hq) => this.sedeName.set(hq.name),
      error: () => {},
    });

    forkJoin({
      categories: this.posCatalog.listCategories(id),
      catalog: this.posCatalog.listCatalog(id, this.catalogPage(), 20, this.catalogListFilters()),
    })
      .pipe(finalize(() => this.pageLoading.set(false)))
      .subscribe({
        next: ({ categories, catalog }) => {
          this.saleCategories.set(categories);
          this.catalog.set(catalog.items);
          this.catalogMetadata.set(catalog.metadata);
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  reloadCatalog(): void {
    const id = this.headquarterId();
    if (id <= 0) return;

    this.error.set(null);
    this.catalogLoading.set(true);

    this.posCatalog
      .listCatalog(id, this.catalogPage(), 20, this.catalogListFilters())
      .pipe(finalize(() => this.catalogLoading.set(false)))
      .subscribe({
        next: (page) => {
          this.catalog.set(page.items);
          this.catalogMetadata.set(page.metadata);
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  private catalogListFilters(): {
    search?: string;
    saleCategory?: string;
    available?: boolean;
    stockPolicy?: string;
  } {
    const applied = this.catalogSearchApplied().trim();
    return {
      search: applied || undefined,
      saleCategory: this.catalogCategory() || undefined,
      available:
        this.catalogAvailability() === '' ? undefined : this.catalogAvailability() === 'true',
      stockPolicy: this.catalogStockPolicy() || undefined,
    };
  }

  applyCatalogSearch(term: string): void {
    const normalized = term.trim();
    if (normalized === this.catalogSearchApplied()) {
      return;
    }
    this.catalogSearchApplied.set(normalized);
    this.catalogSearchDraft.set(normalized);
    this.catalogPage.set(0);
    this.reloadCatalog();
  }

  previousCatalogPage(): void {
    if (this.catalogMetadata()?.hasPrevious) {
      this.catalogPage.update((page) => page - 1);
      this.reloadCatalog();
    }
  }

  nextCatalogPage(): void {
    if (this.catalogMetadata()?.hasNext) {
      this.catalogPage.update((page) => page + 1);
      this.reloadCatalog();
    }
  }

  onCatalogFilterChange(): void {
    this.catalogPage.set(0);
    this.reloadCatalog();
  }

  abrirAgregar(): void {
    this.notice.set('');
    this.surtidoEdit.set(null);
    this.surtidoIntent.set('create');
    this.surtidoAbierto.set(true);
  }

  abrirLinkear(): void {
    this.notice.set('');
    this.surtidoEdit.set(null);
    this.surtidoIntent.set('link');
    this.surtidoAbierto.set(true);
  }

  startEditCatalog(row: HeadquarterPosCatalogItemResponse): void {
    this.notice.set('');
    this.surtidoIntent.set('create');
    this.surtidoEdit.set(row);
    this.surtidoAbierto.set(true);
  }

  cerrarSurtido(): void {
    this.surtidoAbierto.set(false);
    this.surtidoEdit.set(null);
    this.surtidoIntent.set('create');
  }

  onSurtidoGuardado(message: string): void {
    this.cerrarSurtido();
    this.notice.set(message);
    this.reloadCatalog();
  }

  removeCatalogItem(row: HeadquarterPosCatalogItemResponse): void {
    const name = row.productName || this.productName(row.productId);
    if (
      !confirm(
        `¿Quitar «${name}» del catálogo POS de esta sede?\n\nDejará de aparecer en el POS tras el próximo sync. El producto no se borra.`,
      )
    ) {
      return;
    }
    this.posCatalog.deleteCatalogItem(this.headquarterId(), row.productId).subscribe({
      next: () => {
        this.printNotice.set(
          `«${name}» se quitó de la sede. El POS lo eliminará en el próximo sync.`,
        );
        this.reloadCatalog();
      },
      error: (err: unknown) => this.error.set(parseApiError(err)),
    });
  }

  createCategory(): void {
    const name = this.categoryNameDraft().trim();
    if (!name || name.length > 64) {
      this.categoryError.set('La categoría debe tener entre 1 y 64 caracteres.');
      return;
    }
    if (this.activeCategories().some((category) => this.sameCategory(category.name, name))) {
      this.categoryError.set('Ya existe una categoría con ese nombre.');
      return;
    }
    this.categoryError.set(null);
    this.categorySaving.set(true);
    this.posCatalog
      .createCategory(this.headquarterId(), name)
      .pipe(finalize(() => this.categorySaving.set(false)))
      .subscribe({
        next: () => {
          this.categoryNameDraft.set('');
          this.reloadCategories();
        },
        error: (err: unknown) => this.categoryError.set(parseApiError(err).message),
      });
  }

  beginCategoryEdit(category: PosSaleCategoryResponse): void {
    this.editingCategoryId.set(category.id);
    this.editingCategoryName.set(category.name);
    this.categoryError.set(null);
  }

  cancelCategoryEdit(): void {
    this.editingCategoryId.set(null);
    this.editingCategoryName.set('');
    this.categoryError.set(null);
  }

  saveCategoryEdit(category: PosSaleCategoryResponse): void {
    if (this.categorySaving()) return;
    const name = this.editingCategoryName().trim();
    if (!name || name.length > 64) {
      this.categoryError.set('La categoría debe tener entre 1 y 64 caracteres.');
      return;
    }
    if (this.activeCategories().some((item) => item.id !== category.id && this.sameCategory(item.name, name))) {
      this.categoryError.set('Ya existe una categoría con ese nombre.');
      return;
    }
    this.categorySaving.set(true);
    this.posCatalog
      .updateCategory(this.headquarterId(), category.id, name)
      .pipe(finalize(() => this.categorySaving.set(false)))
      .subscribe({
        next: () => {
          this.syncOpenAmountCategory(category.name, name);
          this.cancelCategoryEdit();
          this.reloadCategories();
        },
        error: (err: unknown) => this.categoryError.set(parseApiError(err).message),
      });
  }

  archiveCategory(category: PosSaleCategoryResponse): void {
    if (this.categorySaving()) return;
    if (typeof globalThis.confirm === 'function' && !globalThis.confirm(`¿Archivar la categoría ${category.name}?`)) return;
    this.categorySaving.set(true);
    this.posCatalog
      .archiveCategory(this.headquarterId(), category.id)
      .pipe(finalize(() => this.categorySaving.set(false)))
      .subscribe({
        next: () => {
          this.syncOpenAmountCategory(category.name, null);
          this.reloadCategories();
        },
        error: (err: unknown) => this.categoryError.set(parseApiError(err).message),
      });
  }

  setCategoryNameDraft(event: Event): void {
    this.categoryNameDraft.set((event.target as HTMLInputElement).value);
  }

  setEditingCategoryName(event: Event): void {
    this.editingCategoryName.set((event.target as HTMLInputElement).value);
  }

  productName(productId: number): string {
    return this.catalog().find((row) => row.productId === productId)?.productName || `Producto #${productId}`;
  }

  productSku(productId: number): string {
    return this.catalog().find((row) => row.productId === productId)?.productSku ?? '';
  }

  printCatalogLabels(): void {
    const id = this.headquarterId();
    if (id <= 0 || this.printingLabels()) return;

    this.printNotice.set(null);
    this.printingLabels.set(true);

    const filters = this.catalogListFilters();
    const pageSize = 100;

    this.posCatalog
      .listCatalog(id, 0, pageSize, filters)
      .pipe(
        expand((page) =>
          page.metadata.hasNext
            ? this.posCatalog.listCatalog(id, page.metadata.pageNumber + 1, pageSize, filters)
            : EMPTY,
        ),
        reduce(
          (acc, page) => acc.concat(page.items),
          [] as HeadquarterPosCatalogItemResponse[],
        ),
        finalize(() => this.printingLabels.set(false)),
      )
      .subscribe({
        next: (rows) => {
          const labels = rows
            .map((row) => ({
              sku: row.productSku || '',
              name: row.productName || this.productName(row.productId),
              price: row.salePrice,
            }))
            .filter((label) => label.sku);
          const skipped = rows.length - labels.length;
          if (!labels.length) {
            this.printNotice.set(
              skipped > 0
                ? 'Ningún producto del filtro tiene SKU para imprimir.'
                : 'No hay productos para imprimir con el filtro actual.',
            );
            return;
          }
          if (skipped > 0) {
            this.printNotice.set(
              `Se omitieron ${skipped} producto${skipped === 1 ? '' : 's'} sin SKU.`,
            );
          }
          void this.labelPrint.print(labels);
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  printItemLabel(row: HeadquarterPosCatalogItemResponse): void {
    const sku = row.productSku || this.productSku(row.productId);
    if (!sku) {
      this.printNotice.set('Este producto no tiene SKU; no se puede imprimir la etiqueta.');
      return;
    }
    this.printNotice.set(null);
    void this.labelPrint.print([
      { sku, name: row.productName || this.productName(row.productId), price: row.salePrice },
    ]);
  }

  private reloadCategories(): void {
    this.posCatalog.listCategories(this.headquarterId()).subscribe({
      next: (categories) => this.saleCategories.set(categories),
      error: (err: unknown) => this.categoryError.set(parseApiError(err).message),
    });
  }

  private syncOpenAmountCategory(previous: string, next: string | null): void {
    const id = this.headquarterId();
    this.posCatalog.getSettings(id).subscribe({
      next: (settings) => {
        const current = settings.openAmountCategories ?? [];
        const updated = current.flatMap((name) => {
          if (!this.sameCategory(name, previous)) return [name];
          return next ? [next] : [];
        });
        if (updated.length === current.length && updated.every((name, index) => name === current[index])) return;
        this.posCatalog
          .updateSettings(id, {
            currency: settings.currency,
            catalogStaleWarnHours: settings.catalogStaleWarnHours,
            catalogStaleBlockHours: settings.catalogStaleBlockHours,
            openAmountCategories: updated,
            allowOpenProducts: settings.allowOpenProducts,
            defaultNegativeStockLimit: settings.defaultNegativeStockLimit,
            stockless: settings.stockless,
          })
          .subscribe({ error: () => {} });
      },
      error: () => {},
    });
  }

  private sameCategory(left: string, right: string): boolean {
    return left.localeCompare(right, undefined, { sensitivity: 'base' }) === 0;
  }

  canAccess(): boolean {
    const id = this.headquarterId();
    if (this.session.isAdmin()) return true;
    return this.session.assignedHeadquarterIds().includes(id);
  }
}
