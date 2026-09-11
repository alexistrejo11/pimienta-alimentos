import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { payrollFrequencyLabel } from '../../../core/i18n/enum-labels';
import { PayrollLookupService } from '../../../core/payroll/payroll-lookup.service';
import { PayrollService } from '../../../core/payroll/payroll.service';
import type { PayrollPeriodResponse } from '../../../core/model/payroll/payroll.dto';

@Component({
  selector: 'app-payroll-period-select',
  imports: [FormsModule],
  templateUrl: './payroll-period-select.html',
})
export class PayrollPeriodSelectComponent implements OnInit {
  private readonly payroll = inject(PayrollService);
  private readonly lookup = inject(PayrollLookupService);

  readonly label = input('Período');
  readonly allowNull = input(true);
  readonly nullLabel = input('Opcional');
  readonly disabled = input(false);

  readonly value = input<number | null>(null);
  readonly valueChange = output<number | null>();

  readonly options = signal<PayrollPeriodResponse[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.payroll.listPeriods(0, 100).subscribe({
      next: (page) => {
        this.options.set(page.items);
        void this.lookup.ensureLoaded();
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected onChange(id: number | null): void {
    this.valueChange.emit(id);
  }

  protected optionLabel(p: PayrollPeriodResponse): string {
    return `${p.startDate} — ${p.endDate} (${payrollFrequencyLabel(p.frequency)})`;
  }
}
