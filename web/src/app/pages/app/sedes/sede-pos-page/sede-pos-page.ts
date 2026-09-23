import { Component, HostListener, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
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
import { SurtidoModalComponent } from './surtido-modal';

@Component({
  selector: 'app-sede-pos-page',
  imports: [
    PageHeaderComponent,
    DataStateComponent,
    FormsModule,
    HeadquarterSelectComponent,
    SurtidoModalComponent,
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
  readonly loading = signal(true);
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
  readonly notice = signal('');
  readonly catalogSearch = signal('');
  readonly catalogCategory = signal('');
  readonly catalogAvailability = signal('');
  readonly catalogStockPolicy = signal('');
  readonly printingLabels = signal(false);
  readonly printNotice = signal<string | null>(null);

  readonly stockPolicies: StockPolicy[] = ['CONTROLLED', 'NOT_CONTROLLED'];
  readonly stockPolicyLabel = stockPolicyLabel;
  readonly canEditCatalog = computed(() => this.session.isAdmin() || this.session.isManager());
  private catalogSearchTimer: ReturnType<typeof setTimeout> | undefined;

  readonly activeCategories = computed(() =>
    this.saleCategories()
      .filter((category) => category.active)
      .sort((a, b) => a.displayOrder - b.displayOrder),
  );

  /** Matches Tailwind sm:grid-cols-2 xl:grid-cols-3 for category grid. */
  readonly categoryGridColumns = signal(1);

  ngOnInit(): void {
    this.syncCategoryGridColumns();
    const routeId = this.route.snapshot.paramMap.get('id');
    const id = routeId ? Number(routeId) : Number(this.session.activeHeadquarterId() ?? 0);
    this.globalMode.set(!routeId);
    this.headquarterId.set(id);
    if (id > 0) this.cargar(id);
    else this.loading.set(false);
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.syncCategoryGridColumns();
  }

  private syncCategoryGridColumns(): void {
    if (typeof window === 'undefined') return;
    const w = window.innerWidth;
    if (w >= 1280) this.categoryGridColumns.set(3);
    else if (w >= 640) this.categoryGridColumns.set(2);
    else this.categoryGridColumns.set(1);
  }

  private categoryIndex(category: PosSaleCategoryResponse): number {
    return this.activeCategories().findIndex((item) => item.id === category.id);
  }

  onHeadquarterChange(value: number | number[] | null): void {
    if (!this.globalMode() || typeof value !== 'number' || value === this.headquarterId()) return;
    this.catalogPage.set(0);
    this.cerrarSurtido();
    this.cancelCategoryEdit();
    this.headquarterId.set(value);
    this.cargar(value);
  }

  cargar(id: number): void {
    this.error.set(null);
    this.loading.set(true);

    this.hqService.getById(id).subscribe({
      next: (hq) => this.sedeName.set(hq.name),
      error: () => {},
    });

    this.posCatalog
      .listCatalog(id, this.catalogPage(), 20, {
        search: this.catalogSearch(),
        saleCategory: this.catalogCategory(),
        available: this.catalogAvailability() === '' ? undefined : this.catalogAvailability() === 'true',
        stockPolicy: this.catalogStockPolicy() || undefined,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => {
          this.catalog.set(page.items);
          this.catalogMetadata.set(page.metadata);
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });

    this.posCatalog.listCategories(id).subscribe({
      next: (categories) => this.saleCategories.set(categories),
      error: () => {},
    });
  }

  previousCatalogPage(): void {
    if (this.catalogMetadata()?.hasPrevious) {
      this.catalogPage.update((page) => page - 1);
      this.cargar(this.headquarterId());
    }
  }

  nextCatalogPage(): void {
    if (this.catalogMetadata()?.hasNext) {
      this.catalogPage.update((page) => page + 1);
      this.cargar(this.headquarterId());
    }
  }

  onCatalogFilterChange(): void {
    this.catalogPage.set(0);
    this.cargar(this.headquarterId());
  }

  onCatalogSearchChange(value: string): void {
    this.catalogSearch.set(value);
    if (this.catalogSearchTimer) clearTimeout(this.catalogSearchTimer);
    this.catalogSearchTimer = setTimeout(() => this.onCatalogFilterChange(), 300);
  }

  abrirAgregar(): void {
    this.notice.set('');
    this.surtidoEdit.set(null);
    this.surtidoAbierto.set(true);
  }

  startEditCatalog(row: HeadquarterPosCatalogItemResponse): void {
    this.notice.set('');
    this.surtidoEdit.set(row);
    this.surtidoAbierto.set(true);
  }

  cerrarSurtido(): void {
    this.surtidoAbierto.set(false);
    this.surtidoEdit.set(null);
  }

  onSurtidoGuardado(message: string): void {
    this.cerrarSurtido();
    this.notice.set(message);
    this.cargar(this.headquarterId());
  }

  removeCatalogItem(row: HeadquarterPosCatalogItemResponse): void {
    const name = row.itemName || this.itemName(row.itemId);
    if (
      !confirm(
        `¿Quitar «${name}» del catálogo POS de esta sede?\n\nDejará de aparecer en el POS tras el próximo sync. El artículo maestro del inventario no se borra.`,
      )
    ) {
      return;
    }
    this.posCatalog.deleteCatalogItem(this.headquarterId(), row.itemId).subscribe({
      next: () => {
        this.printNotice.set(
          `«${name}» se quitó de la sede. El POS lo eliminará en el próximo sync.`,
        );
        this.cargar(this.headquarterId());
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
      .createCategory(this.headquarterId(), name, this.activeCategories().length)
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
      .updateCategory(this.headquarterId(), category.id, name, category.displayOrder)
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

  /** Vertical move in the on-screen grid (not linear list index). */
  moveCategoryVertical(category: PosSaleCategoryResponse, direction: -1 | 1): void {
    this.swapCategoriesAt(category, this.categoryIndex(category) + direction * this.categoryGridColumns());
  }

  /** Horizontal move in the grid; changes order left-to-right on the same row. */
  moveCategoryHorizontal(category: PosSaleCategoryResponse, direction: -1 | 1): void {
    const index = this.categoryIndex(category);
    const cols = this.categoryGridColumns();
    const row = Math.floor(index / cols);
    const swapIndex = index + direction;
    if (Math.floor(swapIndex / cols) !== row) return;
    this.swapCategoriesAt(category, swapIndex);
  }

  private swapCategoriesAt(category: PosSaleCategoryResponse, swapIndex: number): void {
    if (this.categorySaving()) return;
    const active = [...this.activeCategories()];
    const index = active.findIndex((item) => item.id === category.id);
    if (index < 0 || swapIndex < 0 || swapIndex >= active.length) return;

    [active[index], active[swapIndex]] = [active[swapIndex], active[index]];

    const hq = this.headquarterId();
    this.categorySaving.set(true);
    forkJoin(
      active.map((cat, displayOrder) =>
        this.posCatalog.updateCategory(hq, cat.id, cat.name, displayOrder),
      ),
    )
      .pipe(finalize(() => this.categorySaving.set(false)))
      .subscribe({
        next: () => this.reloadCategories(),
        error: (err: unknown) => this.categoryError.set(parseApiError(err).message),
      });
  }

  canMoveCategoryUp(category: PosSaleCategoryResponse): boolean {
    return this.categoryIndex(category) >= this.categoryGridColumns();
  }

  canMoveCategoryDown(category: PosSaleCategoryResponse): boolean {
    const index = this.categoryIndex(category);
    return index + this.categoryGridColumns() < this.activeCategories().length;
  }

  canMoveCategoryLeft(category: PosSaleCategoryResponse): boolean {
    const index = this.categoryIndex(category);
    const cols = this.categoryGridColumns();
    return index % cols !== 0;
  }

  canMoveCategoryRight(category: PosSaleCategoryResponse): boolean {
    const index = this.categoryIndex(category);
    const cols = this.categoryGridColumns();
    const row = Math.floor(index / cols);
    const next = index + 1;
    return next < this.activeCategories().length && Math.floor(next / cols) === row;
  }

  setCategoryNameDraft(event: Event): void {
    this.categoryNameDraft.set((event.target as HTMLInputElement).value);
  }

  setEditingCategoryName(event: Event): void {
    this.editingCategoryName.set((event.target as HTMLInputElement).value);
  }

  itemName(itemId: number): string {
    return this.catalog().find((row) => row.itemId === itemId)?.itemName || `Ítem #${itemId}`;
  }

  itemSku(itemId: number): string {
    return this.catalog().find((row) => row.itemId === itemId)?.itemSku ?? '';
  }

  printCatalogLabels(): void {
    const id = this.headquarterId();
    if (id <= 0 || this.printingLabels()) return;

    this.printNotice.set(null);
    this.printingLabels.set(true);

    const filters = {
      search: this.catalogSearch(),
      saleCategory: this.catalogCategory(),
      available: this.catalogAvailability() === '' ? undefined : this.catalogAvailability() === 'true',
      stockPolicy: this.catalogStockPolicy() || undefined,
    };
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
              sku: row.itemSku || '',
              name: row.itemName || this.itemName(row.itemId),
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
    const sku = row.itemSku || this.itemSku(row.itemId);
    if (!sku) {
      this.printNotice.set('Este producto no tiene SKU; no se puede imprimir la etiqueta.');
      return;
    }
    this.printNotice.set(null);
    void this.labelPrint.print([
      { sku, name: row.itemName || this.itemName(row.itemId), price: row.salePrice },
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
