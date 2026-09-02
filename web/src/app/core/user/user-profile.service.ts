import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type {
  UpdateProfileRequest,
  UserDashboardResponse,
  UserResponse,
} from '../model/account/user.dto';

/** Llamadas a {@code /api/v1/users/me} y {@code /api/v1/users/me/dashboard} (requieren JWT). */
@Injectable({
  providedIn: 'root',
})
export class UserProfileService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/users/me`;

  getProfile(): Observable<UserResponse> {
    return this.http.get<UserResponse>(this.base);
  }

  updateProfile(body: UpdateProfileRequest): Observable<UserResponse> {
    return this.http.patch<UserResponse>(this.base, body);
  }

  getDashboard(): Observable<UserDashboardResponse> {
    return this.http.get<UserDashboardResponse>(`${this.base}/dashboard`);
  }
}
