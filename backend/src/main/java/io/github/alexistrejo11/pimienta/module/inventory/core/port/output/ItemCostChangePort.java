package io.github.alexistrejo11.pimienta.module.inventory.core.port.output;

/** Tells downstream catalogs that a warehouse item's unit cost changed. */
public interface ItemCostChangePort {

  void publish(long inventoryItemId);
}
