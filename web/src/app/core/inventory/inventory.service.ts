import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type {
  AdjustmentTransactionRequest,
  CreateInitialStockRequest,
  InventoryCountResponseRequest,
  InventoryCountSearchParams,
  InventoryCountSessionResponse,
  InventoryCountSessionSummaryResponse,
  InventoryStockResponse,
  InventoryStockSearchParams,
  InventoryTransactionResponse,
  ItemCreateRequest,
  ItemResponse,
  ItemSearchParams,
  ItemUpdateRequest,
  OpenInventoryCountRequest,
  PurchaseTransactionRequest,
  ScrapTransactionRequest,
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
  private readonly countsBase = `${API_BASE_URL}/inventory/count-sessions`;
  private readonly transactionsBase = `${API_BASE_URL}/inventory/transactions`;

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

  searchStock(params: InventoryStockSearchParams = {}): Observable<PagedResponse<InventoryStockResponse>> {
    let p = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20));
    if (params.itemId != null) p = p.set('itemId', String(params.itemId));
    if (params.locationId != null) p = p.set('locationId', String(params.locationId));
    if (params.status) p = p.set('status', params.status);
    if (params.headquarterId != null) p = p.set('headquarterId', String(params.headquarterId));
    return this.http.get<PagedResponse<InventoryStockResponse>>(this.stockBase, { params: p });
  }

  createInitialStock(body: CreateInitialStockRequest): Observable<InventoryStockResponse> {
    return this.http.post<InventoryStockResponse>(this.stockBase, body);
  }

  searchLocations(params: StorageLocationSearchParams = {}): Observable<PagedResponse<StorageLocationResponse>> {
    let p = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 50));
    if (params.type) p = p.set('type', params.type);
    if (params.headquarterId != null) p = p.set('headquarterId', String(params.headquarterId));
    return this.http.get<PagedResponse<StorageLocationResponse>>(this.locationsBase, { params: p });
  }

  searchCounts(params: InventoryCountSearchParams = {}): Observable<PagedResponse<InventoryCountSessionSummaryResponse>> {
    let p = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20));
    if (params.locationId != null) p = p.set('locationId', String(params.locationId));
    if (params.status) p = p.set('status', params.status);
    if (params.headquarterId != null) p = p.set('headquarterId', String(params.headquarterId));
    return this.http.get<PagedResponse<InventoryCountSessionSummaryResponse>>(this.countsBase, { params: p });
  }

  openCount(body: OpenInventoryCountRequest): Observable<InventoryCountSessionResponse> {
    return this.http.post<InventoryCountSessionResponse>(this.countsBase, body);
  }

  getCount(id: number): Observable<InventoryCountSessionResponse> {
    return this.http.get<InventoryCountSessionResponse>(`${this.countsBase}/${id}`);
  }

  respondToCount(id: number, body: InventoryCountResponseRequest): Observable<InventoryCountSessionResponse> {
    return this.http.post<InventoryCountSessionResponse>(`${this.countsBase}/${id}/responses`, body);
  }

  submitCount(id: number): Observable<InventoryCountSessionResponse> {
    return this.http.post<InventoryCountSessionResponse>(`${this.countsBase}/${id}/submit`, {});
  }

  approveCount(id: number): Observable<InventoryCountSessionResponse> {
    return this.http.post<InventoryCountSessionResponse>(`${this.countsBase}/${id}/approve`, {});
  }

  cancelCount(id: number): Observable<void> {
    return this.http.post<void>(`${this.countsBase}/${id}/cancel`, {});
  }

  purchase(body: PurchaseTransactionRequest): Observable<InventoryTransactionResponse> {
    return this.http.post<InventoryTransactionResponse>(`${this.transactionsBase}/purchase`, body);
  }

  scrap(body: ScrapTransactionRequest): Observable<InventoryTransactionResponse> {
    return this.http.post<InventoryTransactionResponse>(`${this.transactionsBase}/scrap`, body);
  }

  adjustment(body: AdjustmentTransactionRequest): Observable<InventoryTransactionResponse> {
    return this.http.post<InventoryTransactionResponse>(`${this.transactionsBase}/adjustment`, body);
  }
}
