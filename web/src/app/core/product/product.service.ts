import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type { PagedResponse } from '../model/common/pagination';
import type {
  ProductCreateRequest,
  ProductResponse,
  ProductSearchParams,
  ProductUpdateRequest,
} from '../model/product/product.dto';

/** Catálogo de venta: /api/v1/products */
@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/products`;

  search(params: ProductSearchParams = {}): Observable<PagedResponse<ProductResponse>> {
    let p = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20));
    if (params.search) p = p.set('search', params.search);
    if (params.status) p = p.set('status', params.status);
    return this.http.get<PagedResponse<ProductResponse>>(this.base, { params: p });
  }

  get(id: number): Observable<ProductResponse> {
    return this.http.get<ProductResponse>(`${this.base}/${id}`);
  }

  lookup(q: string): Observable<ProductResponse> {
    const params = new HttpParams().set('q', q);
    return this.http.get<ProductResponse>(`${this.base}/lookup`, { params });
  }

  create(body: ProductCreateRequest): Observable<ProductResponse> {
    return this.http.post<ProductResponse>(this.base, body);
  }

  update(id: number, body: ProductUpdateRequest): Observable<ProductResponse> {
    return this.http.put<ProductResponse>(`${this.base}/${id}`, body);
  }
}
