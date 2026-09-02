import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { API_BASE_URL } from '../config/api.config';
import type {
  FileAssetResponse,
  FileDownloadUrlResponse,
  FileManagementSearchParams,
  FileResourceSearchParams,
  FileUploadParams,
} from '../model/files/file.dto';
import type { PagedResponse } from '../model/common/pagination';

/** Llamadas al módulo de archivos: /api/v1/files/management y /api/v1/files/resources */
@Injectable({ providedIn: 'root' })
export class FileService {
  private readonly http = inject(HttpClient);
  private readonly managementBase = `${API_BASE_URL}/files/management`;
  private readonly resourcesBase = `${API_BASE_URL}/files/resources`;

  // ── Admin — management ─────────────────────────────────────────────────────

  searchManagement(
    params: FileManagementSearchParams = {},
  ): Observable<PagedResponse<FileAssetResponse>> {
    return this.http.get<PagedResponse<FileAssetResponse>>(this.managementBase, {
      params: this.buildManagementParams(params),
    });
  }

  uploadManagement(file: File, params: FileUploadParams): Observable<FileAssetResponse> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<FileAssetResponse>(`${this.managementBase}/upload`, formData, {
      params: this.buildUploadParams(params),
    });
  }

  getById(id: string): Observable<FileAssetResponse> {
    return this.http.get<FileAssetResponse>(`${this.managementBase}/${id}`);
  }

  delete(id: string): Observable<void> {
    return this.http.delete(`${this.managementBase}/${id}`, { observe: 'response' }).pipe(
      map(() => undefined),
    );
  }

  getManagementDownloadUrl(id: string): Observable<FileDownloadUrlResponse> {
    return this.http.get<FileDownloadUrlResponse>(`${this.managementBase}/${id}/download-url`);
  }

  // ── Manager/Admin — resources ──────────────────────────────────────────────

  searchResources(
    params: FileResourceSearchParams,
  ): Observable<PagedResponse<FileAssetResponse>> {
    return this.http.get<PagedResponse<FileAssetResponse>>(this.resourcesBase, {
      params: this.buildResourceParams(params),
    });
  }

  uploadResource(file: File, params: FileUploadParams): Observable<FileAssetResponse> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<FileAssetResponse>(`${this.resourcesBase}/upload`, formData, {
      params: this.buildResourceUploadParams(params),
    });
  }

  getResourceDownloadUrl(id: string): Observable<FileDownloadUrlResponse> {
    return this.http.get<FileDownloadUrlResponse>(`${this.resourcesBase}/${id}/download-url`);
  }

  // ── HttpParams builders ────────────────────────────────────────────────────

  private buildManagementParams(params: FileManagementSearchParams): HttpParams {
    let httpParams = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20));

    if (params.category) httpParams = httpParams.set('category', params.category);
    if (params.module) httpParams = httpParams.set('module', params.module);
    if (params.entityType) httpParams = httpParams.set('entityType', params.entityType);
    if (params.entityId != null) httpParams = httpParams.set('entityId', String(params.entityId));
    if (params.originalNameContains)
      httpParams = httpParams.set('originalNameContains', params.originalNameContains);
    if (params.contentTypeContains)
      httpParams = httpParams.set('contentTypeContains', params.contentTypeContains);
    if (params.uploadedByUserId != null)
      httpParams = httpParams.set('uploadedByUserId', String(params.uploadedByUserId));
    if (params.createdFrom) httpParams = httpParams.set('createdFrom', params.createdFrom);
    if (params.createdTo) httpParams = httpParams.set('createdTo', params.createdTo);

    return httpParams;
  }

  private buildResourceParams(params: FileResourceSearchParams): HttpParams {
    let httpParams = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20))
      .set('module', params.module);

    if (params.entityType) httpParams = httpParams.set('entityType', params.entityType);
    if (params.entityId != null) httpParams = httpParams.set('entityId', String(params.entityId));
    if (params.originalNameContains)
      httpParams = httpParams.set('originalNameContains', params.originalNameContains);
    if (params.contentTypeContains)
      httpParams = httpParams.set('contentTypeContains', params.contentTypeContains);
    if (params.createdFrom) httpParams = httpParams.set('createdFrom', params.createdFrom);
    if (params.createdTo) httpParams = httpParams.set('createdTo', params.createdTo);

    return httpParams;
  }

  private buildUploadParams(params: FileUploadParams): HttpParams {
    let httpParams = new HttpParams();
    if (params.category) httpParams = httpParams.set('category', params.category);
    if (params.module) httpParams = httpParams.set('module', params.module);
    if (params.entityType) httpParams = httpParams.set('entityType', params.entityType);
    if (params.entityId != null) httpParams = httpParams.set('entityId', String(params.entityId));
    if (params.description) httpParams = httpParams.set('description', params.description);
    if (params.uploadedByUserId != null)
      httpParams = httpParams.set('uploadedByUserId', String(params.uploadedByUserId));
    return httpParams;
  }

  private buildResourceUploadParams(params: FileUploadParams): HttpParams {
    let httpParams = new HttpParams();
    if (params.module) httpParams = httpParams.set('module', params.module);
    if (params.entityType) httpParams = httpParams.set('entityType', params.entityType);
    if (params.entityId != null) httpParams = httpParams.set('entityId', String(params.entityId));
    if (params.description) httpParams = httpParams.set('description', params.description);
    if (params.uploadedByUserId != null)
      httpParams = httpParams.set('uploadedByUserId', String(params.uploadedByUserId));
    return httpParams;
  }
}
