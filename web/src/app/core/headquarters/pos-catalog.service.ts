import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type {
  HeadquarterPosCatalogItemRequest,
  HeadquarterPosCatalogItemResponse,
  PosSettingsRequest,
  PosSettingsResponse,
  PosSaleCategoryResponse,
  CreatePosProductRequest,
  CreatedPosProductResponse,
  PosCatalogCandidateResponse,
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

  listCandidates(headquarterId: number): Observable<PosCatalogCandidateResponse[]> {
    return this.http.get<PosCatalogCandidateResponse[]>(`${this.hqBase}/${headquarterId}/pos-catalog/candidates`);
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

  deleteCatalogItem(headquarterId: number, itemId: number): Observable<HeadquarterPosCatalogItemResponse> {
    return this.http.delete<HeadquarterPosCatalogItemResponse>(`${this.hqBase}/${headquarterId}/pos-catalog/${itemId}`);
  }

  listCategories(headquarterId: number, includeInactive = false): Observable<PosSaleCategoryResponse[]> {
    const params = new HttpParams().set('includeInactive', includeInactive);
    return this.http.get<PosSaleCategoryResponse[]>(`${this.hqBase}/${headquarterId}/pos-categories`, { params });
  }

  createCategory(headquarterId: number, name: string, displayOrder = 0): Observable<PosSaleCategoryResponse> {
    return this.http.post<PosSaleCategoryResponse>(`${this.hqBase}/${headquarterId}/pos-categories`, { name, displayOrder });
  }

  updateCategory(
    headquarterId: number,
    categoryId: number,
    name: string,
    displayOrder: number,
  ): Observable<PosSaleCategoryResponse> {
    return this.http.put<PosSaleCategoryResponse>(
      `${this.hqBase}/${headquarterId}/pos-categories/${categoryId}`,
      { name, displayOrder },
    );
  }

  archiveCategory(headquarterId: number, categoryId: number): Observable<void> {
    return this.http.delete<void>(`${this.hqBase}/${headquarterId}/pos-categories/${categoryId}`);
  }

  createPosProduct(headquarterId: number, body: CreatePosProductRequest): Observable<CreatedPosProductResponse> {
    return this.http.post<CreatedPosProductResponse>(`${this.hqBase}/${headquarterId}/pos-products`, body);
  }
}
