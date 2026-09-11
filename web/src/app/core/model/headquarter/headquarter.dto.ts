/** POST/PUT /api/v1/headquarters — solo `name` es obligatorio. */
export interface HeadQuarterRequest {
  name: string;
  address?: string | null;
  description?: string | null;
}

/** GET /api/v1/headquarters/:id, POST, PUT */
export interface HeadQuarterResponse {
  id: number;
  name: string;
  address: string;
  description: string;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
  version: number;
}

/** GET /api/v1/headquarters/statistics */
export interface HeadquarterStatisticsResponse {
  total: number;
  active: number;
  softDeleted: number;
}
