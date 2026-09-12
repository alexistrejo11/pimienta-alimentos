package io.github.alexistrejo11.pimienta.module.inventory.core.port.input;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.ApplyPosSaleStockCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory;

/**
 * POS cafeteria stock path. Allows negative available quantity on {@code LocationType.POS} only.
 * Does not use {@code InventoryTransactionManagementUseCases.sale()} or {@code Inventory.removeStock()}.
 */
public interface PosSaleInventoryUseCases {

  Inventory applySaleStock(ApplyPosSaleStockCommand command);
}
