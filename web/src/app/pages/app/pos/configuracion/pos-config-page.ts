import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { PosCatalogService } from '../../../../core/headquarters/pos-catalog.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { PosSettingsResponse } from '../../../../core/model/pos/pos.dto';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';

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

  readonly settingsForm = this.fb.nonNullable.group({
    currency: ['MXN', Validators.required],
    catalogStaleWarnHours: [24, [Validators.required, Validators.min(1)]],
    catalogStaleBlockHours: [72, [Validators.required, Validators.min(1)]],
    openAmountCategories: [''],
    allowOpenProducts: [false],
    defaultNegativeStockLimit: [null as number | null],
  });

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
            openAmountCategories: (s.openAmountCategories ?? []).join(', '),
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
              openAmountCategories: '',
              allowOpenProducts: false,
              defaultNegativeStockLimit: null,
            });
            return;
          }
          this.error.set(parsed);
        },
      });
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
        openAmountCategories: v.openAmountCategories
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean),
        allowOpenProducts: v.allowOpenProducts,
        defaultNegativeStockLimit: v.defaultNegativeStockLimit,
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (s) => this.settings.set(s),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  canAccess(): boolean {
    const id = this.headquarterId();
    if (id <= 0) return true;
    if (this.session.isAdmin()) return true;
    return this.session.assignedHeadquarterIds().includes(id);
  }
}
