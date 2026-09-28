/** Mirrors backend SupplierResponse / UpsertSupplierRequest. */
export interface SupplierHeadquarterAssignment {
  headquarterId: number;
  active: boolean;
}

export interface SupplierResponse {
  id: number;
  name: string;
  phone: string;
  brand: string;
  headquarters: SupplierHeadquarterAssignment[];
}

export interface UpsertSupplierRequest {
  name: string;
  phone: string;
  brand: string;
  headquarters: SupplierHeadquarterAssignment[];
}

export interface SupplierSearchParams {
  page?: number;
  size?: number;
  headquarterId?: number;
  search?: string;
}
