/** Mirrors backend SupplierResponse / UpsertSupplierRequest. */
export interface SupplierResponse {
  id: number;
  name: string;
  contactName: string;
  phone: string;
  brand: string;
  headquarterIds: number[];
}

export interface UpsertSupplierRequest {
  name: string;
  contactName: string;
  phone: string;
  brand: string;
  headquarterIds: number[];
}

export interface SupplierSearchParams {
  page?: number;
  size?: number;
  headquarterId?: number;
  search?: string;
}
