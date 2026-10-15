import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { API_BASE_URL } from '../config/api.config';
import type { PagedResponse } from '../model/common/pagination';
import type {
  OperatingExpenseResponse,
  OperatingExpenseSearchParams,
  OperatingExpenseSummaryResponse,
  UpsertOperatingExpenseRequest,
} from '../model/operating-expense/expense.dto';

function jsonPart(body: UpsertOperatingExpenseRequest): Blob {
  const data: Record<string, unknown> = {
    amount: body.amount,
    category: body.category,
    concept: body.concept,
  };
  if (body.headquarterId != null) {
    data['headquarterId'] = body.headquarterId;
  }
  if (body.notes != null && body.notes.trim() !== '') {
    data['notes'] = body.notes.trim();
  }
  if (body.occurredOn) {
    data['occurredOn'] = body.occurredOn;
  }
  if (body.retainAttachmentKeys) {
    data['retainAttachmentKeys'] = body.retainAttachmentKeys;
  }
  return new Blob([JSON.stringify(data)], { type: 'application/json' });
}

function multipart(body: UpsertOperatingExpenseRequest, files: readonly File[]): FormData {
  const form = new FormData();
  form.append('data', jsonPart(body));
  for (const file of files) {
    form.append('attachments', file, file.name);
  }
  return form;
}

/** Llamadas al módulo de gastos: /api/v1/operating-expenses */
@Injectable({ providedIn: 'root' })
export class OperatingExpenseService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/operating-expenses`;

  list(params: OperatingExpenseSearchParams = {}): Observable<PagedResponse<OperatingExpenseResponse>> {
    let httpParams = new HttpParams().set('page', params.page ?? 0).set('size', params.size ?? 20);
    if (params.headquarterId != null) {
      httpParams = httpParams.set('headquarterId', params.headquarterId);
    }
    if (params.search?.trim()) {
      httpParams = httpParams.set('search', params.search.trim());
    }
    if (params.from) {
      httpParams = httpParams.set('from', params.from);
    }
    if (params.to) {
      httpParams = httpParams.set('to', params.to);
    }
    if (params.category) {
      httpParams = httpParams.set('category', params.category);
    }
    return this.http.get<PagedResponse<OperatingExpenseResponse>>(this.base, { params: httpParams });
  }

  summary(params: OperatingExpenseSearchParams = {}): Observable<OperatingExpenseSummaryResponse> {
    let httpParams = new HttpParams();
    if (params.headquarterId != null) {
      httpParams = httpParams.set('headquarterId', params.headquarterId);
    }
    if (params.from) {
      httpParams = httpParams.set('from', params.from);
    }
    if (params.to) {
      httpParams = httpParams.set('to', params.to);
    }
    if (params.category) {
      httpParams = httpParams.set('category', params.category);
    }
    return this.http.get<OperatingExpenseSummaryResponse>(`${this.base}/summary`, { params: httpParams });
  }

  getById(id: number): Observable<OperatingExpenseResponse> {
    return this.http.get<OperatingExpenseResponse>(`${this.base}/${id}`);
  }

  create(body: UpsertOperatingExpenseRequest, files: readonly File[] = []): Observable<OperatingExpenseResponse> {
    return this.http.post<OperatingExpenseResponse>(this.base, multipart(body, files));
  }

  update(id: number, body: UpsertOperatingExpenseRequest, files: readonly File[] = []): Observable<OperatingExpenseResponse> {
    return this.http.put<OperatingExpenseResponse>(`${this.base}/${id}`, multipart(body, files));
  }

  delete(id: number): Observable<void> {
    return this.http.delete(`${this.base}/${id}`, { observe: 'response' }).pipe(map(() => undefined));
  }
}
