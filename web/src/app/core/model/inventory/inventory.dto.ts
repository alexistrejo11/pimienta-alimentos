import type {
  InventoryStatus,
  ItemCategory,
  ItemKind,
  ItemStatus,
  ItemUnit,
  LocationType,
  InventoryTransactionType,
  InventoryTransactionStatus,
  InventoryMovementType,
  MovementDirection,
  InventoryExitReason,
} from './inventory.enums';

/** GET /api/v1/inventory/items */
export interface ItemResponse {
  id: number;
  name: string;
  description: string;
  category: ItemCategory;
  unit: ItemUnit;
  brand: string;
  costPrice: number;
  reorderPoint: number;
  reorderQuantity: number;
  status: ItemStatus;
  kind: ItemKind;
  productId: number | null;
  saleSku: string | null;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
  version: number;
}

/** POST /api/v1/inventory/items — always creates a warehouse item. */
export interface ItemCreateRequest {
  name: string;
  description?: string;
  category: ItemCategory;
  unit: ItemUnit;
  brand?: string;
  costPrice?: number;
  reorderPoint?: number;
  reorderQuantity?: number;
}

/** PUT /api/v1/inventory/items/:id — identity of a warehouse item only. */
export interface ItemUpdateRequest {
  name: string;
  description?: string;
  category: ItemCategory;
  unit: ItemUnit;
  brand?: string;
}

/** PUT /api/v1/inventory/items/:id/stock-settings — any item. */
export interface ItemStockSettingsRequest {
  costPrice: number;
  reorderPoint: number;
  reorderQuantity: number;
  status: ItemStatus;
}

export interface ItemSearchParams {
  page?: number;
  size?: number;
  name?: string;
  category?: ItemCategory;
  status?: ItemStatus;
  kind?: ItemKind;
  search?: string;
  minCost?: number;
  maxCost?: number;
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

/** GET /api/v1/inventory/stock/summary */
export interface GlobalInventoryResponse {
  itemId: number;
  sku: string;
  name: string;
  category: ItemCategory;
  headquarterId: number | null;
  headquarterName: string;
  availableQuantity: number;
  totalQuantity: number;
  unitCost: number;
  totalValue: number;
  status: InventoryStatus;
}

export interface GlobalInventorySearchParams {
  page?: number;
  size?: number;
  search?: string;
  category?: ItemCategory;
  status?: InventoryStatus;
  headquarterId?: number;
  minCost?: number;
  maxCost?: number;
}

/** GET /api/v1/inventory/dashboard */
export interface InventoryDashboardResponse {
  skuCount: number;
  lowStockCount: number;
  outOfStockCount: number;
  totalAvailableQuantity: number;
  totalStockValue: number;
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

export interface InLineRequest {
  itemId: number;
  locationId: number;
  quantity: number;
  unitCost: number;
}

export interface InTransactionRequest {
  externalReference?: string;
  notes?: string;
  initiatedById?: number;
  lines: InLineRequest[];
}

export interface OutLineRequest {
  itemId: number;
  locationId: number;
  quantity: number;
  unitCost: number;
}

export interface OutTransactionRequest {
  externalReference?: string;
  notes?: string;
  exitReason: InventoryExitReason;
  initiatedById?: number;
  lines: OutLineRequest[];
}

export interface AdjustmentLineRequest {
  itemId: number;
  locationId: number;
  countedQuantity: number;
  reason: string;
}

export interface AdjustmentTransactionRequest {
  externalReference?: string;
  notes?: string;
  initiatedById?: number;
  lines: AdjustmentLineRequest[];
}

export interface InventoryTransactionResponse {
  id: number;
  transactionNumber: string;
  type: InventoryTransactionType;
  status: InventoryTransactionStatus;
  externalReference: string | null;
  notes: string | null;
  exitReason: InventoryExitReason | null;
  initiatedById: number | null;
  approvedById: number | null;
  approvedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
  version: number;
  movements: InventoryMovementResponse[];
}

export interface InventoryMovementResponse {
  id: number;
  itemId: number;
  itemSku: string;
  sourceLocationId: number | null;
  sourceLocationCode: string | null;
  destinationLocationId: number | null;
  destinationLocationCode: string | null;
  quantity: number;
  unitCost: number;
  type: InventoryMovementType;
  direction: MovementDirection;
  exitReason: InventoryExitReason | null;
  description: string | null;
  referenceNumber: string | null;
  performedById: number | null;
  stockAfterMovement: number;
  transactionId: number | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface InventoryTransactionSearchParams {
  page?: number;
  size?: number;
  type?: InventoryTransactionType;
  status?: InventoryTransactionStatus;
  fromDate?: string;
  toDate?: string;
}

export interface InventoryMovementSearchParams {
  page?: number;
  size?: number;
  type?: InventoryMovementType;
  direction?: MovementDirection;
  exitReason?: InventoryExitReason;
  itemId?: number;
  locationId?: number;
  search?: string;
  category?: ItemCategory;
  fromDate?: string;
  toDate?: string;
}
