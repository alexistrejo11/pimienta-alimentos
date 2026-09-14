import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { InventoryService } from '../../../../core/inventory/inventory.service';
import { SessionContextService } from '../../../../core/auth/session-context.service';
import { parseApiError } from '../../../../core/http/parse-api-error';
import type { InventoryCountSessionResponse, InventoryStockResponse } from '../../../../core/model/inventory/inventory.dto';

@Component({ selector: 'app-count-session-detail-page', imports: [FormsModule, RouterLink], templateUrl: './count-session-detail-page.html' })
export class CountSessionDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute); private readonly router = inject(Router); private readonly inventory = inject(InventoryService); private readonly sessionContext = inject(SessionContextService);
  readonly session = signal<InventoryCountSessionResponse | null>(null); readonly stock = signal<InventoryStockResponse[]>([]); readonly error = signal(''); readonly busy = signal(false);
  readonly canApprove = computed(() => this.sessionContext.isAdmin() || this.sessionContext.isManager()); readonly review = this.route.snapshot.data['review'] === true;
  quantities: Record<number, number> = {};
  ngOnInit(): void { const id = Number(this.route.snapshot.paramMap.get('id')); this.load(id); }
  load(id: number): void { this.inventory.getCount(id).subscribe({ next: s => { this.session.set(s); for (const response of s.responses) if (response.countedQuantity != null) this.quantities[response.itemId] = response.countedQuantity; this.inventory.searchStock({ locationId: s.locationId, size: 100 }).subscribe(p => this.stock.set(p.items)); }, error: err => this.error.set(parseApiError(err).message) }); }
  itemName(itemId: number): string { return this.stock().find(item => item.itemId === itemId)?.itemName ?? `Producto #${itemId}`; }
  save(itemId: number): void { const value = this.quantities[itemId]; if (!Number.isInteger(value) || value < 0 || !this.session()) return; this.busy.set(true); this.inventory.respondToCount(this.session()!.id, { itemId, countedQuantity: value }).subscribe({ next: s => { this.session.set(s); this.busy.set(false); }, error: err => { this.error.set(parseApiError(err).message); this.busy.set(false); } }); }
  submit(): void { const s = this.session(); if (!s) return; this.busy.set(true); this.inventory.submitCount(s.id).subscribe({ next: updated => { this.session.set(updated); this.busy.set(false); }, error: err => { this.error.set(parseApiError(err).message); this.busy.set(false); } }); }
  approve(): void { const s = this.session(); if (!s) return; this.busy.set(true); this.inventory.approveCount(s.id).subscribe({ next: updated => { this.session.set(updated); this.busy.set(false); }, error: err => { this.error.set(parseApiError(err).message); this.busy.set(false); } }); }
  cancel(): void { const s = this.session(); if (!s || !confirm('¿Cancelar esta sesión de conteo?')) return; this.busy.set(true); this.inventory.cancelCount(s.id).subscribe({ next: () => void this.router.navigate(['/app/inventario/conteos']), error: err => { this.error.set(parseApiError(err).message); this.busy.set(false); } }); }
  isEditable(): boolean { return this.session()?.status === 'DRAFT'; }
}
