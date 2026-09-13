import type {
  InventoryStatus,
  ItemCategory,
  ItemStatus,
  ItemUnit,
  LocationType,
} from './inventory.enums';

/** GET /api/v1/inventory/items */
export interface ItemResponse {
  id: number;
  sku: string;
  name: string;
  description: string;
  category: ItemCategory;
  unit: ItemUnit;
  brand: string;
  barcode: string | null;
  costPrice: number;
  salePrice: number;
  reorderPoint: number;
  reorderQuantity: number;
  status: ItemStatus;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
  version: number;
  catalogRole: 'INVENTORY_ONLY' | 'POS_SELLABLE';
}

/** POST /api/v1/inventory/items */
export interface ItemCreateRequest {
  sku?: string;
  name: string;
  description?: string;
  costPrice: number;
  salePrice: number;
  category: ItemCategory;
  unit: ItemUnit;
  reorderPoint: number;
  reorderQuantity: number;
  brand?: string;
  barcode?: string;
  catalogRole?: 'INVENTORY_ONLY' | 'POS_SELLABLE';
}

/** PUT /api/v1/inventory/items/:id */
export interface ItemUpdateRequest {
  sku: string;
  name: string;
  description?: string;
  costPrice: number;
  salePrice: number;
  category: ItemCategory;
  unit: ItemUnit;
  reorderPoint: number;
  reorderQuantity: number;
  brand?: string;
  barcode?: string;
  status: ItemStatus;
  catalogRole?: 'INVENTORY_ONLY' | 'POS_SELLABLE';
}

export interface ItemSearchParams {
  page?: number;
  size?: number;
  name?: string;
  sku?: string;
  category?: ItemCategory;
  status?: ItemStatus;
}

/** GET /api/v1/inventory/stock */
export interface InventoryStockResponse {
  id: number;
  itemId: number;
  itemSku: string;
  itemName: string;
  locationId: number;
  locationCode: string;
  locationName: string;
  availableQuantity: number;
  reservedQuantity: number;
  inTransitQuantity: number;
  status: InventoryStatus;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
  version: number;
}

/** POST /api/v1/inventory/stock */
export interface CreateInitialStockRequest {
  itemId: number;
  locationId: number;
  initialQuantity: number;
}

export interface InventoryStockSearchParams {
  page?: number;
  size?: number;
  itemId?: number;
  locationId?: number;
  status?: InventoryStatus;
}

/** GET /api/v1/inventory/locations */
export interface StorageLocationResponse {
  id: number;
  code: string;
  name: string;
  description: string;
  type: LocationType;
  parentId: number | null;
  headquarterId: number | null;
  maxCapacity: number;
  occupiedCapacity: number;
  status: string;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
  version: number;
}

export interface StorageLocationSearchParams {
  page?: number;
  size?: number;
  type?: LocationType;
}
