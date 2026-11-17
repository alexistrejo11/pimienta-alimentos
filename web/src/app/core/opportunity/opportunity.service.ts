import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type { PagedResponse } from '../model/common/pagination';
import type {
  CreateOpportunityRequest,
  OpportunityResponse,
  UpdateOpportunityRequest,
} from '../model/opportunity/opportunity.dto';
import type { OpportunityStatus } from '../model/opportunity/opportunity.enums';

/** /api/v1/opportunities */
@Injectable({ providedIn: 'root' })
export class OpportunityService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/opportunities`;

  list(opts: {
    page?: number;
    size?: number;
    search?: string;
    organizationId?: number | null;
    status?: OpportunityStatus | '';
  } = {}): Observable<PagedResponse<OpportunityResponse>> {
    let params = new HttpParams()
      .set('page', String(opts.page ?? 0))
      .set('size', String(opts.size ?? 50));
    if (opts.search?.trim()) params = params.set('search', opts.search.trim());
    if (opts.organizationId != null) {
      params = params.set('organizationId', String(opts.organizationId));
    }
    if (opts.status) params = params.set('status', opts.status);
    return this.http.get<PagedResponse<OpportunityResponse>>(this.base, { params });
  }

  getById(id: number): Observable<OpportunityResponse> {
    return this.http.get<OpportunityResponse>(`${this.base}/${id}`);
  }

  create(body: CreateOpportunityRequest): Observable<OpportunityResponse> {
    return this.http.post<OpportunityResponse>(this.base, body);
  }

  update(id: number, body: UpdateOpportunityRequest): Observable<OpportunityResponse> {
    return this.http.put<OpportunityResponse>(`${this.base}/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  submitForReview(id: number): Observable<OpportunityResponse> {
    return this.http.post<OpportunityResponse>(`${this.base}/${id}/submit-for-review`, null);
  }

  startPreparing(id: number): Observable<OpportunityResponse> {
    return this.http.post<OpportunityResponse>(`${this.base}/${id}/start-preparing`, null);
  }

  markSubmitted(id: number): Observable<OpportunityResponse> {
    return this.http.post<OpportunityResponse>(`${this.base}/${id}/mark-submitted`, null);
  }

  markWon(id: number): Observable<OpportunityResponse> {
    return this.http.post<OpportunityResponse>(`${this.base}/${id}/mark-won`, null);
  }

  markLost(id: number): Observable<OpportunityResponse> {
    return this.http.post<OpportunityResponse>(`${this.base}/${id}/mark-lost`, null);
  }

  abandon(id: number): Observable<OpportunityResponse> {
    return this.http.post<OpportunityResponse>(`${this.base}/${id}/abandon`, null);
  }

  cancel(id: number): Observable<OpportunityResponse> {
    return this.http.post<OpportunityResponse>(`${this.base}/${id}/cancel`, null);
  }
}
