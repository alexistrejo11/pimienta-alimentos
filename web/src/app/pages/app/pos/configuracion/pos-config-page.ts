import { Component, inject, OnInit, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { PosCatalogService } from '../../../../core/headquarters/pos-catalog.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { PosSaleCategoryResponse, PosSettingsResponse } from '../../../../core/model/pos/pos.dto';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';

export function openAmountSettingsValidator(control: AbstractControl): ValidationErrors | null {
  const enabled = control.get('allowOpenProducts')?.value === true;
  const categories = (control.get('openAmountCategories')?.value as string[] | null) ?? [];
  const normalized = categories.map((category) => category.trim().toLocaleLowerCase()).filter(Boolean);
  const errors: ValidationErrors = {};
  if (enabled && normalized.length === 0) errors['openAmountCategoriesRequired'] = true;
  if (new Set(normalized).size !== normalized.length) errors['duplicateOpenAmountCategories'] = true;
  if (categories.some((category) => category.trim().length > 64)) errors['openAmountCategoryTooLong'] = true;
  return Object.keys(errors).length > 0 ? errors : null;
}

@Component({
  selector: 'app-pos-config-page',
  imports: [PageHeaderComponent, DataStateComponent, ReactiveFormsModule, HeadquarterSelectComponent],
  templateUrl: './pos-config-page.html',
})
export class PosConfigPageComponent implements OnInit {
  private readonly session = inject(SessionContextService);
  private readonly posCatalog = inject(PosCatalogService);
  private readonly fb = inject(FormBuilder);

  readonly headquarterId = signal(0);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly settings = signal<PosSettingsResponse | null>(null);
  readonly saleCategories = signal<PosSaleCategoryResponse[]>([]);
  readonly categoryNameDraft = signal('');
  readonly editingCategoryId = signal<number | null>(null);
  readonly editingCategoryName = signal('');
  readonly categoryError = signal<string | null>(null);
  readonly categorySaving = signal(false);

  readonly settingsForm = this.fb.nonNullable.group({
    currency: ['MXN', Validators.required],
    catalogStaleWarnHours: [24, [Validators.required, Validators.min(1)]],
    catalogStaleBlockHours: [72, [Validators.required, Validators.min(1)]],
    openAmountCategories: this.fb.control<string[]>([]),
    allowOpenProducts: [false],
    defaultNegativeStockLimit: [null as number | null],
  });

  constructor() {
    this.settingsForm.addValidators(openAmountSettingsValidator);
  }

  ngOnInit(): void {
    const id = Number(this.session.activeHeadquarterId() ?? 0);
    this.headquarterId.set(id);
    if (id > 0) this.cargar(id);
    else this.loading.set(false);
  }

  onHeadquarterChange(value: number | number[] | null): void {
    if (typeof value !== 'number' || value === this.headquarterId()) return;
    this.headquarterId.set(value);
    this.cargar(value);
  }

  cargar(id: number): void {
    this.error.set(null);
    this.loading.set(true);
    this.posCatalog
      .getSettings(id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (s) => {
          this.settings.set(s);
          this.settingsForm.patchValue({
            currency: s.currency,
            catalogStaleWarnHours: s.catalogStaleWarnHours,
            catalogStaleBlockHours: s.catalogStaleBlockHours,
            openAmountCategories: this.normalizeSelectedCategories(s.openAmountCategories ?? []),
            allowOpenProducts: s.allowOpenProducts,
            defaultNegativeStockLimit: s.defaultNegativeStockLimit,
          });
        },
        error: (err: unknown) => {
          const parsed = parseApiError(err);
          if (parsed.errorCode === 'HEADQUARTER_POS_SETTINGS_NOT_FOUND') {
            this.settings.set(null);
            this.settingsForm.reset({
              currency: 'MXN',
              catalogStaleWarnHours: 24,
              catalogStaleBlockHours: 72,
              openAmountCategories: [],
              allowOpenProducts: false,
              defaultNegativeStockLimit: null,
            });
            return;
          }
          this.error.set(parsed);
        },
      });

    this.loadCategories(id);
  }

  saveSettings(): void {
    if (this.settingsForm.invalid || this.headquarterId() <= 0) return;
    const v = this.settingsForm.getRawValue();
    this.saving.set(true);
    this.posCatalog
      .updateSettings(this.headquarterId(), {
        currency: v.currency,
        catalogStaleWarnHours: v.catalogStaleWarnHours,
        catalogStaleBlockHours: v.catalogStaleBlockHours,
        openAmountCategories: this.normalizeSelectedCategories(v.openAmountCategories ?? []),
        allowOpenProducts: v.allowOpenProducts,
        defaultNegativeStockLimit: v.defaultNegativeStockLimit,
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (s) => {
          this.settings.set(s);
          this.settingsForm.patchValue({
            currency: s.currency,
            catalogStaleWarnHours: s.catalogStaleWarnHours,
            catalogStaleBlockHours: s.catalogStaleBlockHours,
            openAmountCategories: this.normalizeSelectedCategories(s.openAmountCategories ?? []),
            allowOpenProducts: s.allowOpenProducts,
            defaultNegativeStockLimit: s.defaultNegativeStockLimit,
          });
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  private loadCategories(id: number): void {
    this.posCatalog.listCategories(id, true).subscribe({
      next: (categories) => this.saleCategories.set(categories),
      error: (err: unknown) => this.error.set(parseApiError(err)),
    });
  }

  createCategory(): void {
    const name = this.categoryNameDraft().trim();
    if (!name || name.length > 64) {
      this.categoryError.set('La categoría debe tener entre 1 y 64 caracteres.');
      return;
    }
    if (this.saleCategories().some((category) => category.active && this.sameCategory(category.name, name))) {
      this.categoryError.set('Ya existe una categoría con ese nombre.');
      return;
    }
    this.categoryError.set(null);
    this.categorySaving.set(true);
    const order = this.activeCategories().length;
    this.posCatalog
      .createCategory(this.headquarterId(), name, order)
      .pipe(finalize(() => this.categorySaving.set(false)))
      .subscribe({
        next: () => {
          this.categoryNameDraft.set('');
          this.loadCategories(this.headquarterId());
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
  }

  saveCategoryEdit(category: PosSaleCategoryResponse): void {
    const name = this.editingCategoryName().trim();
    if (!name || name.length > 64) {
      this.categoryError.set('La categoría debe tener entre 1 y 64 caracteres.');
      return;
    }
    if (this.saleCategories().some((item) => item.active && item.id !== category.id && this.sameCategory(item.name, name))) {
      this.categoryError.set('Ya existe una categoría con ese nombre.');
      return;
    }
    this.categorySaving.set(true);
    this.posCatalog
      .updateCategory(this.headquarterId(), category.id, name, category.displayOrder)
      .pipe(finalize(() => this.categorySaving.set(false)))
      .subscribe({
        next: () => {
          const selected = (this.settingsForm.controls.openAmountCategories.value ?? []).map((selectedName) =>
            this.sameCategory(selectedName, category.name) ? name : selectedName,
          );
          this.settingsForm.controls.openAmountCategories.setValue(selected);
          this.cancelCategoryEdit();
          this.loadCategories(this.headquarterId());
        },
        error: (err: unknown) => this.categoryError.set(parseApiError(err).message),
      });
  }

  archiveCategory(category: PosSaleCategoryResponse): void {
    if (typeof globalThis.confirm === 'function' && !globalThis.confirm(`¿Archivar la categoría ${category.name}?`)) return;
    this.categorySaving.set(true);
    this.posCatalog
      .archiveCategory(this.headquarterId(), category.id)
      .pipe(finalize(() => this.categorySaving.set(false)))
      .subscribe({
        next: () => {
          const selected = (this.settingsForm.controls.openAmountCategories.value ?? []).filter(
            (name) => !this.sameCategory(name, category.name),
          );
          this.settingsForm.controls.openAmountCategories.setValue(selected);
          this.loadCategories(this.headquarterId());
        },
        error: (err: unknown) => this.categoryError.set(parseApiError(err).message),
      });
  }

  moveCategory(category: PosSaleCategoryResponse, direction: -1 | 1): void {
    const active = this.activeCategories();
    const index = active.findIndex((item) => item.id === category.id);
    const target = active[index + direction];
    if (!target) return;
    this.categorySaving.set(true);
    this.posCatalog
      .updateCategory(this.headquarterId(), category.id, category.name, target.displayOrder)
      .pipe(finalize(() => this.categorySaving.set(false)))
      .subscribe({
        next: () => this.loadCategories(this.headquarterId()),
        error: (err: unknown) => this.categoryError.set(parseApiError(err).message),
      });
  }

  activeCategories(): PosSaleCategoryResponse[] {
    return this.saleCategories().filter((category) => category.active).sort((a, b) => a.displayOrder - b.displayOrder);
  }

  isFirstCategory(category: PosSaleCategoryResponse): boolean {
    return this.activeCategories().at(0)?.id === category.id;
  }

  isLastCategory(category: PosSaleCategoryResponse): boolean {
    const active = this.activeCategories();
    return active.at(-1)?.id === category.id;
  }

  toggleCategory(name: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    const current = this.settingsForm.controls.openAmountCategories.value ?? [];
    const next = checked
      ? [...current.filter((category) => !this.sameCategory(category, name)), name]
      : current.filter((category) => !this.sameCategory(category, name));
    this.settingsForm.controls.openAmountCategories.setValue(next);
  }

  setCategoryNameDraft(event: Event): void {
    this.categoryNameDraft.set((event.target as HTMLInputElement).value);
  }

  setEditingCategoryName(event: Event): void {
    this.editingCategoryName.set((event.target as HTMLInputElement).value);
  }

  isCategorySelected(name: string): boolean {
    return (this.settingsForm.controls.openAmountCategories.value ?? []).some((category) =>
      this.sameCategory(category, name),
    );
  }

  private normalizeSelectedCategories(categories: string[]): string[] {
    const activeNames = this.saleCategories().map((category) => category.name);
    const selected = categories
      .map((category) => category.trim())
      .filter(Boolean)
      .map((category) => activeNames.find((active) => this.sameCategory(active, category)) ?? category);
    return selected.filter(
      (category, index) =>
        selected.findIndex((value) => this.sameCategory(value, category)) === index,
    );
  }

  private sameCategory(left: string, right: string): boolean {
    return left.localeCompare(right, undefined, { sensitivity: 'base' }) === 0;
  }

  canAccess(): boolean {
    const id = this.headquarterId();
    if (id <= 0) return true;
    if (this.session.isAdmin()) return true;
    return this.session.assignedHeadquarterIds().includes(id);
  }
}
