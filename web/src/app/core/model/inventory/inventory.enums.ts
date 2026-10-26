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

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemKind}. */
export type ItemKind = 'PRODUCT' | 'WAREHOUSE';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory.InventoryStatus}. */
export type InventoryStatus = 'NORMAL' | 'LOW_STOCK' | 'OUT_OF_STOCK';

export type InventoryTransactionType = 'IN' | 'OUT' | 'ADJUSTMENT';
export type InventoryTransactionStatus = 'COMPLETED';
export type InventoryMovementType = 'IN' | 'OUT' | 'ADJUSTMENT_PLUS' | 'ADJUSTMENT_MINUS';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.inventory.core.domain.enums.InventoryExitReason}. */
export type InventoryExitReason = 'SALE' | 'WASTE' | 'WITHDRAWAL' | 'RETURN_FROM_CLIENT';

/** Exit reasons a staff member can register. POS writes SALE and RETURN_FROM_CLIENT. */
export type StaffExitReason = Extract<InventoryExitReason, 'WASTE' | 'WITHDRAWAL'>;

export type MovementDirection = 'IN' | 'OUT';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation.LocationType}. */
export type LocationType =
  | 'WAREHOUSE'
  | 'ZONE'
  | 'AISLE'
  | 'SHELF'
  | 'BIN'
  | 'POS';
