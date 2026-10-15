import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../core/auth/session-context.service';
import { businessToday, formatIsoDate, formatMxn } from '../../../core/operating-expenses/business-date';
import { OperatingExpenseService } from '../../../core/operating-expenses/expense.service';
import { HeadquarterLookupService } from '../../../core/headquarters/headquarter-lookup.service';
import { operatingExpenseCategoryLabel } from '../../../core/i18n/enum-labels';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import {
  OPERATING_EXPENSE_CATEGORIES,
  type OperatingExpenseCategory,
  type OperatingExpenseResponse,
} from '../../../core/model/operating-expense/expense.dto';
import type { PageMetadata } from '../../../core/model/common/pagination';
import { DataStateComponent } from '../../../shared/ui/data-state/data-state';
import { HeadquarterSelectComponent } from '../../../shared/ui/headquarter-select/headquarter-select';
import { ListSearchFieldComponent } from '../../../shared/ui/list-search-field/list-search-field';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-operating-expenses-page',
  imports: [
    FormsModule,
    RouterLink,
    PageHeaderComponent,
    DataStateComponent,
    HeadquarterSelectComponent,
    ListSearchFieldComponent,
  ],
  templateUrl: './gastos-page.html',
})
export class OperatingExpensesPageComponent implements OnInit {
  private readonly expenses = inject(OperatingExpenseService);
  private readonly hqLookup = inject(HeadquarterLookupService);
  readonly session = inject(SessionContextService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly items = signal<OperatingExpenseResponse[]>([]);
  readonly metadata = signal<PageMetadata | null>(null);
  readonly deletingId = signal<number | null>(null);
  readonly staffUnassigned = signal(false);
  readonly previewUrl = signal<string | null>(null);
  readonly formatMxn = formatMxn;
  readonly formatIsoDate = formatIsoDate;
  readonly categories = OPERATING_EXPENSE_CATEGORIES;
  readonly categoryLabel = operatingExpenseCategoryLabel;

  selectedHeadquarterId: number | null = null;
  fromDate = '';
  toDate = '';
  category: OperatingExpenseCategory | '' = '';
  searchDraft = '';
  private searchApplied = '';
  private page = 0;

  readonly canManageOperatingExpenses = this.session.canManageOperatingExpenses;

  ngOnInit(): void {
    void this.hqLookup.ensureLoaded();
    this.session.ensureLoaded().subscribe({
      next: () => {
        if (!this.canManageOperatingExpenses()) {
          const assigned = this.session.assignedHeadquarterIds();
          if (assigned.length === 0) {
            this.staffUnassigned.set(true);
            this.loading.set(false);
            return;
          }
          if (assigned.length === 1) {
            this.selectedHeadquarterId = assigned[0];
          } else {
            const active = this.session.activeHeadquarterId();
            this.selectedHeadquarterId = active != null && assigned.includes(active) ? active : assigned[0];
          }
        }
        this.cargar();
      },
      error: () => this.loading.set(false),
    });
  }

  showHeadquarterFilter(): boolean {
    if (this.canManageOperatingExpenses()) return true;
    return this.session.assignedHeadquarterIds().length > 1;
  }

  pageTitle(): string {
    return this.canManageOperatingExpenses() ? 'Gastos Operativos' : 'Gastos operativos de hoy';
  }

  pageSubtitle(): string {
    if (this.canManageOperatingExpenses()) return 'Costos de operación por sede';
    return `Solo los gastos operativos de hoy (${formatIsoDate(businessToday())})`;
  }

  headquarterLabel(id: number): string {
    return this.hqLookup.name(id);
  }

  onHeadquarterChange(id: number | number[] | null): void {
    this.selectedHeadquarterId = typeof id === 'number' ? id : null;
    this.page = 0;
    this.cargar();
  }

  onDateChange(): void {
    this.page = 0;
    this.cargar();
  }

  onCategoryChange(): void {
    this.page = 0;
    this.cargar();
  }

  applySearch(term: string): void {
    this.searchApplied = term.trim();
    this.page = 0;
    this.cargar();
  }

  setPage(page: number): void {
    this.page = page;
    this.cargar();
  }

  openReceipt(url: string): void {
    this.previewUrl.set(url);
  }

  closeReceipt(): void {
    this.previewUrl.set(null);
  }

  cargar(): void {
    if (this.staffUnassigned()) return;
    this.error.set(null);
    this.loading.set(true);
    const managed = this.canManageOperatingExpenses();
    this.expenses
      .list({
        page: this.page,
        size: 20,
        headquarterId: this.selectedHeadquarterId ?? undefined,
        search: this.searchApplied,
        from: managed && this.fromDate ? this.fromDate : undefined,
        to: managed && this.toDate ? this.toDate : undefined,
        category: this.category || undefined,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => {
          this.items.set(page.items);
          this.metadata.set(page.metadata);
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  eliminar(item: OperatingExpenseResponse): void {
    if (!confirm(`¿Eliminar el gasto operativo "${item.concept}"?`)) return;
    this.deletingId.set(item.id);
    this.expenses.delete(item.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.cargar();
      },
      error: (err: unknown) => {
        this.deletingId.set(null);
        this.error.set(parseApiError(err));
      },
    });
  }
}
