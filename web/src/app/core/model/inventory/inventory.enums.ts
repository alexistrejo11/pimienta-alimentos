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

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory.InventoryStatus}. */
export type InventoryStatus = 'NORMAL' | 'LOW_STOCK' | 'OUT_OF_STOCK';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation.LocationType}. */
export type LocationType =
  | 'WAREHOUSE'
  | 'ZONE'
  | 'AISLE'
  | 'SHELF'
  | 'BIN'
  | 'POS';
