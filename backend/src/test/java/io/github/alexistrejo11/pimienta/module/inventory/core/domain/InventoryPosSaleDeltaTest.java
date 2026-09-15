package io.github.alexistrejo11.pimienta.module.inventory.core.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class InventoryPosSaleDeltaTest {

  @Test
  void applyPosSaleDelta_allowsNegativeOnPosLocation() {
    StorageLocation pos =
        StorageLocation.create(
            "POS-1", "POS", null, StorageLocation.LocationType.POS, null, 10_000, 1L);
    Item item = sampleItem();
    Inventory inv = Inventory.create(item, pos, 2);

    inv.applyPosSaleDelta(5);

    assertEquals(-3, inv.getAvailableQuantity());
    assertEquals(Inventory.InventoryStatus.OUT_OF_STOCK, inv.getStatus());
  }

  @Test
  void applyPosSaleDelta_rejectsNonPosLocation() {
    StorageLocation warehouse =
        StorageLocation.create(
            "WH-1", "Warehouse", null, StorageLocation.LocationType.WAREHOUSE, null, 10_000);
    Item item = sampleItem();
    Inventory inv = Inventory.create(item, warehouse, 2);

    assertThrows(IllegalStateException.class, () -> inv.applyPosSaleDelta(1));
  }

  @Test
  void removeStock_stillRejectsInsufficientOnWarehouse() {
    StorageLocation warehouse =
        StorageLocation.create(
            "WH-1", "Warehouse", null, StorageLocation.LocationType.WAREHOUSE, null, 10_000);
    Item item = sampleItem();
    Inventory inv = Inventory.create(item, warehouse, 2);

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> inv.removeStock(5));
    assertTrue(ex.getMessage().contains("Stock insuficiente"));
  }

  private static Item sampleItem() {
    Item item = new Item();
    item.setId(10L);
    item.setSku("SKU-1");
    item.setName("Test");
    item.setCostPrice(BigDecimal.ONE);
    item.setReorderPoint(0);
    return item;
  }
}
