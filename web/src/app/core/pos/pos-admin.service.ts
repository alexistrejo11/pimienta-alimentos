import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type { PagedResponse } from '../model/common/pagination';
import type {
  CreateEnrollmentCodeRequest,
  CreatePosOperatorRequest,
  EnrollmentCodeResponse,
  PosAdminListParams,
  PosDeviceAdminResponse,
  PosLedgerEventReportResponse,
  PosOperatorResponse,
  PosProductReportResponse,
  PosReportFilterParams,
  PosReportSummaryResponse,
  PosSaleReportResponse,
  PosShiftListParams,
  PosShiftListItemResponse,
  PosShiftDetailResponse,
  PosShiftReconciliationResponse,
  PosShiftResponse,
  UpdatePosOperatorRequest,
} from '../model/pos/pos.dto';

/** Admin POS: /api/v1/pos/admin */
@Injectable({ providedIn: 'root' })
export class PosAdminService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/pos/admin`;

  // ── Dispositivos ──────────────────────────────────────────────────────────

  listDevices(params: PosAdminListParams = {}): Observable<PagedResponse<PosDeviceAdminResponse>> {
    let p = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20));
    if (params.headquarterId != null) p = p.set('headquarterId', String(params.headquarterId));
    return this.http.get<PagedResponse<PosDeviceAdminResponse>>(`${this.base}/devices`, { params: p });
  }

  revokeDevice(id: string): Observable<PosDeviceAdminResponse> {
    return this.http.post<PosDeviceAdminResponse>(`${this.base}/devices/${id}/revoke`, {});
  }

  // ── Enrolamiento ──────────────────────────────────────────────────────────

  createEnrollmentCode(body: CreateEnrollmentCodeRequest): Observable<EnrollmentCodeResponse> {
    return this.http.post<EnrollmentCodeResponse>(`${this.base}/enrollment-codes`, body);
  }

  // ── Operadores ────────────────────────────────────────────────────────────

  listOperators(params: PosAdminListParams = {}): Observable<PagedResponse<PosOperatorResponse>> {
    let p = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20));
    if (params.headquarterId != null) {
      p = p.set('headquarterId', String(params.headquarterId));
    }
    return this.http.get<PagedResponse<PosOperatorResponse>>(`${this.base}/operators`, { params: p });
  }

  getOperator(id: number): Observable<PosOperatorResponse> {
    return this.http.get<PosOperatorResponse>(`${this.base}/operators/${id}`);
  }

  createOperator(body: CreatePosOperatorRequest): Observable<PosOperatorResponse> {
    return this.http.post<PosOperatorResponse>(`${this.base}/operators`, body);
  }

  updateOperator(id: number, body: UpdatePosOperatorRequest): Observable<PosOperatorResponse> {
    return this.http.put<PosOperatorResponse>(`${this.base}/operators/${id}`, body);
  }

  deleteOperator(id: number): Observable<PosOperatorResponse> {
    return this.http.delete<PosOperatorResponse>(`${this.base}/operators/${id}`);
  }

  assignOperatorHeadquarter(id: number, headquarterId: number): Observable<PosOperatorResponse> {
    return this.http.post<PosOperatorResponse>(`${this.base}/operators/${id}/headquarters`, { headquarterId });
  }

  unassignOperatorHeadquarter(id: number, headquarterId: number): Observable<PosOperatorResponse> {
    return this.http.delete<PosOperatorResponse>(`${this.base}/operators/${id}/headquarters/${headquarterId}`);
  }

  // ── Reportes ──────────────────────────────────────────────────────────────

  reportSummary(headquarterId?: number, from?: string, to?: string): Observable<PosReportSummaryResponse> {
    let p = new HttpParams();
    if (headquarterId != null) p = p.set('headquarterId', String(headquarterId));
    if (from) p = p.set('from', from);
    if (to) p = p.set('to', to);
    return this.http.get<PosReportSummaryResponse>(`${this.base}/reports/summary`, { params: p });
  }

  reportSales(params: PosReportFilterParams): Observable<PagedResponse<PosSaleReportResponse>> {
    return this.http.get<PagedResponse<PosSaleReportResponse>>(`${this.base}/reports/sales`, {
      params: this.reportParams(params),
    });
  }

  reportProducts(params: PosReportFilterParams): Observable<PagedResponse<PosProductReportResponse>> {
    return this.http.get<PagedResponse<PosProductReportResponse>>(`${this.base}/reports/products`, {
      params: this.reportParams(params),
    });
  }

  reportShiftCloses(params: PosReportFilterParams): Observable<PagedResponse<PosLedgerEventReportResponse>> {
    return this.http.get<PagedResponse<PosLedgerEventReportResponse>>(`${this.base}/reports/shift-closes`, {
      params: this.reportParams(params),
    });
  }

  listShifts(params: PosShiftListParams = {}): Observable<PagedResponse<PosShiftListItemResponse>> {
    let p = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20));
    if (params.headquarterId != null) p = p.set('headquarterId', String(params.headquarterId));
    if (params.from) p = p.set('from', params.from);
    if (params.to) p = p.set('to', params.to);
    if (params.status) p = p.set('status', params.status);
    if (params.cashierOperatorId != null) {
      p = p.set('cashierOperatorId', String(params.cashierOperatorId));
    }
    return this.http.get<PagedResponse<PosShiftListItemResponse>>(`${this.base}/shifts`, { params: p });
  }

  getShift(shiftId: string, headquarterId: number): Observable<PosShiftDetailResponse> {
    const p = new HttpParams().set('headquarterId', String(headquarterId));
    return this.http.get<PosShiftDetailResponse>(`${this.base}/shifts/${shiftId}`, { params: p });
  }

  getShiftReconciliation(
    shiftId: string,
    headquarterId: number,
  ): Observable<PosShiftReconciliationResponse> {
    const p = new HttpParams().set('headquarterId', String(headquarterId));
    return this.http.get<PosShiftReconciliationResponse>(
      `${this.base}/shifts/${shiftId}/reconciliation`,
      { params: p },
    );
  }

  private reportParams(params: PosReportFilterParams): HttpParams {
    let p = new HttpParams()
      .set('headquarterId', String(params.headquarterId))
      .set('from', params.from)
      .set('to', params.to)
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20));
    if (params.shiftId) p = p.set('shiftId', params.shiftId);
    if (params.productId != null) p = p.set('productId', String(params.productId));
    if (params.eventType) p = p.set('eventType', params.eventType);
    if (params.lineType) p = p.set('lineType', params.lineType);
    if (params.openProductsOnly) p = p.set('openProductsOnly', 'true');
    return p;
  }
}
