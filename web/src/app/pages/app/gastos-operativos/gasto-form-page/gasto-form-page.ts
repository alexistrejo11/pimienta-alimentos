import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { clientZone } from '../../../../core/i18n/client-zone';
import { businessToday } from '../../../../core/operating-expenses/business-date';
import { OperatingExpenseService } from '../../../../core/operating-expenses/expense.service';
import { compressReceiptImage } from '../../../../core/operating-expenses/receipt-image';
import { markFormPristine } from '../../../../core/forms/mark-form-pristine';
import { HeadquarterLookupService } from '../../../../core/headquarters/headquarter-lookup.service';
import {
  fieldMessage,
  parseApiError,
  type ParsedApiError,
} from '../../../../core/http/parse-api-error';
import { operatingExpenseCategoryLabel } from '../../../../core/i18n/enum-labels';
import {
  OPERATING_EXPENSE_CATEGORIES,
  type OperatingExpenseAttachmentResponse,
  type UpsertOperatingExpenseRequest,
} from '../../../../core/model/operating-expense/expense.dto';
import { ApiFormErrorComponent } from '../../../../shared/ui/api-form-error/api-form-error';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';

const MAX_ATTACHMENTS = 5;

interface PendingReceipt {
  file: File;
  url: string;
}

@Component({
  selector: 'app-operating-expense-form-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    PageHeaderComponent,
    ApiFormErrorComponent,
    HeadquarterSelectComponent,
  ],
  templateUrl: './gasto-form-page.html',
})
export class OperatingExpenseFormPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly expenses = inject(OperatingExpenseService);
  private readonly session = inject(SessionContextService);
  private readonly hqLookup = inject(HeadquarterLookupService);

  readonly saving = signal(false);
  readonly loadingExisting = signal(false);
  readonly apiError = signal<ParsedApiError | null>(null);
  readonly expenseId = signal<number | null>(null);
  readonly retained = signal<OperatingExpenseAttachmentResponse[]>([]);
  readonly pending = signal<PendingReceipt[]>([]);
  readonly previewUrl = signal<string | null>(null);
  readonly fixedHeadquarterLabel = signal<string | null>(null);
  readonly staffUnassigned = signal(false);

  readonly canManageOperatingExpenses = this.session.canManageOperatingExpenses;
  readonly maxAttachments = MAX_ATTACHMENTS;
  readonly categories = OPERATING_EXPENSE_CATEGORIES;
  readonly categoryLabel = operatingExpenseCategoryLabel;

  readonly form = this.fb.nonNullable.group({
    headquarterId: [null as number | null, Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    category: ['', Validators.required],
    concept: ['', [Validators.required, Validators.maxLength(200)]],
    notes: ['', Validators.maxLength(500)],
    occurredOn: [businessToday(new Date(), clientZone()), Validators.required],
  });

  get isEdit(): boolean {
    return this.expenseId() != null;
  }

  ngOnInit(): void {
    void this.hqLookup.ensureLoaded();
    const isEditRoute = this.route.snapshot.url.some((segment) => segment.path === 'editar');
    const idRaw = isEditRoute ? this.route.snapshot.paramMap.get('id') : null;

    this.session.ensureLoaded().subscribe({
      next: () => {
        if (!this.canManageOperatingExpenses()) {
          this.form.controls.occurredOn.disable();
          this.form.controls.occurredOn.setValue(businessToday(new Date(), clientZone()));
        }
        if (!idRaw) {
          this.prefillHeadquarter();
          return;
        }
        const id = Number(idRaw);
        if (!Number.isFinite(id)) return;
        this.expenseId.set(id);
        this.loadOperatingExpense(id);
      },
    });
  }

  ngOnDestroy(): void {
    for (const item of this.pending()) {
      URL.revokeObjectURL(item.url);
    }
  }

  attachmentCount(): number {
    return this.retained().length + this.pending().length;
  }

  headquarterLabel(): string {
    const id = this.form.controls.headquarterId.value;
    return id == null ? '—' : this.hqLookup.name(id);
  }

  apiFieldMessage(field: string): string | undefined {
    const parsed = this.apiError();
    if (!parsed) return undefined;
    return fieldMessage(parsed, field);
  }

  onHeadquarterChange(id: number | number[] | null): void {
    this.form.controls.headquarterId.setValue(typeof id === 'number' ? id : null);
    this.form.controls.headquarterId.markAsDirty();
  }

  async onFilesSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const selected = Array.from(input.files ?? []);
    input.value = '';
    await this.addFiles(selected);
  }

  removeRetained(key: string): void {
    this.retained.update((rows) => rows.filter((row) => row.key !== key));
    this.form.markAsDirty();
  }

  removePending(url: string): void {
    const current = this.pending();
    const target = current.find((item) => item.url === url);
    if (target) URL.revokeObjectURL(target.url);
    this.pending.set(current.filter((item) => item.url !== url));
    this.form.markAsDirty();
  }

  openReceipt(url: string): void {
    this.previewUrl.set(url);
  }

  closeReceipt(): void {
    this.previewUrl.set(null);
  }

  submit(): void {
    this.apiError.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const amount = Number(raw.amount);
    if (!Number.isFinite(amount) || amount <= 0) {
      this.form.controls.amount.setErrors({ min: true });
      this.form.controls.amount.markAsTouched();
      return;
    }
    const body: UpsertOperatingExpenseRequest = {
      headquarterId: raw.headquarterId,
      amount,
      category: raw.category as UpsertOperatingExpenseRequest['category'],
      concept: raw.concept.trim(),
      notes: raw.notes.trim() || null,
      occurredOn: raw.occurredOn,
    };
    if (this.isEdit) {
      body.retainAttachmentKeys = this.retained().map((row) => row.key);
    }

    this.saving.set(true);
    const files = this.pending().map((item) => item.file);
    const request = this.isEdit
      ? this.expenses.update(this.expenseId()!, body, files)
      : this.expenses.create(body, files);
    request.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        markFormPristine(this.form);
        void this.router.navigate(['/app/ops/gastos-operativos']);
      },
      error: (err: unknown) => this.apiError.set(parseApiError(err)),
    });
  }

  private prefillHeadquarter(): void {
    if (this.canManageOperatingExpenses()) {
      const active = this.session.activeHeadquarterId();
      if (active != null) this.form.controls.headquarterId.setValue(active);
      return;
    }
    const assigned = this.session.assignedHeadquarterIds();
    if (assigned.length === 0) {
      this.staffUnassigned.set(true);
      return;
    }
    if (assigned.length === 1) {
      this.form.controls.headquarterId.setValue(assigned[0]);
      this.form.controls.headquarterId.disable();
      this.fixedHeadquarterLabel.set(this.hqLookup.name(assigned[0]));
      return;
    }
    const active = this.session.activeHeadquarterId();
    const chosen = active != null && assigned.includes(active) ? active : assigned[0];
    this.form.controls.headquarterId.setValue(chosen);
  }

  private loadOperatingExpense(id: number): void {
    this.loadingExisting.set(true);
    this.expenses
      .getById(id)
      .pipe(finalize(() => this.loadingExisting.set(false)))
      .subscribe({
        next: (expense) => {
          this.form.patchValue({
            headquarterId: expense.headquarterId,
            amount: Number(expense.amount),
            category: expense.category,
            concept: expense.concept,
            notes: expense.notes ?? '',
            occurredOn: expense.occurredOn,
          });
          this.retained.set(expense.attachments);
          markFormPristine(this.form);
        },
        error: (err: unknown) => this.apiError.set(parseApiError(err)),
      });
  }

  private async addFiles(selected: File[]): Promise<void> {
    const room = MAX_ATTACHMENTS - this.attachmentCount();
    if (selected.length === 0) return;
    if (selected.length > room) {
      this.apiError.set({
        message: `Puedes adjuntar hasta ${MAX_ATTACHMENTS} comprobantes.`,
        httpStatus: 400,
        traceId: null,
        errorCode: 'VALIDATION',
        fieldErrors: [],
        context: null,
        rawBody: null,
      });
      return;
    }
    this.apiError.set(null);
    try {
      const next: PendingReceipt[] = [];
      for (const file of selected) {
        const compressed = await compressReceiptImage(file);
        next.push({ file: compressed, url: URL.createObjectURL(compressed) });
      }
      this.pending.update((current) => [...current, ...next]);
      this.form.markAsDirty();
    } catch (err: unknown) {
      this.apiError.set({
        message: err instanceof Error ? err.message : 'No se pudo preparar la imagen.',
        httpStatus: 400,
        traceId: null,
        errorCode: 'VALIDATION',
        fieldErrors: [],
        context: null,
        rawBody: null,
      });
    }
  }
}
