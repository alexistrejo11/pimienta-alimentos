import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { EmployeeService } from './employee.service';

@Injectable({ providedIn: 'root' })
export class EmployeeLookupService {
  private readonly employees = inject(EmployeeService);

  private readonly names = signal<ReadonlyMap<number, string>>(new Map());
  private loadPromise: Promise<void> | null = null;

  name(id: number): string {
    return this.names().get(id) ?? `#${id}`;
  }

  ensureLoaded(): Promise<void> {
    if (this.names().size > 0) {
      return Promise.resolve();
    }
    if (this.loadPromise) {
      return this.loadPromise;
    }

    this.loadPromise = firstValueFrom(this.employees.listActive(0, 200))
      .then((page) => {
        const map = new Map<number, string>();
        for (const e of page.items) {
          map.set(e.id, e.fullName);
        }
        this.names.set(map);
      })
      .catch(() => {
        this.loadPromise = null;
      });

    return this.loadPromise;
  }
}
