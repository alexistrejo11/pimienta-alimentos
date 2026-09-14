import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import type { InventoryCountStatus, InventoryCountType } from '../../../../core/model/inventory/inventory.enums';

interface RecentCount { id: number; locationId: number; type: InventoryCountType; status: InventoryCountStatus; createdAt: string; }

@Component({
  selector: 'app-count-session-list-page',
  imports: [RouterLink],
  templateUrl: './count-session-list-page.html',
})
export class CountSessionListPageComponent {
  readonly sessions = signal<RecentCount[]>(this.readSessions());

  private readSessions(): RecentCount[] {
    if (typeof localStorage === 'undefined') return [];
    try { return JSON.parse(localStorage.getItem('pimienta.inventory.counts') ?? '[]') as RecentCount[]; }
    catch { return []; }
  }
}
