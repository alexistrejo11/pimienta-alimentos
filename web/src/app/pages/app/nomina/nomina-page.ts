import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { PayrollService } from '../../../core/payroll/payroll.service';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import type { PageMetadata } from '../../../core/model/common/pagination';
import type {
  PayrollDebtResponse,
  PayrollPaymentResponse,
  PayrollPeriodResponse,
  PayrollRecordResponse,
  PayrollSummaryResponse,
} from '../../../core/model/payroll/payroll.dto';
import type {
  PayrollAdjustmentType,
  PayrollFrequency,
  PayrollRecordStatus,
} from '../../../core/model/payroll/payroll.enums';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../shared/ui/data-state/data-state';

type NominaTab = 'registros' | 'periodos' | 'pagos' | 'adeudos';

@Component({
  selector: 'app-nomina-page',
  
  imports: [PageHeaderComponent, DataStateComponent, ReactiveFormsModule, FormsModule],
  templateUrl: './nomina-page.html',
})
export class NominaPageComponent implements OnInit {
  private readonly payroll = inject(PayrollService);
  private readonly fb = inject(FormBuilder);

  readonly activeTab = signal<NominaTab>('registros');

  /** Resumen agregado */
  readonly summaryLoading = signal(false);
  readonly summary = signal<PayrollSummaryResponse | null>(null);
  readonly summaryError = signal<ParsedApiError | null>(null);
  summaryFrom = '';
  summaryTo = '';

  /** Registros */
  readonly recordsLoading = signal(false);
  readonly recordsError = signal<ParsedApiError | null>(null);
  readonly records = signal<PayrollRecordResponse[]>([]);
  readonly recordsMeta = signal<PageMetadata | null>(null);
  readonly recordsPage = signal(0);
  filterEmployeeId = '';
  filterPeriodId = '';

  readonly recordSaving = signal(false);
  readonly recordFormError = signal<ParsedApiError | null>(null);
  readonly recordForm = this.fb.nonNullable.group({
    employeeId: [null as number | null, [Validators.required, Validators.min(1)]],
    periodId: [null as number | null],
    workedDaysStart: ['', Validators.required],
    workedDaysEnd: ['', Validators.required],
    grossAmount: [null as number | null, [Validators.required, Validators.min(0.01)]],
  });

