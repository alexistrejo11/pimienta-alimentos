import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { API_BASE_URL } from '../config/api.config';
import type { PagedResponse } from '../model/common/pagination';
import type {
  SupplierResponse,
  SupplierSearchParams,
  UpsertSupplierRequest,
} from '../model/supplier/supplier.dto';

/** Llamadas al módulo de proveedores: /api/v1/suppliers */
@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/suppliers`;

  list(params: SupplierSearchParams = {}): Observable<PagedResponse<SupplierResponse>> {
    let httpParams = new HttpParams()
      .set('page', params.page ?? 0)
      .set('size', params.size ?? 20);
    if (params.headquarterId != null) {
      httpParams = httpParams.set('headquarterId', params.headquarterId);
    }
    if (params.search?.trim()) {
      httpParams = httpParams.set('search', params.search.trim());
    }
    return this.http.get<PagedResponse<SupplierResponse>>(this.base, { params: httpParams });
  }

  getById(id: number): Observable<SupplierResponse> {
    return this.http.get<SupplierResponse>(`${this.base}/${id}`);
  }

  create(body: UpsertSupplierRequest): Observable<SupplierResponse> {
    return this.http.post<SupplierResponse>(this.base, body);
  }

  update(id: number, body: UpsertSupplierRequest): Observable<SupplierResponse> {
    return this.http.put<SupplierResponse>(`${this.base}/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete(`${this.base}/${id}`, { observe: 'response' }).pipe(map(() => undefined));
  }
}
