import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type { PagedResponse } from '../model/common/pagination';
import type {
  OrganizationResponse,
  UpsertOrganizationRequest,
} from '../model/opportunity/opportunity.dto';
import type { OrganizationKind } from '../model/opportunity/opportunity.enums';

/** /api/v1/organizations */
@Injectable({ providedIn: 'root' })
export class OrganizationService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/organizations`;

  list(opts: {
    page?: number;
    size?: number;
    search?: string;
    kind?: OrganizationKind | '';
  } = {}): Observable<PagedResponse<OrganizationResponse>> {
    let params = new HttpParams()
      .set('page', String(opts.page ?? 0))
      .set('size', String(opts.size ?? 50));
    if (opts.search?.trim()) params = params.set('search', opts.search.trim());
    if (opts.kind) params = params.set('kind', opts.kind);
    return this.http.get<PagedResponse<OrganizationResponse>>(this.base, { params });
  }

  getById(id: number): Observable<OrganizationResponse> {
    return this.http.get<OrganizationResponse>(`${this.base}/${id}`);
  }

  create(body: UpsertOrganizationRequest): Observable<OrganizationResponse> {
    return this.http.post<OrganizationResponse>(this.base, body);
  }

  update(id: number, body: UpsertOrganizationRequest): Observable<OrganizationResponse> {
    return this.http.put<OrganizationResponse>(`${this.base}/${id}`, body);
  }
}
