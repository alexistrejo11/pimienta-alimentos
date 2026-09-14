import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type { PagedResponse } from '../model/common/pagination';
import type {
  AddRolesRequest,
  AssignHeadquartersRequest,
  BanUserRequest,
  UserResponse,
  UserStatisticsResponse,
} from '../model/account/user.dto';

@Injectable({ providedIn: 'root' })
export class UserManagementService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/users/management`;

  statistics(): Observable<UserStatisticsResponse> {
    return this.http.get<UserStatisticsResponse>(`${this.base}/statistics`);
  }

  list(page = 0, size = 20): Observable<PagedResponse<UserResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PagedResponse<UserResponse>>(this.base, { params });
  }

  approve(id: number): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/approve`, {});
  }

  ban(id: number, body: BanUserRequest = {}): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/ban`, body);
  }

  unban(id: number): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/unban`, {});
  }

  replaceRoles(id: number, body: AddRolesRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.base}/${id}/roles`, body);
  }

  assignHeadquarters(id: number, body: AssignHeadquartersRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.base}/${id}/headquarters`, body);
  }
}