  /** Ajuste sobre registro */
  readonly adjustingRecordId = signal<number | null>(null);
  readonly adjustmentSaving = signal(false);
  readonly adjustmentError = signal<ParsedApiError | null>(null);
  readonly adjustmentForm = this.fb.nonNullable.group({
    type: ['DISCOUNT' as PayrollAdjustmentType, Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    reason: ['', [Validators.required, Validators.minLength(2)]],
  });

  /** Períodos */
  readonly periodsLoading = signal(false);
  readonly periodsError = signal<ParsedApiError | null>(null);
  readonly periods = signal<PayrollPeriodResponse[]>([]);
  readonly periodSaving = signal(false);
  readonly periodFormError = signal<ParsedApiError | null>(null);
  readonly periodForm = this.fb.nonNullable.group({
    frequency: ['WEEKLY' as PayrollFrequency, Validators.required],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
  });

  /** Pagos */
  readonly paymentsLoading = signal(false);
  readonly paymentsError = signal<ParsedApiError | null>(null);
  readonly payments = signal<PayrollPaymentResponse[]>([]);
  readonly paymentsMeta = signal<PageMetadata | null>(null);
  readonly paymentsPage = signal(0);
  filterPaymentEmployeeId = '';

  /** Adeudos */
  readonly debtsLoading = signal(false);
  readonly debtsError = signal<ParsedApiError | null>(null);
  readonly debts = signal<PayrollDebtResponse[]>([]);
  readonly debtsMeta = signal<PageMetadata | null>(null);
  readonly debtsPage = signal(0);

  readonly importExporting = signal(false);
  readonly importMessage = signal<string | null>(null);
  readonly importError = signal<ParsedApiError | null>(null);

  readonly frequencyOptions: { value: PayrollFrequency; label: string }[] = [
    { value: 'WEEKLY', label: 'Semanal' },
    { value: 'BIWEEKLY', label: 'Quincenal' },
    { value: 'MONTHLY', label: 'Mensual' },
    { value: 'CUSTOM', label: 'Personalizado' },
  ];

  readonly tabButtonInactive =
    'rounded-lg px-4 py-2 text-sm font-semibold text-stone-600 transition hover:bg-stone-100 dark:text-stone-400 dark:hover:bg-stone-900';
  readonly tabButtonActive =
    'rounded-lg bg-[var(--color-primary)] px-4 py-2 text-sm font-bold text-[var(--color-on-primary)] shadow-sm';

  ngOnInit(): void {
    const now = new Date();
    const first = new Date(now.getFullYear(), now.getMonth(), 1);
    this.summaryFrom = first.toISOString().slice(0, 10);
    this.summaryTo = now.toISOString().slice(0, 10);
    this.loadSummary();
    this.loadRecords();
  }

  setTab(tab: NominaTab): void {
    this.activeTab.set(tab);
    if (tab === 'periodos' && this.periods().length === 0 && !this.periodsLoading()) {
      this.loadPeriods();
    }
    if (tab === 'pagos' && this.payments().length === 0 && !this.paymentsLoading()) {
      this.loadPayments();
    }
    if (tab === 'adeudos' && this.debts().length === 0 && !this.debtsLoading()) {
      this.loadDebts();
    }
  }

  loadSummary(): void {
    this.summaryError.set(null);
    this.summaryLoading.set(true);
    this.payroll
      .getSummary(this.summaryFrom || undefined, this.summaryTo || undefined)
      .pipe(finalize(() => this.summaryLoading.set(false)))
      .subscribe({
        next: (s) => this.summary.set(s),
        error: (err: unknown) => this.summaryError.set(parseApiError(err)),
      });
  }

  loadRecords(): void {
    this.recordsError.set(null);
    this.recordsLoading.set(true);
    const employeeId = this.filterEmployeeId.trim() ? Number(this.filterEmployeeId) : undefined;
    const periodId = this.filterPeriodId.trim() ? Number(this.filterPeriodId) : undefined;
    this.payroll
      .listRecords({
        page: this.recordsPage(),
        size: 20,
        employeeId: Number.isFinite(employeeId as number) ? employeeId : undefined,
        periodId: Number.isFinite(periodId as number) ? periodId : undefined,
      })
      .pipe(finalize(() => this.recordsLoading.set(false)))
      .subscribe({
        next: (page) => {
          this.records.set(page.items);
          this.recordsMeta.set(page.metadata);
        },
        error: (err: unknown) => this.recordsError.set(parseApiError(err)),
      });
  }

  applyRecordFilters(): void {
    this.recordsPage.set(0);
    this.loadRecords();
  }

  recordsPrev(): void {
    const m = this.recordsMeta();
    if (m && m.hasPrevious) {
      this.recordsPage.update((p) => p - 1);
      this.loadRecords();
    }
  }

  recordsNext(): void {
    const m = this.recordsMeta();
    if (m && m.hasNext) {
      this.recordsPage.update((p) => p + 1);
      this.loadRecords();
    }
  }

  submitRecord(): void {
    this.recordFormError.set(null);
    if (this.recordForm.invalid) {
      this.recordForm.markAllAsTouched();
      return;
    }
    const v = this.recordForm.getRawValue();
    this.recordSaving.set(true);
    this.payroll
      .registerRecord({
        employeeId: v.employeeId!,
        periodId: v.periodId ?? undefined,
        workedDaysStart: v.workedDaysStart,
        workedDaysEnd: v.workedDaysEnd,
        grossAmount: v.grossAmount!,
      })
      .pipe(finalize(() => this.recordSaving.set(false)))
      .subscribe({
        next: () => {
          this.recordForm.reset({
            employeeId: null,
            periodId: null,
            workedDaysStart: '',
            workedDaysEnd: '',
            grossAmount: null,
          });
          this.loadRecords();
          this.loadSummary();
        },
        error: (err: unknown) => this.recordFormError.set(parseApiError(err)),
      });
  }

  toggleAdjust(recordId: number): void {
    this.adjustmentError.set(null);
    this.adjustingRecordId.update((id) => (id === recordId ? null : recordId));
    this.adjustmentForm.reset({
      type: 'DISCOUNT',
      amount: null,
      reason: '',
    });
  }

  submitAdjustment(recordId: number): void {
    this.adjustmentError.set(null);
    if (this.adjustmentForm.invalid) {
      this.adjustmentForm.markAllAsTouched();
      return;
    }
    const v = this.adjustmentForm.getRawValue();
    this.adjustmentSaving.set(true);
    this.payroll
      .registerAdjustment(recordId, {
        type: v.type,
        amount: v.amount!,
        reason: v.reason.trim(),
      })
      .pipe(finalize(() => this.adjustmentSaving.set(false)))
      .subscribe({
        next: () => {
          this.adjustingRecordId.set(null);
          this.loadRecords();
          this.loadSummary();
        },
        error: (err: unknown) => this.adjustmentError.set(parseApiError(err)),
      });
  }

  loadPeriods(): void {
    this.periodsError.set(null);
    this.periodsLoading.set(true);
    this.payroll
      .listPeriods(0, 50)
      .pipe(finalize(() => this.periodsLoading.set(false)))
      .subscribe({
        next: (page) => this.periods.set(page.items),
        error: (err: unknown) => this.periodsError.set(parseApiError(err)),
      });
  }

  submitPeriod(): void {
    this.periodFormError.set(null);
    if (this.periodForm.invalid) {
      this.periodForm.markAllAsTouched();
      return;
    }
    const v = this.periodForm.getRawValue();
    this.periodSaving.set(true);
    this.payroll
      .registerPeriod({
        frequency: v.frequency,
        startDate: v.startDate,
        endDate: v.endDate,
      })
      .pipe(finalize(() => this.periodSaving.set(false)))
      .subscribe({
        next: () => {
          this.periodForm.reset({
            frequency: 'WEEKLY',
            startDate: '',
            endDate: '',
          });
          this.loadPeriods();
        },
        error: (err: unknown) => this.periodFormError.set(parseApiError(err)),
      });
  }

  loadPayments(): void {
    this.paymentsError.set(null);
    this.paymentsLoading.set(true);
    const eid = this.filterPaymentEmployeeId.trim() ? Number(this.filterPaymentEmployeeId) : undefined;
    this.payroll
      .listPayments(
        this.paymentsPage(),
        20,
        Number.isFinite(eid as number) ? eid : undefined,
      )
      .pipe(finalize(() => this.paymentsLoading.set(false)))
      .subscribe({
        next: (page) => {
          this.payments.set(page.items);
          this.paymentsMeta.set(page.metadata);
        },
        error: (err: unknown) => this.paymentsError.set(parseApiError(err)),
      });
  }

  applyPaymentFilters(): void {
    this.paymentsPage.set(0);
    this.loadPayments();
  }

  paymentsPrev(): void {
    const m = this.paymentsMeta();
    if (m?.hasPrevious) {
      this.paymentsPage.update((p) => p - 1);
      this.loadPayments();
    }
  }

  paymentsNext(): void {
    const m = this.paymentsMeta();
    if (m?.hasNext) {
      this.paymentsPage.update((p) => p + 1);
      this.loadPayments();
    }
  }

  loadDebts(): void {
    this.debtsError.set(null);
    this.debtsLoading.set(true);
    this.payroll
      .listDebts(this.debtsPage(), 20)
      .pipe(finalize(() => this.debtsLoading.set(false)))
      .subscribe({
        next: (page) => {
          this.debts.set(page.items);
          this.debtsMeta.set(page.metadata);
        },
        error: (err: unknown) => this.debtsError.set(parseApiError(err)),
      });
  }

  debtsPrev(): void {
    const m = this.debtsMeta();
    if (m?.hasPrevious) {
      this.debtsPage.update((p) => p - 1);
      this.loadDebts();
    }
  }

  debtsNext(): void {
    const m = this.debtsMeta();
    if (m?.hasNext) {
      this.debtsPage.update((p) => p + 1);
      this.loadDebts();
    }
  }

  exportExcel(): void {
    this.importError.set(null);
    this.importExporting.set(true);
    const employeeId = this.filterEmployeeId.trim() ? Number(this.filterEmployeeId) : undefined;
    const periodId = this.filterPeriodId.trim() ? Number(this.filterPeriodId) : undefined;
    this.payroll
      .exportRecords({
        page: 0,
        size: 1000,
        from: this.summaryFrom || undefined,
        to: this.summaryTo || undefined,
        employeeId: Number.isFinite(employeeId as number) ? employeeId : undefined,
        periodId: Number.isFinite(periodId as number) ? periodId : undefined,
      })
      .pipe(finalize(() => this.importExporting.set(false)))
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = 'payroll_reporte.xlsx';
          a.click();
          URL.revokeObjectURL(url);
        },
        error: (err: unknown) => this.importError.set(parseApiError(err)),
      });
  }

  onImportFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.importMessage.set(null);
    this.importError.set(null);
    this.importExporting.set(true);
    const employeeId = this.filterEmployeeId.trim() ? Number(this.filterEmployeeId) : undefined;
    const periodId = this.filterPeriodId.trim() ? Number(this.filterPeriodId) : undefined;
    this.payroll
      .importRecords(file, {
        from: this.summaryFrom || undefined,
        to: this.summaryTo || undefined,
        employeeId: Number.isFinite(employeeId as number) ? employeeId : undefined,
        periodId: Number.isFinite(periodId as number) ? periodId : undefined,
      })
      .pipe(finalize(() => {
        this.importExporting.set(false);
        input.value = '';
      }))
      .subscribe({
        next: (r) => {
          this.importMessage.set(
            `Importación: ${r.created} creados, ${r.updated} actualizados, ${r.skipped} omitidos. Errores: ${r.errors.length}.`,
          );
          this.loadRecords();
          this.loadSummary();
        },
        error: (err: unknown) => this.importError.set(parseApiError(err)),
      });
  }

  formatMXN(value: number): string {
    return new Intl.NumberFormat('es-MX', {
      style: 'currency',
      currency: 'MXN',
      maximumFractionDigits: 2,
    }).format(value);
  }

  formatDate(iso: string): string {
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }

  statusLabel(status: PayrollRecordStatus): string {
    const map: Record<PayrollRecordStatus, string> = {
      PENDING: 'Pendiente',
      PAID: 'Pagado',
      PARTIAL: 'Parcial',
      DEFERRED: 'Diferido',
    };
    return map[status] ?? status;
  }

  paymentStatusLabel(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'Pendiente',
      PAID: 'Pagado',
      PARTIAL: 'Parcial',
      DEFERRED: 'Diferido',
    };
    return map[status] ?? status;
  }

  frequencyLabel(f: PayrollFrequency): string {
    return this.frequencyOptions.find((o) => o.value === f)?.label ?? f;
  }
}
