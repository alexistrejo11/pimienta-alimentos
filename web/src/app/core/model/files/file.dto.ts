import type { FileCategory } from './file.enums';

/** Stored company file asset (GET list/detail, POST upload response). */
export interface FileAssetResponse {
  id: string;
  category: FileCategory;
  module: string | null;
  entityType: string | null;
  entityId: number | null;
  s3Key: string;
  originalName: string;
  contentType: string;
  fileSizeBytes: number;
  description: string | null;
  uploadedByUserId: number | null;
  createdAt: string;
}

/** Pre-signed 24-hour download URL. */
export interface FileDownloadUrlResponse {
  id: string;
  url: string;
}

/** Query params for GET /api/v1/files/management (admin). */
export interface FileManagementSearchParams {
  page?: number;
  size?: number;
  category?: FileCategory;
  module?: string;
  entityType?: string;
  entityId?: number;
  originalNameContains?: string;
  contentTypeContains?: string;
  uploadedByUserId?: number;
  createdFrom?: string;
  createdTo?: string;
}

/** Query params for GET /api/v1/files/resources (manager/admin). */
export interface FileResourceSearchParams {
  page?: number;
  size?: number;
  module: string;
  entityType?: string;
  entityId?: number;
  originalNameContains?: string;
  contentTypeContains?: string;
  createdFrom?: string;
  createdTo?: string;
}

/** Query params for POST upload endpoints. */
export interface FileUploadParams {
  category?: FileCategory;
  module?: string;
  entityType?: string;
  entityId?: number;
  description?: string;
  uploadedByUserId?: number;
}
