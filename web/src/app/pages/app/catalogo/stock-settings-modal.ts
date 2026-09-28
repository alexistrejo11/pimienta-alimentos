import { Component, DestroyRef, HostListener, inject, input, OnInit, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import { InventoryService } from '../../../core/inventory/inventory.service';
import { itemKindLabel, itemStatusLabel } from '../../../core/i18n/enum-labels';
import type { ItemResponse } from '../../../core/model/inventory/inventory.dto';
import type { ItemStatus } from '../../../core/model/inventory/inventory.enums';

@Component({
  selector: 'app-stock-settings-modal',
  imports: [ReactiveFormsModule],
  templateUrl: './stock-settings-modal.html',
})
export class StockSettingsModalComponent implements OnInit {
  private readonly inventory = inject(InventoryService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly item = input.required<ItemResponse>();

  readonly cerrar = output<void>();
  readonly guardado = output<ItemResponse>();

  readonly saving = signal(false);
  readonly error = signal<ParsedApiError | null>(null);

  readonly statuses: ItemStatus[] = ['ACTIVE', 'DISCONTINUED', 'OUT_OF_STOCK', 'PENDING_APPROVAL'];
  readonly itemStatusLabel = itemStatusLabel;
  readonly itemKindLabel = itemKindLabel;

  readonly form = this.fb.nonNullable.group({
    costPrice: [0, [Validators.required, Validators.min(0)]],
    reorderPoint: [0, [Validators.required, Validators.min(0)]],
    reorderQuantity: [0, [Validators.required, Validators.min(0)]],
    status: ['ACTIVE' as ItemStatus, Validators.required],
  });

  ngOnInit(): void {
    const item = this.item();
    this.form.reset({
      costPrice: item.costPrice,
      reorderPoint: item.reorderPoint,
      reorderQuantity: item.reorderQuantity,
      status: item.status,
    });
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

  guardar(): void {
    if (this.saving() || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.inventory
      .updateStockSettings(this.item().id, this.form.getRawValue())
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (updated) => this.guardado.emit(updated),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
