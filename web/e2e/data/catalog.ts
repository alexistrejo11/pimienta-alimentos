import type { ItemResponse } from '../../src/app/core/model/inventory/inventory.dto';
import type { ProductResponse } from '../../src/app/core/model/product/product.dto';
import type {
  HeadquarterPosCatalogItemResponse,
  PosSaleCategoryResponse,
} from '../../src/app/core/model/pos/pos.dto';

const stamp = '2026-03-01T00:00:00Z';

export function product(overrides: Partial<ProductResponse> = {}): ProductResponse {
  return {
    id: 50,
    sku: 'SKU-50',
    name: 'Salsa verde',
    description: '',
    unit: 'PIECE',
    barcode: '750123',
    status: 'ACTIVE',
    trackStock: true,
    inventoryItemId: 80,
    createdAt: stamp,
    updatedAt: stamp,
    version: 0,
    ...overrides,
  };
}

export function warehouseItem(overrides: Partial<ItemResponse> = {}): ItemResponse {
  return {
    id: 80,
    name: 'Aceite 20L',
    description: '',
    category: 'RAW_MATERIAL',
    unit: 'PIECE',
    brand: 'Capullo',
    costPrice: 120,
    reorderPoint: 2,
    reorderQuantity: 4,
    status: 'ACTIVE',
    kind: 'WAREHOUSE',
    productId: null,
    saleSku: null,
    createdAt: stamp,
    updatedAt: stamp,
    deletedAt: null,
    version: 0,
    ...overrides,
  };
}

export function posCategory(overrides: Partial<PosSaleCategoryResponse> = {}): PosSaleCategoryResponse {
  return { id: 1, headquarterId: 10, name: 'Comidas', active: true, ...overrides };
}

export function posCatalogItem(
  overrides: Partial<HeadquarterPosCatalogItemResponse> = {},
): HeadquarterPosCatalogItemResponse {
  return {
    id: 9,
    headquarterId: 10,
    productId: 50,
    productName: 'Salsa verde',
    productSku: 'SKU-50',
    productBarcode: '750123',
    saleCategory: 'Comidas',
    salePrice: 45,
    available: true,
    stockPolicy: 'CONTROLLED',
    createdAt: stamp,
    updatedAt: stamp,
    version: 0,
    ...overrides,
  };
}
