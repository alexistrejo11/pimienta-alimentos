import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type {
  CreateInitialStockRequest,
  InventoryStockResponse,
  InventoryStockSearchParams,
  ItemCreateRequest,
  ItemResponse,
  ItemSearchParams,
  ItemUpdateRequest,
  StorageLocationResponse,
  StorageLocationSearchParams,
} from '../model/inventory/inventory.dto';
import type { PagedResponse } from '../model/common/pagination';

/** Llamadas al módulo de inventario: /api/v1/inventory */
@Injectable({ providedIn: 'root' })
export class InventoryService {
  private readonly http = inject(HttpClient);
  private readonly itemsBase = `${API_BASE_URL}/inventory/items`;
  private readonly stockBase = `${API_BASE_URL}/inventory/stock`;
  private readonly locationsBase = `${API_BASE_URL}/inventory/locations`;

  // ── Ítems maestro ─────────────────────────────────────────────────────────

  searchItems(params: ItemSearchParams = {}): Observable<PagedResponse<ItemResponse>> {
    let p = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20));
    if (params.name) p = p.set('name', params.name);
    if (params.sku) p = p.set('sku', params.sku);
    if (params.category) p = p.set('category', params.category);
    if (params.status) p = p.set('status', params.status);
    return this.http.get<PagedResponse<ItemResponse>>(this.itemsBase, { params: p });
  }

  getItem(id: number): Observable<ItemResponse> {
    return this.http.get<ItemResponse>(`${this.itemsBase}/${id}`);
  }

  lookupItem(q: string): Observable<ItemResponse> {
    const params = new HttpParams().set('q', q);
    return this.http.get<ItemResponse>(`${this.itemsBase}/lookup`, { params });
  }

  createItem(body: ItemCreateRequest): Observable<ItemResponse> {
    return this.http.post<ItemResponse>(this.itemsBase, body);
  }

  updateItem(id: number, body: ItemUpdateRequest): Observable<ItemResponse> {
    return this.http.put<ItemResponse>(`${this.itemsBase}/${id}`, body);
  }

  discontinueItem(id: number): Observable<ItemResponse> {
    return this.http.put<ItemResponse>(`${this.itemsBase}/${id}/discontinue`, {});
  }

  activateItem(id: number): Observable<ItemResponse> {
    return this.http.put<ItemResponse>(`${this.itemsBase}/${id}/activate`, {});
  }

  // ── Stock ─────────────────────────────────────────────────────────────────

  searchStock(params: InventoryStockSearchParams = {}): Observable<PagedResponse<InventoryStockResponse>> {
    let p = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20));
    if (params.itemId != null) p = p.set('itemId', String(params.itemId));
    if (params.locationId != null) p = p.set('locationId', String(params.locationId));
    if (params.status) p = p.set('status', params.status);
    return this.http.get<PagedResponse<InventoryStockResponse>>(this.stockBase, { params: p });
  }

  createInitialStock(body: CreateInitialStockRequest): Observable<InventoryStockResponse> {
    return this.http.post<InventoryStockResponse>(this.stockBase, body);
  }

  // ── Ubicaciones ───────────────────────────────────────────────────────────

  searchLocations(params: StorageLocationSearchParams = {}): Observable<PagedResponse<StorageLocationResponse>> {
    let p = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 50));
    if (params.type) p = p.set('type', params.type);
    return this.http.get<PagedResponse<StorageLocationResponse>>(this.locationsBase, { params: p });
  }
}
