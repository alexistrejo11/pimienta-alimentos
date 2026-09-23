import type { PosDeviceStatus, PosRole, StockPolicy } from './pos.enums';
import type { ItemCategory, ItemUnit } from '../inventory/inventory.enums';
import type { ItemResponse } from '../inventory/inventory.dto';

export interface PosSaleCategoryResponse {
  id: number;
  headquarterId: number;
  name: string;
  displayOrder: number;
  active: boolean;
}

export interface CreatePosProductRequest {
  name: string;
  description?: string;
  costPrice: number;
  salePrice: number;
  category?: ItemCategory;
  unit: ItemUnit;
  brand?: string;
  barcode?: string;
  reorderPoint: number;
  reorderQuantity: number;
  posSaleCategoryId: number;
  available?: boolean;
  stockPolicy?: StockPolicy;
  negativeStockLimit?: number | null;
}

export interface CreatedPosProductResponse {
  id: number;
  sku: string;
  name: string;
  barcode: string | null;
  category: ItemCategory;
  unit: ItemUnit;
  costPrice: number;
  salePrice: number;
  headquarterId: number;
  posSaleCategoryId: number;
  saleCategory: string;
  available: boolean;
  stockPolicy: StockPolicy;
}

/** GET/PUT /api/v1/headquarters/{id}/pos-settings */
export interface PosSettingsResponse {
  id: number;
  headquarterId: number;
  currency: string;
  catalogStaleWarnHours: number;
  catalogStaleBlockHours: number;
  openAmountCategories: string[];
  allowOpenProducts: boolean;
  defaultNegativeStockLimit: number | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface PosSettingsRequest {
  currency?: string;
  catalogStaleWarnHours?: number;
  catalogStaleBlockHours?: number;
  openAmountCategories?: string[];
  allowOpenProducts?: boolean;
  defaultNegativeStockLimit?: number | null;
}

/** GET/PUT /api/v1/headquarters/{id}/pos-catalog/{itemId} */
export interface HeadquarterPosCatalogItemResponse {
  id: number;
  headquarterId: number;
  itemId: number;
  itemName: string;
  itemSku: string;
  itemBarcode: string | null;
  saleCategory: string;
  salePrice: number;
  available: boolean;
  stockPolicy: StockPolicy;
  negativeStockLimit: number | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface HeadquarterPosCatalogItemRequest {
  saleCategory: string;
  salePrice: number;
  available?: boolean;
  stockPolicy: StockPolicy;
  negativeStockLimit?: number | null;
}

export type PosCatalogCandidateResponse = ItemResponse;

/** GET /api/v1/pos/admin/devices */
export interface PosDeviceAdminResponse {
  id: string;
  headquarterId: number;
  visibleCode: string;
  deviceName: string;
  appVersion: string;
  status: PosDeviceStatus;
  minAppVersion: string;
}

export interface PosShiftResponse {
  shiftId: string;
  headquarterId: number;
  deviceId: string;
  cashierOperatorId: number | null;
  openingCashCentavos: number;
  openedAt: string;
  closedAt: string | null;
  status: string;
  expectedCashCentavos: number | null;
  countedCashCentavos: number | null;
  differenceCentavos: number | null;
}

export interface PosShiftListItemResponse extends PosShiftResponse {
  cashierDisplayName: string;
  deviceName: string;
  deviceVisibleCode: string;
}

export interface PosShiftCashMovement {
  movementId: string;
  type: string;
  amountCentavos: number;
  folio: string | null;
  reason: string | null;
  occurredAt: string;
}

export interface PosShiftCashCount {
  countId: string;
  totalCentavos: number;
  denominations: string;
  submittedAt: string;
}

export interface PosShiftDetailResponse {
  shift: PosShiftListItemResponse;
  cashMovements: PosShiftCashMovement[];
  cashCounts: PosShiftCashCount[];
  cashReconciledCentavos: number;
}

export interface PosShiftReconciliationResponse {
  shift: PosShiftListItemResponse;
  openingCashCentavos: number;
  cashSalesCentavos: number;
  cardSalesCentavos: number;
  courtesySalesCentavos: number;
  withdrawalsCentavos: number;
  depositsCentavos: number;
  expectedCashCentavos: number | null;
  countedCashCentavos: number | null;
  differenceCentavos: number | null;
  saleTicketCount: number;
  cashMovements: PosShiftCashMovement[];
  cashCounts: PosShiftCashCount[];
}

export interface PosShiftListParams {
  headquarterId?: number;
  from?: string;
  to?: string;
  status?: string;
  cashierOperatorId?: number;
  page?: number;
  size?: number;
}

export interface PosActivityItem {
  id: string;
  type: 'VENTA' | 'MERMA' | 'CANCELACIÓN' | 'CORTE' | 'INCIDENCIA';
  detail: string;
  deviceId: string | null;
  occurredAt: string;
  amountCentavos?: number;
}

/** POST /api/v1/pos/admin/enrollment-codes */
export interface CreateEnrollmentCodeRequest {
  headquarterId: number;
}

export interface EnrollmentCodeResponse {
  id: number;
  code: string;
  headquarterId: number;
  expiresAt: string;
  createdAt: string;
}

/** GET/POST /api/v1/pos/admin/operators */
export interface PosOperatorResponse {
  id: number;
  displayName: string;
  posRole: PosRole;
  userId: number | null;
  active: boolean;
  headquarterIds: number[];
}

export interface CreatePosOperatorRequest {
  displayName: string;
  posRole: PosRole;
  pin: string;
  userId?: number | null;
  headquarterIds: number[];
}

export interface UpdatePosOperatorRequest {
  displayName?: string;
  posRole?: PosRole;
  pin?: string;
  userId?: number | null;
  active?: boolean;
  headquarterIds?: number[];
}

/** GET /api/v1/pos/admin/reports/summary */
export interface PosReportSummaryResponse {
  rows: PosReportSummaryItemResponse[];
}

export interface PosReportSummaryItemResponse {
  headquarterId: number;
  salesCentavos: number;
  ticketCount: number;
  wasteCount: number;
  cancellationCount: number;
  lastShiftClosedAt: string | null;
  openIncidentCount: number;
  deviceCount: number;
  openProductCentavos: number;
  openProductLineCount: number;
  openProductTicketCount: number;
  openProductPendingReviewCount: number;
}

/** GET /api/v1/pos/admin/reports/sales */
export interface PosSaleReportResponse {
  saleId: string;
  eventId: string;
  headquarterId: number;
  deviceId: string;
  shiftId: string;
  cashierOperatorId: number | null;
  folio: string;
  grossCentavos: number;
  discountCentavos: number;
  totalCentavos: number;
  status: string;
  syncStatus: string;
  occurredAt: string;
  containsOpenProduct?: boolean;
  lines?: PosSaleLineReportResponse[];
}

export interface PosSaleLineReportResponse {
  lineId: string;
  productId: number | null;
  lineType: 'CATALOG' | 'OPEN_AMOUNT' | 'PENDING_CATALOG';
  productName: string;
  saleCategory: string | null;
  quantity: number;
  unitPriceCentavos: number;
  subtotalCentavos: number;
}

/** GET /api/v1/pos/admin/reports/products */
export interface PosProductReportResponse {
  productId: number | null;
  productName: string;
  quantitySum: number;
  subtotalCentavosSum: number;
  saleCount: number;
}

/** GET /api/v1/pos/admin/reports/waste-cancellations | shift-closes */
export interface PosLedgerEventReportResponse {
  eventId: string;
  eventType: string;
  headquarterId: number;
  deviceId: string;
  shiftId: string;
  occurredAt: string;
  payload: Record<string, unknown>;
}

export interface PosReportFilterParams {
  headquarterId: number;
  from: string;
  to: string;
  shiftId?: string;
  productId?: number;
  lineType?: 'CATALOG' | 'OPEN_AMOUNT' | 'PENDING_CATALOG';
  openProductsOnly?: boolean;
  eventType?: string;
  page?: number;
  size?: number;
}

export interface PosAdminListParams {
  page?: number;
  size?: number;
  headquarterId?: number;
  openOnly?: boolean;
}
