import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type { ContractResponse, CreateOrUpdateContractRequest } from '../model/contract/contract.dto';
import type { PagedResponse } from '../model/common/pagination';

/** Llamadas al módulo de contratos: /api/v1/contracts */
@Injectable({ providedIn: 'root' })
export class ContractService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/contracts`;

  list(page = 0, size = 20): Observable<PagedResponse<ContractResponse>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<PagedResponse<ContractResponse>>(this.base, { params });
  }

  create(body: CreateOrUpdateContractRequest): Observable<ContractResponse> {
    return this.http.post<ContractResponse>(this.base, body);
  }
}
