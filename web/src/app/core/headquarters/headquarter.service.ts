import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type {
  HeadQuarterResponse,
  HeadquarterStatisticsResponse,
} from '../model/headquarter/headquarter.dto';
import type { SpringDataPage } from '../model/common/pagination';

/** Llamadas al módulo de sedes: /api/v1/headquarters */
@Injectable({ providedIn: 'root' })
export class HeadquarterService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/headquarters`;

  /** Lista paginada de sedes (Spring Data Page). */
  list(page = 0, size = 20): Observable<SpringDataPage<HeadQuarterResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<SpringDataPage<HeadQuarterResponse>>(this.base, { params });
  }

  /** Detalle de una sede por ID. */
  getById(id: number): Observable<HeadQuarterResponse> {
    return this.http.get<HeadQuarterResponse>(`${this.base}/${id}`);
  }

  /** Estadísticas globales: total, activas, dadas de baja. */
  statistics(): Observable<HeadquarterStatisticsResponse> {
    return this.http.get<HeadquarterStatisticsResponse>(`${this.base}/statistics`);
  }
}
