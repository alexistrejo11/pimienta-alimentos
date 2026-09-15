import type {
  InventoryStatus,
  ItemCategory,
  ItemStatus,
  ItemUnit,
  LocationType,
  InventoryCountStatus,
  InventoryCountType,
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
  headquarterId?: number;
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
  headquarterId?: number;
}

export interface InventoryCountResponseDto {
  itemId: number;
  expectedQuantity: number | null;
  countedQuantity: number | null;
  variance: number | null;
}

export interface InventoryCountSessionResponse {
  id: number;
  locationId: number;
  type: InventoryCountType;
  status: InventoryCountStatus;
  createdById: number;
  submittedById: number | null;
  approvedById: number | null;
  createdAt: string;
  submittedAt: string | null;
  approvedAt: string | null;
  cancelledAt: string | null;
  responses: InventoryCountResponseDto[];
}

export interface OpenInventoryCountRequest {
  locationId: number;
  type: InventoryCountType;
  itemIds?: number[];
}

export interface InventoryCountResponseRequest {
  itemId: number;
  countedQuantity: number;
}

export interface InventoryCountSessionSummaryResponse {
  id: number;
  locationId: number;
  type: InventoryCountType;
  status: InventoryCountStatus;
  createdById: number;
  submittedById: number | null;
  approvedById: number | null;
  createdAt: string;
  submittedAt: string | null;
  approvedAt: string | null;
  cancelledAt: string | null;
}

export interface InventoryCountSearchParams {
  page?: number;
  size?: number;
  locationId?: number;
  status?: InventoryCountStatus;
  headquarterId?: number;
}

export interface PurchaseLineRequest {
  itemId: number;
  locationId: number;
  quantity: number;
  unitCost: number;
}

export interface PurchaseTransactionRequest {
  externalReference?: string;
  notes?: string;
  lines: PurchaseLineRequest[];
}

export interface ScrapLineRequest {
  itemId: number;
  locationId: number;
  quantity: number;
  unitCost: number;
}

export interface ScrapTransactionRequest {
  externalReference?: string;
  notes?: string;
  lines: ScrapLineRequest[];
}

export interface AdjustmentLineRequest {
  itemId: number;
  locationId: number;
  newQuantity: number;
  reason: string;
}

export interface AdjustmentTransactionRequest {
  externalReference?: string;
  notes?: string;
  lines: AdjustmentLineRequest[];
}

export interface InventoryTransactionResponse {
  id: number;
  transactionNumber: string;
  type: string;
  status: string;
}
