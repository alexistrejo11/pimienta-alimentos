import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { HeadquarterService } from './headquarter.service';

/** In-memory id → name cache for POS tables and labels. */
@Injectable({ providedIn: 'root' })
export class HeadquarterLookupService {
  private readonly hqService = inject(HeadquarterService);

  private readonly names = signal<ReadonlyMap<number, string>>(new Map());
  private loadPromise: Promise<void> | null = null;

  /** Resolve headquarter name; falls back to `#id` until cache is warm. */
  name(id: number): string {
    return this.names().get(id) ?? `#${id}`;
  }

  /** Comma-separated names for a list of IDs. */
  namesList(ids: number[]): string {
    if (ids.length === 0) return '—';
    return ids.map((id) => this.name(id)).join(', ');
  }

  /** Load all headquarters into cache (idempotent). */
  ensureLoaded(): Promise<void> {
    if (this.names().size > 0) {
      return Promise.resolve();
    }
    if (this.loadPromise) {
      return this.loadPromise;
    }

    this.loadPromise = firstValueFrom(this.hqService.list(0, 100))
      .then((page) => {
        const map = new Map<number, string>();
        for (const hq of page.content) {
          map.set(hq.id, hq.name);
        }
        this.names.set(map);
      })
      .catch(() => {
        this.loadPromise = null;
      });

    return this.loadPromise;
  }
}
