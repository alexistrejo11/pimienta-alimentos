package io.github.alexistrejo11.pimienta.module.inventory.core.port.input;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.AdjustmentTransactionCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.InTransactionCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.OutTransactionCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.InventoryTransactionSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryTransactionManagementUseCases {

  Page<InventoryTransaction> search(InventoryTransactionSearchCriteria criteria, Pageable pageable);

  InventoryTransaction getById(Long id);

  InventoryTransaction receive(InTransactionCommand command);

  InventoryTransaction exit(OutTransactionCommand command);

  InventoryTransaction adjustment(AdjustmentTransactionCommand command);
}
