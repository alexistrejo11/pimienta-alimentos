import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ClientService } from './client.service';

@Injectable({ providedIn: 'root' })
export class ClientLookupService {
  private readonly clients = inject(ClientService);

  private readonly names = signal<ReadonlyMap<number, string>>(new Map());
  private loadPromise: Promise<void> | null = null;

  name(id: number): string {
    const cached = this.names().get(id);
    if (cached) return cached;
    return `#${id}`;
  }

  label(id: number, companyName?: string | null): string {
    const n = this.name(id);
    if (n.startsWith('#') && companyName) {
      return companyName;
    }
    return n;
  }

  ensureLoaded(): Promise<void> {
    if (this.names().size > 0) {
      return Promise.resolve();
    }
    if (this.loadPromise) {
      return this.loadPromise;
    }

    this.loadPromise = firstValueFrom(this.clients.list({ page: 0, size: 200 }))
      .then((page) => {
        const map = new Map<number, string>();
        for (const c of page.items) {
          map.set(c.id, c.companyName ? `${c.name} (${c.companyName})` : c.name);
        }
        this.names.set(map);
      })
      .catch(() => {
        this.loadPromise = null;
      });

    return this.loadPromise;
  }
}
