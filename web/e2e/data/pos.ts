import type {
  EnrollmentCodeResponse,
  PosDeviceAdminResponse,
  PosOperatorResponse,
  PosReportSummaryItemResponse,
  PosSaleReportResponse,
  PosShiftListItemResponse,
  PosShiftReconciliationResponse,
} from '../../src/app/core/model/pos/pos.dto';

export function posSale(overrides: Partial<PosSaleReportResponse> = {}): PosSaleReportResponse {
  return {
    saleId: 'sale-1',
    eventId: 'evt-1',
    headquarterId: 10,
    deviceId: 'dev-1',
    shiftId: 'shift-1',
    cashierOperatorId: 1,
    folio: 'F-100',
    grossCentavos: 15000,
    discountCentavos: 0,
    totalCentavos: 15000,
    status: 'COMPLETED',
    syncStatus: 'SYNCED',
    occurredAt: '2026-11-17T12:00:00Z',
    lines: [],
    ...overrides,
  };
}

export function posShift(overrides: Partial<PosShiftListItemResponse> = {}): PosShiftListItemResponse {
  return {
    shiftId: 'shift-1',
    headquarterId: 10,
    deviceId: 'dev-1',
    cashierOperatorId: 1,
    openingCashCentavos: 50000,
    openedAt: '2026-11-17T08:00:00Z',
    closedAt: null,
    status: 'OPEN',
    expectedCashCentavos: null,
    countedCashCentavos: null,
    differenceCentavos: null,
    cashierDisplayName: 'Caja Ana',
    deviceName: 'Caja 1',
    deviceVisibleCode: 'CAJA-1',
    ...overrides,
  };
}

export function posReconciliation(
  overrides: Partial<PosShiftReconciliationResponse> = {},
): PosShiftReconciliationResponse {
  const shift = posShift({ status: 'CLOSED', closedAt: '2026-11-17T16:00:00Z' });
  return {
    shift,
    openingCashCentavos: 50000,
    cashSalesCentavos: 120000,
    cardSalesCentavos: 30000,
    courtesySalesCentavos: 0,
    withdrawalsCentavos: 0,
    depositsCentavos: 0,
    expectedCashCentavos: 170000,
    countedCashCentavos: 170000,
    differenceCentavos: 0,
    saleTicketCount: 12,
    cashMovements: [],
    cashCounts: [],
    ...overrides,
  };
}

export function posOperator(overrides: Partial<PosOperatorResponse> = {}): PosOperatorResponse {
  return {
    id: 1,
    displayName: 'Caja Ana',
    posRole: 'CASHIER',
    userId: null,
    active: true,
    headquarterIds: [10],
    ...overrides,
  };
}

export function posDevice(overrides: Partial<PosDeviceAdminResponse> = {}): PosDeviceAdminResponse {
  return {
    id: 'dev-1',
    headquarterId: 10,
    visibleCode: 'CAJA-1',
    deviceName: 'Caja 1',
    appVersion: '1.0.0',
    status: 'ACTIVE',
    function: 'CASH_REGISTER',
    minAppVersion: '1.0.0',
    ...overrides,
  };
}

export function posEnrollment(overrides: Partial<EnrollmentCodeResponse> = {}): EnrollmentCodeResponse {
  return {
    id: 99,
    code: 'ENROLL-99',
    headquarterId: 10,
    function: 'CASH_REGISTER',
    expiresAt: '2026-11-17T18:00:00Z',
    createdAt: '2026-11-17T12:00:00Z',
    ...overrides,
  };
}

export function posSummaryRow(
  overrides: Partial<PosReportSummaryItemResponse> = {},
): PosReportSummaryItemResponse {
  return {
    headquarterId: 10,
    salesCentavos: 250000,
    cashSalesCentavos: 180000,
    cardSalesCentavos: 70000,
    ticketCount: 18,
    wasteCount: 1,
    cancellationCount: 0,
    lastShiftClosedAt: '2026-11-16T22:00:00Z',
    openIncidentCount: 0,
    deviceCount: 2,
    openProductCentavos: 0,
    openProductLineCount: 0,
    openProductTicketCount: 0,
    openProductPendingReviewCount: 0,
    ...overrides,
  };
}
