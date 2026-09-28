import type { ItemUnit } from '../inventory/inventory.enums';

export type ProductStatus = 'ACTIVE' | 'DISCONTINUED';

export interface ProductResponse {
  id: number;
  sku: string;
  name: string;
  description: string;
  unit: ItemUnit;
  barcode: string | null;
  status: ProductStatus;
  trackStock: boolean;
  inventoryItemId: number | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface ProductCreateRequest {
  name: string;
  description?: string;
  unit: ItemUnit;
  barcode?: string;
  trackStock: boolean;
}

export interface ProductUpdateRequest {
  name: string;
  description?: string;
  unit: ItemUnit;
  barcode?: string;
  status: ProductStatus;
  trackStock: boolean;
}

export interface ProductSearchParams {
  page?: number;
  size?: number;
  search?: string;
  status?: ProductStatus;
}
