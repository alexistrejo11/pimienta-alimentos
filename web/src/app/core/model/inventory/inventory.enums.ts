/** Mirrors {@link io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory}. */
export type ItemCategory =
  | 'RAW_MATERIAL'
  | 'FINISHED_GOOD'
  | 'CONSUMABLE'
  | 'SPARE_PART'
  | 'PACKAGING'
  | 'TOOL'
  | 'MACHINE'
  | 'FURNITURE'
  | 'OTHER';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemUnit}. */
export type ItemUnit =
  | 'PIECE'
  | 'KG'
  | 'GRAM'
  | 'LITER'
  | 'ML'
  | 'BOX'
  | 'DOZEN'
  | 'METER'
  | 'SQUARE_METER';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemStatus}. */
export type ItemStatus = 'ACTIVE' | 'DISCONTINUED' | 'OUT_OF_STOCK' | 'PENDING_APPROVAL';
export type CatalogRole = 'INVENTORY_ONLY' | 'POS_SELLABLE';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory.InventoryStatus}. */
export type InventoryStatus = 'NORMAL' | 'LOW_STOCK' | 'OUT_OF_STOCK';

export type InventoryTransactionType = 'PURCHASE_RECEIPT' | 'SALE_DISPATCH' | 'INTERNAL_TRANSFER' | 'PHYSICAL_COUNT' | 'RETURN_FROM_CLIENT' | 'RETURN_TO_SUPPLIER' | 'PRODUCTION_ISSUE' | 'SCRAP_WRITE_OFF';
export type InventoryTransactionStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type InventoryMovementType = 'PURCHASE' | 'RETURN_FROM_CLIENT' | 'INITIAL_STOCK' | 'SALE' | 'RETURN_TO_SUPPLIER' | 'USAGE' | 'SCRAP' | 'TRANSFER' | 'ADJUSTMENT_PLUS' | 'ADJUSTMENT_MINUS';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.inventory.core.domain.enums.InventoryExitReason}. */
export type InventoryExitReason =
  | 'SCRAP'
  | 'DAMAGED'
  | 'EXPIRED'
  | 'INTERNAL_USE'
  | 'INVENTORY_ADJUSTMENT';
export type MovementDirection = 'IN' | 'OUT' | 'NEUTRAL';

export type InventoryCountType = 'FULL' | 'PARTIAL';
export type InventoryCountStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'CANCELLED';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation.LocationType}. */
export type LocationType =
  | 'WAREHOUSE'
  | 'ZONE'
  | 'AISLE'
  | 'SHELF'
  | 'BIN'
  | 'POS';
