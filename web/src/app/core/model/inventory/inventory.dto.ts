import type {
  InventoryStatus,
  ItemCategory,
  ItemKind,
  ItemStatus,
  ItemUnit,
  LocationType,
  InventoryCountStatus,
  InventoryCountType,
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

/** GET /api/v1/inventory/stock/summary */
export interface GlobalInventoryResponse {
  itemId: number;
  sku: string;
  name: string;
  category: ItemCategory;
  headquarterId: number | null;
  headquarterName: string;
  availableQuantity: number;
  reservedQuantity: number;
  inTransitQuantity: number;
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
  openCountSessionCount: number;
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
  exitReason: InventoryExitReason;
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

export interface InventoryMovementSearchParams extends InventoryTransactionSearchParams {
  direction?: MovementDirection;
  itemId?: number;
  locationId?: number;
  search?: string;
  category?: ItemCategory;
}

export interface TransferLineRequest {
  itemId: number;
  fromLocationId: number;
  toLocationId: number;
  quantity: number;
  unitCost: number;
}

export interface TransferTransactionRequest {
  externalReference?: string;
  notes?: string;
  lines: TransferLineRequest[];
}
