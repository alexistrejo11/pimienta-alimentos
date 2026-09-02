import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type { PagedResponse } from '../model/common/pagination';
import type {
  PayrollBulkScopeParams,
  PayrollPeriodResponse,
  PayrollRecordResponse,
  PayrollRecordSearchParams,
  PayrollPaymentResponse,
  PayrollSummaryResponse,
  PayrollDebtResponse,
  RegisterPayrollAdjustmentRequest,
  RegisterPayrollPaymentRequest,
  RegisterPayrollPeriodRequest,
  RegisterPayrollRecordRequest,
  SpreadsheetBulkImportResult,
} from '../model/payroll/payroll.dto';

/** Llamadas al módulo de nómina: `/api/v1/payroll`. */
@Injectable({ providedIn: 'root' })
export class PayrollService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/payroll`;

  getSummary(from?: string | null, to?: string | null): Observable<PayrollSummaryResponse> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<PayrollSummaryResponse>(`${this.base}/summary`, { params });
  }

  listRecords(search: PayrollRecordSearchParams = {}): Observable<PagedResponse<PayrollRecordResponse>> {
    let params = new HttpParams()
      .set('page', String(search.page ?? 0))
      .set('size', String(search.size ?? 20));
    if (search.employeeId != null) params = params.set('employeeId', String(search.employeeId));
    if (search.periodId != null) params = params.set('periodId', String(search.periodId));
    return this.http.get<PagedResponse<PayrollRecordResponse>>(`${this.base}/records`, { params });
  }

  registerRecord(body: RegisterPayrollRecordRequest): Observable<PayrollRecordResponse> {
    return this.http.post<PayrollRecordResponse>(`${this.base}/records`, body);
  }

  registerAdjustment(
    recordId: number,
    body: RegisterPayrollAdjustmentRequest,
  ): Observable<PayrollPaymentResponse> {
    return this.http.post<PayrollPaymentResponse>(
      `${this.base}/records/${recordId}/adjustments`,
      body,
    );
  }

  listPeriods(page = 0, size = 20): Observable<PagedResponse<PayrollPeriodResponse>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<PagedResponse<PayrollPeriodResponse>>(`${this.base}/periods`, { params });
  }

  registerPeriod(body: RegisterPayrollPeriodRequest): Observable<PayrollPeriodResponse> {
    return this.http.post<PayrollPeriodResponse>(`${this.base}/periods`, body);
  }

  listPayments(
    page = 0,
    size = 20,
    employeeId?: number,
  ): Observable<PagedResponse<PayrollPaymentResponse>> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (employeeId != null) params = params.set('employeeId', String(employeeId));
    return this.http.get<PagedResponse<PayrollPaymentResponse>>(`${this.base}/payments`, { params });
  }

  registerPayment(body: RegisterPayrollPaymentRequest): Observable<PayrollPaymentResponse> {
    return this.http.post<PayrollPaymentResponse>(`${this.base}/payments`, body);
  }

  listDebts(page = 0, size = 20): Observable<PagedResponse<PayrollDebtResponse>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<PagedResponse<PayrollDebtResponse>>(`${this.base}/debts`, { params });
  }

  /** Descarga Excel (nombre sugerido: `payroll_reporte.xlsx`). */
  exportRecords(scope: PayrollBulkScopeParams = {}): Observable<Blob> {
    let params = new HttpParams()
      .set('page', String(scope.page ?? 0))
      .set('size', String(scope.size ?? 100));
    if (scope.employeeId != null) params = params.set('employeeId', String(scope.employeeId));
    if (scope.periodId != null) params = params.set('periodId', String(scope.periodId));
    if (scope.from) params = params.set('from', scope.from);
    if (scope.to) params = params.set('to', scope.to);
    return this.http.get(`${this.base}/export`, {
      params,
      responseType: 'blob',
    });
  }

  importRecords(file: File, scope: PayrollBulkScopeParams = {}): Observable<SpreadsheetBulkImportResult> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    let params = new HttpParams()
      .set('page', String(scope.page ?? 0))
      .set('size', String(scope.size ?? 100));
    if (scope.employeeId != null) params = params.set('employeeId', String(scope.employeeId));
    if (scope.periodId != null) params = params.set('periodId', String(scope.periodId));
    if (scope.from) params = params.set('from', scope.from);
    if (scope.to) params = params.set('to', scope.to);
    return this.http.post<SpreadsheetBulkImportResult>(`${this.base}/import`, formData, { params });
  }
}
