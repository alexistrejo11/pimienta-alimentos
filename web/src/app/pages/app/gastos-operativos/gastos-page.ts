import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';

import { SessionContextService } from '../../../core/auth/session-context.service';
import {
  businessMonthToDate,
  businessToday,
  businessWeekToDate,
  businessYesterday,
  formatCompactDate,
  formatIsoDate,
  formatMxn,
} from '../../../core/operating-expenses/business-date';
import { OperatingExpenseService } from '../../../core/operating-expenses/expense.service';
import { dailyExpenseBars, type DailyExpenseBar } from '../../../core/operating-expenses/expense-summary-bars';
import { HeadquarterLookupService } from '../../../core/headquarters/headquarter-lookup.service';
import { operatingExpenseCategoryLabel } from '../../../core/i18n/enum-labels';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import {
  OPERATING_EXPENSE_CATEGORIES,
  type OperatingExpenseCategory,
  type OperatingExpenseResponse,
  type OperatingExpenseSummaryResponse,
} from '../../../core/model/operating-expense/expense.dto';
import type { PageMetadata } from '../../../core/model/common/pagination';
import { DataStateComponent } from '../../../shared/ui/data-state/data-state';
import { HeadquarterSelectComponent } from '../../../shared/ui/headquarter-select/headquarter-select';
import { ListSearchFieldComponent } from '../../../shared/ui/list-search-field/list-search-field';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header';

type ExpenseDatePreset = 'today' | 'yesterday' | 'week' | 'month';

interface ReceiptPreview {
  urls: readonly string[];
  index: number;
}

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
  styles: ['.expense-table { min-width: 0; }'],
})
export class OperatingExpensesPageComponent implements OnInit {
  private readonly expenses = inject(OperatingExpenseService);
  private readonly hqLookup = inject(HeadquarterLookupService);
  readonly session = inject(SessionContextService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly items = signal<OperatingExpenseResponse[]>([]);
  readonly metadata = signal<PageMetadata | null>(null);
  readonly summary = signal<OperatingExpenseSummaryResponse | null>(null);
  readonly deletingId = signal<number | null>(null);
  readonly staffUnassigned = signal(false);
  readonly preview = signal<ReceiptPreview | null>(null);
  readonly formatMxn = formatMxn;
  readonly formatCompactDate = formatCompactDate;
  readonly categories = OPERATING_EXPENSE_CATEGORIES;
  readonly categoryLabel = operatingExpenseCategoryLabel;
  readonly datePresets: { id: ExpenseDatePreset; label: string }[] = [
    { id: 'today', label: 'Hoy' },
    { id: 'yesterday', label: 'Ayer' },
    { id: 'week', label: 'Esta semana' },
    { id: 'month', label: 'Este mes' },
  ];

  selectedHeadquarterId: number | null = null;
  fromDate = '';
  toDate = '';
  activePreset: ExpenseDatePreset | null = null;
  category: OperatingExpenseCategory | '' = '';
  searchDraft = '';
  private searchApplied = '';
  private page = 0;
  private loadSeq = 0;

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
        } else {
          const active = this.session.activeHeadquarterId();
          if (active != null) this.selectedHeadquarterId = active;
          const today = businessToday();
          this.fromDate = today;
          this.toDate = today;
          this.activePreset = 'today';
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

  showSedeColumn(): boolean {
    return this.selectedHeadquarterId == null;
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

  dailyBars(): DailyExpenseBar[] {
    if (!this.canManageOperatingExpenses()) return [];
    return dailyExpenseBars(this.fromDate, this.toDate, this.summary()?.byDay ?? [], formatMxn);
  }

  conceptTitle(row: OperatingExpenseResponse): string {
    return row.notes ? `${row.concept} — ${row.notes}` : row.concept;
  }

  onHeadquarterChange(id: number | number[] | null): void {
    this.selectedHeadquarterId = typeof id === 'number' ? id : null;
    this.page = 0;
    this.cargar();
  }

  onDateChange(): void {
    this.activePreset = this.presetMatching(this.fromDate, this.toDate);
    this.page = 0;
    this.cargar();
  }

  applyPreset(preset: ExpenseDatePreset): void {
    const today = businessToday();
    if (preset === 'today') {
      this.fromDate = today;
      this.toDate = today;
    } else if (preset === 'yesterday') {
      const day = businessYesterday();
      this.fromDate = day;
      this.toDate = day;
    } else if (preset === 'week') {
      const range = businessWeekToDate();
      this.fromDate = range.from;
      this.toDate = range.to;
    } else {
      const range = businessMonthToDate();
      this.fromDate = range.from;
      this.toDate = range.to;
    }
    this.activePreset = preset;
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

  openReceipts(row: OperatingExpenseResponse): void {
    const urls = row.attachments.map((file) => file.url);
    if (urls.length === 0) return;
    this.preview.set({ urls, index: 0 });
  }

  closeReceipt(): void {
    this.preview.set(null);
  }

  stepPreview(delta: number): void {
    const current = this.preview();
    if (!current || current.urls.length < 2) return;
    const count = current.urls.length;
    const index = (current.index + delta + count) % count;
    this.preview.set({ urls: current.urls, index });
  }

  cargar(): void {
    if (this.staffUnassigned()) return;
    const seq = ++this.loadSeq;
    this.error.set(null);
    this.loading.set(true);
    const managed = this.canManageOperatingExpenses();
    const filters = {
      headquarterId: this.selectedHeadquarterId ?? undefined,
      search: this.searchApplied,
      from: managed && this.fromDate ? this.fromDate : undefined,
      to: managed && this.toDate ? this.toDate : undefined,
      category: this.category || undefined,
    };
    forkJoin({
      page: this.expenses.list({ page: this.page, size: 20, ...filters }),
      summary: this.expenses.summary(filters),
    })
      .pipe(
        finalize(() => {
          if (seq === this.loadSeq) this.loading.set(false);
        }),
      )
      .subscribe({
        next: ({ page, summary }) => {
          if (seq !== this.loadSeq) return;
          this.items.set(page.items);
          this.metadata.set(page.metadata);
          this.summary.set(summary);
        },
        error: (err: unknown) => {
          if (seq !== this.loadSeq) return;
          this.items.set([]);
          this.metadata.set(null);
          this.summary.set(null);
          this.error.set(parseApiError(err));
        },
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

  private presetMatching(from: string, to: string): ExpenseDatePreset | null {
    if (!from || !to) return null;
    const today = businessToday();
    if (from === today && to === today) return 'today';
    const yesterday = businessYesterday();
    if (from === yesterday && to === yesterday) return 'yesterday';
    const week = businessWeekToDate();
    if (from === week.from && to === week.to) return 'week';
    const month = businessMonthToDate();
    if (from === month.from && to === month.to) return 'month';
    return null;
  }
}
