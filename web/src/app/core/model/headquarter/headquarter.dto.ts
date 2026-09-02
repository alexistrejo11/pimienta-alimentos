/** POST/PUT /api/v1/headquarters, PUT /api/v1/headquarters/:id */
export interface HeadQuarterRequest {
  name: string;
  address: string;
  description: string;
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
