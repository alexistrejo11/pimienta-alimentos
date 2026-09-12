/** GET /api/v1/clients — query params */
export interface ClientSearchParams {
  nameContains?: string;
  page?: number;
  size?: number;
}

/** POST /api/v1/clients */
export interface CreateClientRequest {
  name: string;
  companyName?: string | null;
}

export interface ClientResponse {
  id: number;
  name: string;
  companyName: string;
  createdAt: string;
  updatedAt: string;
}
