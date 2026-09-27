import { Component, inject, OnInit, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
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
  imports: [PageHeaderComponent, DataStateComponent, ReactiveFormsModule, HeadquarterSelectComponent, RouterLink],
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

  readonly settingsForm = this.fb.nonNullable.group({
    currency: ['MXN', Validators.required],
    catalogStaleWarnHours: [24, [Validators.required, Validators.min(1)]],
    catalogStaleBlockHours: [72, [Validators.required, Validators.min(1)]],
    openAmountCategories: this.fb.control<string[]>([]),
    allowOpenProducts: [false],
    defaultNegativeStockLimit: [null as number | null],
    stockless: [false],
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
    this.settings.set(null);
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
            stockless: s.stockless,
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
              stockless: false,
            });
            return;
          }
          this.error.set(parsed);
        },
      });

    this.loadCategories(id);
  }

  saveSettings(): void {
    if (this.saving()) return;
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
        stockless: v.stockless,
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
            stockless: s.stockless,
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

  activeCategories(): PosSaleCategoryResponse[] {
    return this.saleCategories().filter((category) => category.active).sort((a, b) => a.displayOrder - b.displayOrder);
  }

  toggleCategory(name: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    const current = this.settingsForm.controls.openAmountCategories.value ?? [];
    const next = checked
      ? [...current.filter((category) => !this.sameCategory(category, name)), name]
      : current.filter((category) => !this.sameCategory(category, name));
    this.settingsForm.controls.openAmountCategories.setValue(next);
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
