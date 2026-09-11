import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type {
  ClientResponse,
  ClientSearchParams,
  CreateClientRequest,
} from '../model/crm/client.dto';
import type { PagedResponse } from '../model/common/pagination';

@Injectable({ providedIn: 'root' })
export class ClientService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/clients`;

  list(params: ClientSearchParams = {}): Observable<PagedResponse<ClientResponse>> {
    let httpParams = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 100));
    if (params.nameContains?.trim()) {
      httpParams = httpParams.set('nameContains', params.nameContains.trim());
    }
    return this.http.get<PagedResponse<ClientResponse>>(this.base, { params: httpParams });
  }

  create(body: CreateClientRequest): Observable<ClientResponse> {
    return this.http.post<ClientResponse>(this.base, body);
  }
}
