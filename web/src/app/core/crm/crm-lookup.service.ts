import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { CrmService } from './crm.service';

@Injectable({ providedIn: 'root' })
export class CrmLookupService {
  private readonly crm = inject(CrmService);

  private readonly opportunityNames = signal<ReadonlyMap<number, string>>(new Map());
  private readonly projectNames = signal<ReadonlyMap<number, string>>(new Map());
  private oppLoadPromise: Promise<void> | null = null;
  private projLoadPromise: Promise<void> | null = null;

  opportunityName(id: number): string {
    return this.opportunityNames().get(id) ?? `#${id}`;
  }

  projectName(id: number): string {
    return this.projectNames().get(id) ?? `#${id}`;
  }

  ensureOpportunitiesLoaded(): Promise<void> {
    if (this.opportunityNames().size > 0) {
      return Promise.resolve();
    }
    if (this.oppLoadPromise) {
      return this.oppLoadPromise;
    }

    this.oppLoadPromise = firstValueFrom(this.crm.listOpportunities({ page: 0, size: 100 }))
      .then((page) => {
        const map = new Map<number, string>();
        for (const o of page.items) {
          map.set(o.id, o.title);
        }
        this.opportunityNames.set(map);
      })
      .catch(() => {
        this.oppLoadPromise = null;
      });

    return this.oppLoadPromise;
  }

  ensureProjectsLoaded(): Promise<void> {
    if (this.projectNames().size > 0) {
      return Promise.resolve();
    }
    if (this.projLoadPromise) {
      return this.projLoadPromise;
    }

    this.projLoadPromise = firstValueFrom(this.crm.listProjects({ page: 0, size: 100 }))
      .then((page) => {
        const map = new Map<number, string>();
        for (const p of page.items) {
          map.set(p.id, `${p.projectCode} — ${p.projectName}`);
        }
        this.projectNames.set(map);
      })
      .catch(() => {
        this.projLoadPromise = null;
      });

    return this.projLoadPromise;
  }
}
