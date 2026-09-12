import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';

/** Response from GET /api/v1/pos/releases/android/latest */
export interface PosApkReleaseResponse {
  versionName: string;
  versionCode: number;
  url: string;
  expiresInSeconds: number;
}

/** Public POS APK release metadata (no auth). */
@Injectable({ providedIn: 'root' })
export class PosReleaseService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/pos/releases`;

  getLatestAndroid(): Observable<PosApkReleaseResponse> {
    return this.http.get<PosApkReleaseResponse>(`${this.base}/android/latest`);
  }
}
