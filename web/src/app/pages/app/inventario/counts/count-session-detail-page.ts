import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { InventoryService } from '../../../../core/inventory/inventory.service';
import { SessionContextService } from '../../../../core/auth/session-context.service';
import { parseApiError } from '../../../../core/http/parse-api-error';
import type { InventoryCountSessionResponse } from '../../../../core/model/inventory/inventory.dto';

@Component({
  selector: 'app-count-session-detail-page',
  imports: [FormsModule, RouterLink],
  templateUrl: './count-session-detail-page.html',
})
export class CountSessionDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly inventory = inject(InventoryService);
  private readonly sessionContext = inject(SessionContextService);

  readonly session = signal<InventoryCountSessionResponse | null>(null);
  readonly itemNames = signal<Record<number, string>>({});
  readonly error = signal('');
  readonly busy = signal(false);

  readonly canApprove = computed(
    () =>
      this.sessionContext.isAdmin() &&
      this.session()?.createdById !== this.sessionContext.userId() &&
      this.session()?.status === 'SUBMITTED',
  );
  readonly showVariance = computed(() => {
    const status = this.session()?.status;
    return status === 'APPROVED' || (status === 'SUBMITTED' && this.sessionContext.isAdmin());
  });

  quantities: Record<number, number> = {};

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.load(id);
  }

  load(id: number): void {
    this.inventory.getCount(id).subscribe({
      next: (s) => {
        this.session.set(s);
        for (const response of s.responses) {
          if (response.countedQuantity != null) {
            this.quantities[response.itemId] = response.countedQuantity;
          }
        }
        const requests = s.responses.map((r) => this.inventory.getItem(r.itemId));
        if (requests.length === 0) {
          this.itemNames.set({});
          return;
        }
        forkJoin(requests).subscribe({
          next: (items) => {
            const names: Record<number, string> = {};
            items.forEach((item) => {
              names[item.id] = `${item.sku} · ${item.name}`;
            });
            this.itemNames.set(names);
          },
          error: () => this.itemNames.set({}),
        });
      },
      error: (err) => this.error.set(parseApiError(err).message),
    });
  }

  itemName(itemId: number): string {
    return this.itemNames()[itemId] ?? `Producto #${itemId}`;
  }

  save(itemId: number): void {
    const value = this.quantities[itemId];
    if (!Number.isInteger(value) || value < 0 || !this.session()) return;
    this.busy.set(true);
    this.inventory.respondToCount(this.session()!.id, { itemId, countedQuantity: value }).subscribe({
      next: (s) => {
        this.session.set(s);
        this.busy.set(false);
      },
      error: (err) => {
        this.error.set(parseApiError(err).message);
        this.busy.set(false);
      },
    });
  }

  submit(): void {
    const s = this.session();
    if (!s) return;
    this.busy.set(true);
    this.inventory.submitCount(s.id).subscribe({
      next: (updated) => {
        this.session.set(updated);
        this.busy.set(false);
      },
      error: (err) => {
        this.error.set(parseApiError(err).message);
        this.busy.set(false);
      },
    });
  }

  approve(): void {
    const s = this.session();
    if (!s) return;
    this.busy.set(true);
    this.inventory.approveCount(s.id).subscribe({
      next: (updated) => {
        this.session.set(updated);
        this.busy.set(false);
      },
      error: (err) => {
        this.error.set(parseApiError(err).message);
        this.busy.set(false);
      },
    });
  }

  cancel(): void {
    const s = this.session();
    if (!s || !confirm('¿Cancelar esta sesión de conteo?')) return;
    this.busy.set(true);
    this.inventory.cancelCount(s.id).subscribe({
      next: () => void this.router.navigate(['/app/inventario/conteos']),
      error: (err) => {
        this.error.set(parseApiError(err).message);
        this.busy.set(false);
      },
    });
  }

  isEditable(): boolean {
    return this.session()?.status === 'DRAFT';
  }
}
