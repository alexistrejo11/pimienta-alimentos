import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { PayrollService } from './payroll.service';
import { payrollFrequencyLabel } from '../i18n/enum-labels';

@Injectable({ providedIn: 'root' })
export class PayrollLookupService {
  private readonly payroll = inject(PayrollService);

  private readonly labels = signal<ReadonlyMap<number, string>>(new Map());
  private loadPromise: Promise<void> | null = null;

  periodLabel(id: number): string {
    return this.labels().get(id) ?? `#${id}`;
  }

  ensureLoaded(): Promise<void> {
    if (this.labels().size > 0) {
      return Promise.resolve();
    }
    if (this.loadPromise) {
      return this.loadPromise;
    }

    this.loadPromise = firstValueFrom(this.payroll.listPeriods(0, 100))
      .then((page) => {
        const map = new Map<number, string>();
        for (const p of page.items) {
          const freq = payrollFrequencyLabel(p.frequency);
          map.set(p.id, `${p.startDate} — ${p.endDate} (${freq})`);
        }
        this.labels.set(map);
      })
      .catch(() => {
        this.loadPromise = null;
      });

    return this.loadPromise;
  }
}
