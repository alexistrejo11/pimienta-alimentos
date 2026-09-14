import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { InventoryService } from '../../../../core/inventory/inventory.service';
import { SessionContextService } from '../../../../core/auth/session-context.service';
import type { InventoryStockResponse, StorageLocationResponse } from '../../../../core/model/inventory/inventory.dto';
import type { InventoryCountStatus, InventoryCountType } from '../../../../core/model/inventory/inventory.enums';

@Component({ selector: 'app-count-session-create-page', imports: [FormsModule], templateUrl: './count-session-create-page.html' })
export class CountSessionCreatePageComponent implements OnInit {
  private readonly inventory = inject(InventoryService); private readonly session = inject(SessionContextService); private readonly router = inject(Router);
  readonly locations = signal<StorageLocationResponse[]>([]); readonly stock = signal<InventoryStockResponse[]>([]); readonly busy = signal(false); readonly error = signal('');
  locationId: number | null = null; type: InventoryCountType = 'FULL'; selected = new Set<number>();
  ngOnInit(): void { this.inventory.searchLocations({ type: 'POS', size: 100 }).subscribe({ next: p => this.locations.set(p.items), error: () => this.error.set('No se pudieron cargar las ubicaciones.') }); }
  loadStock(): void { if (this.locationId == null) return; this.inventory.searchStock({ locationId: this.locationId, size: 100 }).subscribe(p => { this.stock.set(p.items); this.selected = new Set(p.items.map(i => i.itemId)); }); }
  toggle(id: number): void { this.selected.has(id) ? this.selected.delete(id) : this.selected.add(id); }
  open(): void { if (this.locationId == null || (this.type === 'PARTIAL' && this.selected.size === 0)) return; this.busy.set(true); this.error.set(''); this.inventory.openCount({ locationId: this.locationId, type: this.type, itemIds: this.type === 'PARTIAL' ? [...this.selected] : undefined }).subscribe({ next: s => { this.remember(s); void this.router.navigate(['/app/inventario/conteos', s.id]); }, error: () => { this.error.set('No se pudo abrir el conteo.'); this.busy.set(false); } }); }
  private remember(s: { id: number; locationId: number; type: InventoryCountType; status: InventoryCountStatus; createdAt: string }): void { const key = 'pimienta.inventory.counts'; const old = typeof localStorage === 'undefined' ? [] : JSON.parse(localStorage.getItem(key) ?? '[]') as unknown[]; localStorage.setItem(key, JSON.stringify([{ id: s.id, locationId: s.locationId, type: s.type, status: s.status, createdAt: s.createdAt }, ...old.filter((x): x is { id: number } => typeof x === 'object' && x !== null && 'id' in x && x.id !== s.id)])); }
}
