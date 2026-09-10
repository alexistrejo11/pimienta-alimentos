import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type {
  HeadquarterPosCatalogItemRequest,
  HeadquarterPosCatalogItemResponse,
  PosSettingsRequest,
  PosSettingsResponse,
} from '../model/pos/pos.dto';
import type { PagedResponse } from '../model/common/pagination';

/** POS por sede: /api/v1/headquarters/{id}/pos-settings y pos-catalog */
@Injectable({ providedIn: 'root' })
export class PosCatalogService {
  private readonly http = inject(HttpClient);
  private readonly hqBase = `${API_BASE_URL}/headquarters`;

  getSettings(headquarterId: number): Observable<PosSettingsResponse> {
    return this.http.get<PosSettingsResponse>(`${this.hqBase}/${headquarterId}/pos-settings`);
  }

  updateSettings(headquarterId: number, body: PosSettingsRequest): Observable<PosSettingsResponse> {
    return this.http.put<PosSettingsResponse>(`${this.hqBase}/${headquarterId}/pos-settings`, body);
  }

  listCatalog(headquarterId: number, page = 0, size = 20): Observable<PagedResponse<HeadquarterPosCatalogItemResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PagedResponse<HeadquarterPosCatalogItemResponse>>(
      `${this.hqBase}/${headquarterId}/pos-catalog`,
      { params },
    );
  }

  getCatalogItem(headquarterId: number, itemId: number): Observable<HeadquarterPosCatalogItemResponse> {
    return this.http.get<HeadquarterPosCatalogItemResponse>(
      `${this.hqBase}/${headquarterId}/pos-catalog/${itemId}`,
    );
  }

  upsertCatalogItem(
    headquarterId: number,
    itemId: number,
    body: HeadquarterPosCatalogItemRequest,
  ): Observable<HeadquarterPosCatalogItemResponse> {
    return this.http.put<HeadquarterPosCatalogItemResponse>(
      `${this.hqBase}/${headquarterId}/pos-catalog/${itemId}`,
      body,
    );
  }
}
